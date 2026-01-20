package com.subscription.poc.data.billing

import android.content.Context
import android.text.TextUtils
import app.cash.turbine.test
import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.subscription.poc.domain.model.PurchaseResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BillingClientWrapper
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillingClientWrapperTest {

    private lateinit var context: Context
    private lateinit var billingClient: BillingClient
    private lateinit var builder: BillingClient.Builder
    private lateinit var wrapper: BillingClientWrapper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        billingClient = mockk(relaxed = true)
        builder = mockk(relaxed = true)

        mockkStatic(BillingClient::class)
        every { BillingClient.newBuilder(any()) } returns builder
        every { builder.setListener(any()) } returns builder
        every { builder.enablePendingPurchases(any()) } returns builder
        every { builder.build() } returns billingClient
        
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { 
            val arg = it.invocation.args[0] as? CharSequence
            arg == null || arg.length == 0 
        }
        
        // Mock Dispatchers.IO to use test dispatcher
        mockkStatic(Dispatchers::class)
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        every { Dispatchers.IO } returns testDispatcher

        wrapper = BillingClientWrapper(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `startConnection should resume success on OK response`() = runTest {
        // Given
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        // When
        val result = wrapper.startConnection()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `startConnection should resume failure on error response`() = runTest {
        // Given
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.ERROR
        every { billingResult.debugMessage } returns "Error"
        
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } answers {
            listenerSlot.captured.onBillingSetupFinished(billingResult)
        }

        // When
        val result = wrapper.startConnection()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Error") == true)
    }

    @Test
    fun `querySubscriptionProducts should return success when client is ready`() = runTest {
        // Given
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        
        // Connect first
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        wrapper.startConnection()
        
        every { billingClient.isReady } returns true
        val products = listOf(mockk<ProductDetails>())
        
        val paramsSlot = slot<QueryProductDetailsParams>()
        val callbackSlot = slot<ProductDetailsResponseListener>()
        
        every { billingClient.queryProductDetailsAsync(capture(paramsSlot), capture(callbackSlot)) } answers {
            callbackSlot.captured.onProductDetailsResponse(billingResult, products)
        }

        // When
        val result = wrapper.querySubscriptionProducts()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(products, result.getOrNull())
    }

    @Test
    fun `queryActiveSubscriptions should return success when client is ready`() = runTest {
        // Given
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        
        // Connect first
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        wrapper.startConnection()
        
        every { billingClient.isReady } returns true
        val purchases = listOf(mockk<Purchase>(relaxed = true))
        
        val callbackSlot = slot<PurchasesResponseListener>()
        
        every { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), capture(callbackSlot)) } answers {
            callbackSlot.captured.onQueryPurchasesResponse(billingResult, purchases)
        }

        // When
        val result = wrapper.queryActiveSubscriptions()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(purchases, result.getOrNull())
    }

    @Test
    fun `endConnection should clear billingClient and call endConnection`() {
        // Given
        val billingResult = mockk<BillingResult>(relaxed = true)
        every { billingResult.responseCode } returns BillingResponseCode.OK
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        
        // Initialize client
        runTest { wrapper.startConnection() }

        // When
        wrapper.endConnection()

        // Then
        verify { billingClient.endConnection() }
    }

    @Test
    fun `launchBillingFlow should resume Success on OK response`() = runTest {
        // Given
        val activity = mockk<Activity>(relaxed = true)
        val productDetails = mockk<ProductDetails>(relaxed = true)
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        
        // Mock connection
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        wrapper.startConnection()
        every { billingClient.isReady } returns true
        
        every { billingClient.launchBillingFlow(any(), any()) } returns billingResult
        val mockOffer = mockk<ProductDetails.SubscriptionOfferDetails>(relaxed = true)
        every { mockOffer.offerToken } returns "test_offer_token"
        every { productDetails.subscriptionOfferDetails } returns listOf(mockOffer)

        // When
        val result = wrapper.launchBillingFlow(activity, productDetails, "test_offer_token")

        // Then
        // Then
        assertEquals(PurchaseResult.Pending, result)
    }

    @Test
    fun `acknowledgePurchase should resume success on OK response`() = runTest {
        // Given
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.isAcknowledged } returns false
        
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        
        // Mock connection
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        wrapper.startConnection()
        every { billingClient.isReady } returns true
        
        val callbackSlot = slot<AcknowledgePurchaseResponseListener>()
        every { billingClient.acknowledgePurchase(any(), capture(callbackSlot)) } answers {
            callbackSlot.captured.onAcknowledgePurchaseResponse(billingResult)
        }

        // When
        val result = wrapper.acknowledgePurchase(purchase)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `onPurchasesUpdated should emit success when purchased`() = runTest {
        // Given
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingResponseCode.OK
        val purchases = listOf(mockk<Purchase>(relaxed = true) {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { isAcknowledged } returns false
        })
        
        // Mock connection
        every { billingClient.startConnection(any()) } answers {
            (args[0] as BillingClientStateListener).onBillingSetupFinished(billingResult)
        }
        
        val listenerSlot = slot<PurchasesUpdatedListener>()
        every { builder.setListener(capture(listenerSlot)) } returns builder
        
        wrapper.startConnection() // This triggers the build() and captures the listener
        every { billingClient.isReady } returns true

        // Mock acknowledgePurchase callback so it doesn't hang
        val acknowledgeCallbackSlot = slot<AcknowledgePurchaseResponseListener>()
        every { billingClient.acknowledgePurchase(any(), capture(acknowledgeCallbackSlot)) } answers {
            acknowledgeCallbackSlot.captured.onAcknowledgePurchaseResponse(billingResult)
        }
        
        // When
        val flow = wrapper.observePurchaseUpdates()
        flow.test {
            listenerSlot.captured.onPurchasesUpdated(billingResult, purchases)
            val item = awaitItem()
            assertTrue(item is PurchaseResult.Success)
        }
    }
}
