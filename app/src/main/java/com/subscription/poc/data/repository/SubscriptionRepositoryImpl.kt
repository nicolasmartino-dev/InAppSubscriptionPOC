package com.subscription.poc.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.Purchase.PurchaseState
import com.subscription.poc.data.billing.BillingClientWrapper
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.repository.SubscriptionRepository
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingClientWrapper: BillingClientWrapper,
    @ApplicationContext private val context: Context
) : SubscriptionRepository {
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()
    private val sharedPrefs by lazy { context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE) }
    private var pendingProductId: String? = null
    private var pendingBasePlanId: String? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _processedPurchaseUpdates = kotlinx.coroutines.flow.MutableSharedFlow<PurchaseResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    init {
        repositoryScope.launch {
            billingClientWrapper.observePurchaseUpdates().collect { result ->
                if (result is PurchaseResult.Success) {
                    synchronized(this) {
                        if (pendingProductId != null && pendingBasePlanId != null) {
                            android.util.Log.d("BillingDiagnostic", "Purchase Success. Saving productId: $pendingProductId, basePlanId: $pendingBasePlanId")
                            saveLastSelectedPlan(pendingProductId!!, pendingBasePlanId!!)
                        }
                        pendingProductId = null
                        pendingBasePlanId = null
                    }
                } else if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
                    synchronized(this) {
                        pendingProductId = null
                        pendingBasePlanId = null
                    }
                }
                _processedPurchaseUpdates.emit(result)
            }
        }
    }

    override fun saveLastSelectedPlanId(planId: String) {
        // Legacy method - now we save both productId and basePlanId
        sharedPrefs.edit { putString("last_base_plan_id", planId) }
    }

    private fun saveLastSelectedPlan(productId: String, basePlanId: String) {
        sharedPrefs.edit { 
            putString("last_product_id", productId)
            putString("last_base_plan_id", basePlanId) 
        }
    }

    override suspend fun startConnection() = billingClientWrapper.startConnection()
    override fun observePurchaseUpdates() = _processedPurchaseUpdates

    private suspend fun <T> runWithRetry(
        action: suspend () -> Result<T>,
        fallback: (() -> T)? = null
    ): Result<T> {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            if (!billingClientWrapper.isReady) billingClientWrapper.startConnection()
            val result = action()
            if (result.isSuccess) return result
            
            lastError = result.exceptionOrNull()
            if (attempt < 2 && lastError?.message?.contains("not ready") == true) {
                delay(1000)
            }
        }
        return fallback?.let { Result.success(it()) } ?: Result.failure(lastError ?: Exception("Operation failed"))
    }

    override suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>> = runWithRetry(
        action = {
            val productResult = billingClientWrapper.querySubscriptionProducts()
            val activeResult = billingClientWrapper.queryActiveSubscriptions()
            
            productResult.onSuccess { products ->
                android.util.Log.d("BillingDiagnostic", "Fetched ${products.size} products: ${products.map { it.productId }}")
                
                products.forEach { productDetailsCache[it.productId] = it }
                
                val activeSubs = activeResult.getOrNull() ?: emptyList()
                val savedProductId = sharedPrefs.getString("last_product_id", null)
                val savedBasePlanId = sharedPrefs.getString("last_base_plan_id", null)
                
                // Flatten all products -> their base plans into a single list
                val plans = products.flatMap { product ->
                    product.subscriptionOfferDetails?.map { offer ->
                        val isCurrent = activeSubs.any { it.products.contains(product.productId) } &&
                                offer.basePlanId == savedBasePlanId &&
                                product.productId == savedProductId
                        mapOffer(product.productId, product.name, offer).copy(isActive = isCurrent)
                    } ?: emptyList()
                }
                
                if (plans.isEmpty()) {
                    return@runWithRetry Result.failure(Exception("No plans found"))
                }
                
                return@runWithRetry Result.success(plans)
            }
            Result.failure(Exception("Failed to query products"))
        },
        fallback = { getMockSubscriptionPlans() }
    )

    override suspend fun getActiveSubscriptions(): Result<List<SubscriptionPlan>> = runWithRetry(
        action = {
            val result = billingClientWrapper.queryActiveSubscriptions()
            if (result.isSuccess) {
                val purchases = result.getOrNull() ?: emptyList()
                val plans = if (purchases.isEmpty()) handleEmptyPurchases() else purchases.mapNotNull { mapPurchase(it) }
                Result.success(plans)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Query failed"))
            }
        }
    )

    private suspend fun handleEmptyPurchases(): List<SubscriptionPlan> {
        val history = billingClientWrapper.queryPurchaseHistory().getOrNull() ?: emptyList()
        val savedProductId = sharedPrefs.getString("last_product_id", null) ?: return emptyList()
        val latest = history.find { it.products.contains(savedProductId) } ?: return emptyList()
        
        if ((System.currentTimeMillis() - latest.purchaseTime) / (1000 * 60 * 60 * 24) < 45) {
            val savedBasePlanId = sharedPrefs.getString("last_base_plan_id", null)
            return listOf(SubscriptionPlan(
                productId = savedProductId,
                basePlanId = savedBasePlanId ?: "",
                offerToken = "",
                name = getProductName(savedProductId),
                description = "Payment issue or paused",
                priceFormatted = "--", priceMicros = 0, currencyCode = "",
                isActive = true, isAutoRenewing = true, isOnHold = true
            ))
        }
        return emptyList()
    }

    private fun mapPurchase(purchase: Purchase): SubscriptionPlan? {
        val productId = purchase.products.firstOrNull() ?: return null
        val product = productDetailsCache[productId]
        val savedBasePlanId = sharedPrefs.getString("last_base_plan_id", null)
        val offer = product?.subscriptionOfferDetails?.find { it.basePlanId == savedBasePlanId }
            ?: product?.subscriptionOfferDetails?.firstOrNull()
        val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()

        return SubscriptionPlan(
            productId = productId,
            basePlanId = offer?.basePlanId ?: "",
            offerToken = offer?.offerToken ?: "",
            name = getProductName(productId),
            description = "Active Subscription",
            priceFormatted = phase?.formattedPrice ?: "",
            priceMicros = phase?.priceAmountMicros ?: 0,
            currencyCode = phase?.priceCurrencyCode ?: "",
            isActive = true,
            isAutoRenewing = purchase.isAutoRenewing,
            billingPeriod = phase?.billingPeriod?.let { mapBillingPeriod(it) },
            isOnHold = purchase.purchaseState == PurchaseState.PENDING
        )
    }

    override suspend fun purchaseSubscription(activity: Activity, productId: String): PurchaseResult {
        // productId here now represents a composite key or we need basePlanId
        // For simplicity, we assume productId is passed as "productId:basePlanId"
        val parts = productId.split(":")
        val actualProductId = parts.getOrNull(0) ?: return PurchaseResult.Error("Invalid plan ID")
        val basePlanId = parts.getOrNull(1) ?: return PurchaseResult.Error("Invalid plan ID")
        
        val product = productDetailsCache[actualProductId] ?: return PurchaseResult.Error("Refresh plans first")
        val offerToken = product.subscriptionOfferDetails?.find { it.basePlanId == basePlanId }?.offerToken
            ?: return PurchaseResult.Error("Plan not found")
        
        val activePurchases = billingClientWrapper.queryActiveSubscriptions().getOrNull() ?: emptyList()
        val oldPurchase = activePurchases.firstOrNull()
        
        if (oldPurchase == null) {
            android.util.Log.d("BillingDiagnostic", "No active sub found. Attempting FRESH purchase.")
            synchronized(this) {
                pendingProductId = actualProductId
                pendingBasePlanId = basePlanId
            }
            return billingClientWrapper.launchBillingFlow(activity, product, offerToken, null, 0)
        }

        val oldPurchaseToken = oldPurchase.purchaseToken
        val savedProductId = sharedPrefs.getString("last_product_id", null)
        val savedBasePlanId = sharedPrefs.getString("last_base_plan_id", null)

        // Don't allow re-subscribing to the same plan
        if (actualProductId == savedProductId && basePlanId == savedBasePlanId) {
            return PurchaseResult.Error("You are already subscribed to this plan")
        }
            
        val allPlans = getSubscriptionPlans().getOrNull() ?: emptyList()
        val targetPlan = allPlans.find { it.productId == actualProductId && it.basePlanId == basePlanId }
        val currentPlan = allPlans.find { it.productId == savedProductId && it.basePlanId == savedBasePlanId }
        
        val replacementMode = if (isSandboxDemoMode()) {
            android.util.Log.d("BillingDiagnostic", "Sandbox Demo Mode: Forcing Mode 5")
            5
        } else {
            when {
                currentPlan == null || targetPlan == null -> 5
                targetPlan.priceMicros > currentPlan.priceMicros -> 2
                targetPlan.priceMicros < currentPlan.priceMicros -> 4
                else -> 5 
            }
        }

        android.util.Log.d("BillingDiagnostic", "Transition: $savedProductId:$savedBasePlanId -> $actualProductId:$basePlanId, Mode $replacementMode")

        synchronized(this) {
            pendingProductId = actualProductId
            pendingBasePlanId = basePlanId
        }
        val result = billingClientWrapper.launchBillingFlow(activity, product, offerToken, oldPurchaseToken, replacementMode)
        if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
            synchronized(this) {
                pendingProductId = null
                pendingBasePlanId = null
            }
        }
        return result
    }

    private fun mapOffer(productId: String, productName: String, offer: ProductDetails.SubscriptionOfferDetails): SubscriptionPlan {
        val phase = offer.pricingPhases.pricingPhaseList.firstOrNull()
        return SubscriptionPlan(
            productId = productId,
            basePlanId = offer.basePlanId,
            offerToken = offer.offerToken,
            name = productName,
            description = getBasePlanDescription(offer.basePlanId),
            priceFormatted = phase?.formattedPrice ?: "N/A",
            priceMicros = phase?.priceAmountMicros ?: 0L,
            currencyCode = phase?.priceCurrencyCode ?: "CAD",
            isActive = false,
            billingPeriod = phase?.billingPeriod?.let { mapBillingPeriod(it) }
        )
    }

    private fun getProductName(productId: String) = when (productId) {
        "subscription_first" -> "First Subscription"
        "subscription_second" -> "Second Subscription"
        "subscription_bundle" -> "Bundle Subscription"
        else -> productId.replace("_", " ").capitalize()
    }

    private fun getBasePlanDescription(basePlanId: String) = when {
        basePlanId.contains("monthly") -> "Billed monthly"
        basePlanId.contains("yearly") || basePlanId.contains("annual") -> "Billed annually"
        basePlanId.contains("weekly") -> "Billed weekly"
        else -> "Subscription plan"
    }

    private fun mapBillingPeriod(iso: String) = when (iso) {
        "P1M" -> "Monthly"; "P1Y" -> "Yearly"; "P1W" -> "Weekly"; else -> iso
    }

    private fun getMockSubscriptionPlans() = listOf(
        SubscriptionPlan("subscription_first", "first-monthly", "mock_token_1", "First Subscription (Mock)", "Billed monthly", "$4.99/mo", 4990000, "USD", billingPeriod = "Monthly"),
        SubscriptionPlan("subscription_second", "second-monthly", "mock_token_2", "Second Subscription (Mock)", "Billed monthly", "$9.99/mo", 9990000, "USD", billingPeriod = "Monthly"),
        SubscriptionPlan("subscription_bundle", "bundle-monthly", "mock_token_3", "Bundle Subscription (Mock)", "Billed monthly", "$14.99/mo", 14990000, "USD", billingPeriod = "Monthly"),
    )

    override fun isSandboxDemoMode(): Boolean {
        return sharedPrefs.getBoolean("sandbox_demo_mode", true)
    }

    override fun setSandboxDemoMode(enabled: Boolean) {
        sharedPrefs.edit { putBoolean("sandbox_demo_mode", enabled) }
    }

    override fun endConnection() {
        billingClientWrapper.endConnection()
    }
}
