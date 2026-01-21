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
    private var pendingPlanId: String? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _processedPurchaseUpdates = kotlinx.coroutines.flow.MutableSharedFlow<PurchaseResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    init {
        repositoryScope.launch {
            billingClientWrapper.observePurchaseUpdates().collect { result ->
                if (result is PurchaseResult.Success) {
                    // CRITICAL: Save the plan ID BEFORE notifying the UI
                    // This ensures the next refresh sees the updated state
                    synchronized(this) {
                        pendingPlanId?.let { 
                            android.util.Log.d("BillingDiagnostic", "Purchase Success. Saving planId: $it")
                            saveLastSelectedPlanId(it) 
                        }
                        pendingPlanId = null
                    }
                } else if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
                    synchronized(this) {
                        pendingPlanId = null
                    }
                }
                _processedPurchaseUpdates.emit(result)
            }
        }
    }

    override fun saveLastSelectedPlanId(planId: String) {
        sharedPrefs.edit { putString("last_plan_id", planId) }
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
            
            productResult.onSuccess { details ->
                android.util.Log.d("BillingDiagnostic", "Fetched ${details.size} products: ${details.map { it.productId }}")
                val product = details.find { it.productId == "premium_access" }
                if (product != null) {
                    productDetailsCache[product.productId] = product
                    val activeSubs = activeResult.getOrNull() ?: emptyList()
                    val savedId = sharedPrefs.getString("last_plan_id", null)
                    
                    val plans = product.subscriptionOfferDetails?.map { offer ->
                        val plan = mapOffer(offer)
                        // Mark as active if this offer is the one we have active
                        val isCurrent = activeSubs.any { it.products.contains("premium_access") } && offer.basePlanId == savedId
                        plan.copy(isActive = isCurrent)
                    } ?: emptyList()
                    
                    return@runWithRetry Result.success(plans)
                }
            }
            Result.failure(Exception("Product not found"))
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
        val latest = history.find { it.products.contains("premium_access") } ?: return emptyList()
        
        if ((System.currentTimeMillis() - latest.purchaseTime) / (1000 * 60 * 60 * 24) < 45) {
            val savedId = sharedPrefs.getString("last_plan_id", null)
            return listOf(SubscriptionPlan(
                productId = "premium_access",
                name = if (savedId != null) getPlanName(savedId) else "Premium Access",
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
        val savedId = sharedPrefs.getString("last_plan_id", null)
        val offer = product?.subscriptionOfferDetails?.find { it.basePlanId == savedId }
            ?: product?.subscriptionOfferDetails?.firstOrNull()
        val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()

        return SubscriptionPlan(
            productId = productId,
            name = if (savedId != null) getPlanName(savedId) else "Premium Access",
            description = if (savedId != null) "Active (${getPlanName(savedId)} Plan)" else "Active Subscription",
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
        val product = productDetailsCache["premium_access"] ?: return PurchaseResult.Error("Refresh plans first")
        val offerToken = product.subscriptionOfferDetails?.find { it.basePlanId == productId }?.offerToken
            ?: return PurchaseResult.Error("Plan not found")
        
        val activePurchases = billingClientWrapper.queryActiveSubscriptions().getOrNull() ?: emptyList()
        var oldPurchase = activePurchases.find { it.products.contains("premium_access") }
        
        // If no active purchase is found, we should first try a FRESH purchase
        // without update parameters. This is safer for "Cancelled" or "Hold" states
        // where Google Play might reject an update handshake.
        if (oldPurchase == null) {
            android.util.Log.d("BillingDiagnostic", "No active sub found. Attempting FRESH purchase (no update params).")
            pendingPlanId = productId
            return billingClientWrapper.launchBillingFlow(activity, product, offerToken, null, 0)
        }

        val oldPurchaseToken = oldPurchase?.purchaseToken
        val savedId = sharedPrefs.getString("last_plan_id", null)

        // Don't allow re-subscribing to the same plan
        if (oldPurchaseToken != null && productId == savedId) {
            return PurchaseResult.Error("You are already subscribed to this plan")
        }
            
        val allPlans = getSubscriptionPlans().getOrNull() ?: emptyList()
        val targetPlan = allPlans.find { it.productId == productId }
        val currentPlan = allPlans.find { it.productId == savedId }
        
        // Sandbox Demo Mode:
        // In the Google Play Sandbox, mathematical proration modes (2 and 4) often fail
        // with "Something went wrong" (Code 6) due to accelerated time and rounding errors.
        // If isSandboxDemoMode() is true, we force WITHOUT_PRORATION (5) which is stable.
        val replacementMode = if (isSandboxDemoMode()) {
            android.util.Log.d("BillingDiagnostic", "Sandbox Demo Mode active (dynamic): Forcing Mode 5 for stability")
            5
        } else {
            // Hybrid Fairness Strategy (Production Logic):
            // - Upgrade: CHARGE_PRORATED_PRICE (2) - Charge diff now, keep cycle same.
            // - Downgrade: DEFERRED (4) - Change at next renewal, user keeps current benefits.
            // - Same/Unknown: WITHOUT_PRORATION (5) - Safe fallback.
            when {
                currentPlan == null || targetPlan == null -> 5
                targetPlan.priceMicros > currentPlan.priceMicros -> 2
                targetPlan.priceMicros < currentPlan.priceMicros -> 4
                else -> 5 
            }
        }

        android.util.Log.d("BillingDiagnostic", "Transition strategy: From ${savedId} to $productId -> Mode $replacementMode")

        synchronized(this) {
            pendingPlanId = productId
        }
        val result = billingClientWrapper.launchBillingFlow(activity, product, offerToken, oldPurchaseToken, replacementMode)
        if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
            synchronized(this) {
                pendingPlanId = null
            }
        }
        return result
    }

    private fun mapOffer(offer: ProductDetails.SubscriptionOfferDetails): SubscriptionPlan {
        val phase = offer.pricingPhases.pricingPhaseList.firstOrNull()
        return SubscriptionPlan(
            productId = offer.basePlanId,
            name = getPlanName(offer.basePlanId),
            description = getPlanDescription(offer.basePlanId),
            priceFormatted = phase?.formattedPrice ?: "N/A",
            priceMicros = phase?.priceAmountMicros ?: 0L,
            currencyCode = phase?.priceCurrencyCode ?: "CAD",
            isActive = false,
            billingPeriod = phase?.billingPeriod?.let { mapBillingPeriod(it) }
        )
    }

    private fun getPlanName(id: String) = when (id) {
        "first-monthly" -> "First"
        "second-monthly" -> "Second"
        "bundle-monthly" -> "Bundle"
        else -> "Premium Access"
    }

    private fun getPlanDescription(id: String) = when (id) {
        "first-monthly" -> "Basic subscription plan"
        "second-monthly" -> "Premium subscription plan"
        "bundle-monthly" -> "Complete bundle subscription"
        else -> ""
    }

    private fun mapBillingPeriod(iso: String) = when (iso) {
        "P1M" -> "Monthly"; "P1Y" -> "Yearly"; "P1W" -> "Weekly"; else -> iso
    }

    private fun getMockSubscriptionPlans() = listOf("first-monthly", "second-monthly", "bundle-monthly").map { id ->
        SubscriptionPlan(
            productId = id, name = "${getPlanName(id)} (Mock)", description = "${getPlanDescription(id)} (Mock)",
            priceFormatted = when(id) { "first-monthly" -> "$4.99" ; "second-monthly" -> "$5.99" ; else -> "$8.99" },
            priceMicros = 0L, currencyCode = "CAD", billingPeriod = "Monthly"
        )
    }

    override fun isSandboxDemoMode(): Boolean {
        // Default to true for easy initial testing
        return sharedPrefs.getBoolean("sandbox_demo_mode", true)
    }

    override fun setSandboxDemoMode(enabled: Boolean) {
        sharedPrefs.edit { putBoolean("sandbox_demo_mode", enabled) }
    }

    override fun endConnection() {
        billingClientWrapper.endConnection()
    }
}
