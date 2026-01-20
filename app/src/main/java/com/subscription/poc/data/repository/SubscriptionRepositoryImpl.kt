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

    init {
        repositoryScope.launch {
            billingClientWrapper.observePurchaseUpdates().collect { result ->
                if (result is PurchaseResult.Success) {
                    pendingPlanId?.let { saveLastSelectedPlanId(it) }
                    pendingPlanId = null
                } else if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
                    pendingPlanId = null
                }
            }
        }
    }

    override fun saveLastSelectedPlanId(planId: String) {
        sharedPrefs.edit { putString("last_plan_id", planId) }
    }

    override suspend fun startConnection() = billingClientWrapper.startConnection()
    override fun endConnection() = billingClientWrapper.endConnection()
    override fun observePurchaseUpdates() = billingClientWrapper.observePurchaseUpdates()

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
            val result = billingClientWrapper.querySubscriptionProducts()
            result.onSuccess { details ->
                val product = details.find { it.productId == "premium_access" }
                if (product != null) {
                    productDetailsCache[product.productId] = product
                    val plans = product.subscriptionOfferDetails?.map { mapOffer(it) } ?: emptyList()
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
        
        val oldPurchaseToken = billingClientWrapper.queryActiveSubscriptions().getOrNull()
            ?.find { it.products.contains("premium_access") }?.purchaseToken
            
        var replacementMode = ReplacementMode.CHARGE_FULL_PRICE
        
        if (oldPurchaseToken != null) {
             val savedId = sharedPrefs.getString("last_plan_id", null)
             if (savedId != null) {
                 val currentOffer = product.subscriptionOfferDetails?.find { it.basePlanId == savedId }
                 val newOffer = product.subscriptionOfferDetails?.find { it.basePlanId == productId }
                 
                 val currentPrice = currentOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros ?: 0L
                 val newPrice = newOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros ?: 0L
                 
                 if (newPrice > currentPrice) {
                     // Upgrade: Charge Prorated Price
                     replacementMode = ReplacementMode.CHARGE_PRORATED_PRICE
                 } else if (newPrice < currentPrice) {
                     // Downgrade: Deferred
                     replacementMode = ReplacementMode.DEFERRED
                 } else {
                     // Same price or unable to determine: Charge Full Price (or Deferred/TimeProration depending on policy)
                     replacementMode = ReplacementMode.CHARGE_FULL_PRICE
                 }
             }
        }

        pendingPlanId = productId
        val result = billingClientWrapper.launchBillingFlow(activity, product, offerToken, oldPurchaseToken, replacementMode)
        if (result is PurchaseResult.UserCancelled || result is PurchaseResult.Error) {
            pendingPlanId = null
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
}
