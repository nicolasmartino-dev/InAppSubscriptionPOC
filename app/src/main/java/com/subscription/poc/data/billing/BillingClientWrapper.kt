package com.subscription.poc.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.Purchase.PurchaseState
import com.subscription.poc.domain.model.PurchaseResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingClientWrapper
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val SUBSCRIPTION_PRODUCT_IDS = listOf("subscription_first", "subscription_second", "subscription_bundle")
    }

    private var billingClient: BillingClient? = null
    private val internalScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _purchaseUpdates = MutableSharedFlow<PurchaseResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private suspend fun <T> safeBillingCall(
        action: (BillingClient, (Result<T>) -> Unit) -> Unit
    ): Result<T> = suspendCancellableCoroutine { continuation ->
        val client = billingClient
        if (client == null || !client.isReady) {
            continuation.resume(Result.failure(Exception("Billing client not ready")))
            return@suspendCancellableCoroutine
        }
        action(client) { result -> continuation.resume(result) }
    }

    suspend fun startConnection(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            handlePurchases(billingResult, purchases)
        }

        billingClient = BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build())
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    continuation.resume(Result.success(Unit))
                } else {
                    continuation.resume(Result.failure(Exception("Setup failed: ${billingResult.debugMessage}")))
                }
            }
            override fun onBillingServiceDisconnected() {
                continuation.resume(Result.failure(Exception("Disconnected")))
            }
        })
    }

    private fun handlePurchases(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> purchases?.forEach { purchase ->
                if (purchase.purchaseState == PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    internalScope.launch {
                        acknowledgePurchase(purchase)
                        _purchaseUpdates.emit(PurchaseResult.Success)
                    }
                } else if (purchase.purchaseState == PurchaseState.PENDING) {
                    _purchaseUpdates.tryEmit(PurchaseResult.Pending)
                }
            }
            BillingResponseCode.USER_CANCELED -> _purchaseUpdates.tryEmit(PurchaseResult.UserCancelled)
            else -> {
                android.util.Log.e("BillingDiagnostic", "Purchase update error: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
                _purchaseUpdates.tryEmit(PurchaseResult.Error(billingResult.debugMessage))
            }
        }
    }

    val isReady: Boolean get() = billingClient?.isReady == true

    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
    }

    suspend fun querySubscriptionProducts(): Result<List<ProductDetails>> = safeBillingCall { client, onResult ->
        val productList = SUBSCRIPTION_PRODUCT_IDS.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it).setProductType(ProductType.SUBS).build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingResponseCode.OK) {
                onResult(Result.success(details ?: emptyList()))
            } else {
                onResult(Result.failure(Exception("Query failed: ${result.debugMessage}")))
            }
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
        oldPurchaseToken: String? = null,
        replacementMode: Int = ReplacementMode.WITH_TIME_PRORATION,
    ): PurchaseResult {
        val client = billingClient ?: return PurchaseResult.Error("Not initialized")
        if (!client.isReady) return PurchaseResult.Error("Not ready")

        val paramsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )

        android.util.Log.d("BillingDiagnostic", "Params: PID=${productDetails.productId} OT=${offerToken.take(10)}...")

        if (oldPurchaseToken != null) {
            android.util.Log.d("BillingDiagnostic", "Update: oldToken=${oldPurchaseToken.take(10)}... mode=$replacementMode")
            paramsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldPurchaseToken)
                    .setSubscriptionReplacementMode(replacementMode)
                    .build()
            )
        }
        
        val params = paramsBuilder.build()
        val billingResult = client.launchBillingFlow(activity, params)
        android.util.Log.d("BillingDiagnostic", "Launch Result: ${billingResult.responseCode} - ${billingResult.debugMessage}")
        if (billingResult.responseCode != BillingResponseCode.OK) {
            android.util.Log.e("BillingDiagnostic", "Launch failed: ${billingResult.debugMessage} (Code: ${billingResult.responseCode})")
        }
        return when (billingResult.responseCode) {
            BillingResponseCode.OK -> PurchaseResult.Pending
            BillingResponseCode.USER_CANCELED -> PurchaseResult.UserCancelled
            else -> PurchaseResult.Error(billingResult.debugMessage)
        }
    }

    suspend fun queryActiveSubscriptions(): Result<List<Purchase>> = safeBillingCall { client, onResult ->
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                internalScope.launch {
                    purchases.filter { !it.isAcknowledged && it.purchaseState == PurchaseState.PURCHASED }.forEach {
                        acknowledgePurchase(it)
                    }
                }
                onResult(Result.success(purchases))
            } else {
                onResult(Result.failure(Exception("Query active failed: ${result.debugMessage}")))
            }
        }
    }

    fun observePurchaseUpdates(): Flow<PurchaseResult> = _purchaseUpdates

    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> = safeBillingCall { client, onResult ->
        if (purchase.purchaseState == PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken).build()
            client.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingResponseCode.OK) onResult(Result.success(Unit))
                else onResult(Result.failure(Exception("Ack failed: ${result.debugMessage}")))
            }
        } else {
            onResult(Result.success(Unit))
        }
    }

    suspend fun queryPurchaseHistory(): Result<List<PurchaseHistoryRecord>> = safeBillingCall { client, onResult ->
        val params = QueryPurchaseHistoryParams.newBuilder().setProductType(ProductType.SUBS).build()
        client.queryPurchaseHistoryAsync(params) { result, records ->
            if (result.responseCode == BillingResponseCode.OK) onResult(Result.success(records ?: emptyList()))
            else onResult(Result.failure(Exception("History failed: ${result.debugMessage}")))
        }
    }
}
