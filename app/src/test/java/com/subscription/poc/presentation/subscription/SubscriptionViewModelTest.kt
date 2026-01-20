package com.subscription.poc.presentation.subscription

import android.app.Activity
import app.cash.turbine.test
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.usecase.GetActiveSubscriptionsUseCase
import com.subscription.poc.domain.usecase.GetSubscriptionPlansUseCase
import com.subscription.poc.domain.usecase.PurchaseSubscriptionUseCase
import com.subscription.poc.domain.usecase.ManageSubscriptionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SubscriptionViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {
    private lateinit var getSubscriptionPlansUseCase: GetSubscriptionPlansUseCase
    private lateinit var purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase
    private lateinit var getActiveSubscriptionsUseCase: GetActiveSubscriptionsUseCase
    private lateinit var observePurchaseUpdatesUseCase: com.subscription.poc.domain.usecase.ObservePurchaseUpdatesUseCase
    private lateinit var manageSubscriptionUseCase: ManageSubscriptionUseCase
    private lateinit var viewModel: SubscriptionViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getSubscriptionPlansUseCase = mockk()
        purchaseSubscriptionUseCase = mockk()
        getActiveSubscriptionsUseCase = mockk()
        observePurchaseUpdatesUseCase = mockk()
        manageSubscriptionUseCase = mockk(relaxed = true)

        // Default mock responses
        coEvery { getSubscriptionPlansUseCase() } returns Result.success(emptyList())
        coEvery { getActiveSubscriptionsUseCase() } returns Result.success(emptyList())
        coEvery { observePurchaseUpdatesUseCase() } returns kotlinx.coroutines.flow.emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load subscription plans`() =
        runTest {
            // Given
            val plans =
                listOf(
                    SubscriptionPlan(
                        productId = "subscription_first",
                        name = "First",
                        description = "Basic",
                        priceFormatted = "$4.99",
                        priceMicros = 4990000,
                        currencyCode = "CAD",
                        isActive = false,
                        isAutoRenewing = true,
                    ),
                )
            coEvery { getSubscriptionPlansUseCase() } returns Result.success(plans)

            // When
            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()

            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(plans, state.subscriptionPlans)
                assertNull(state.errorMessage)
            }
        }

    @Test
    fun `loadSubscriptionPlans should update state with error on failure`() =
        runTest {
            // Given
            val errorMessage = "Network error"
            coEvery { getSubscriptionPlansUseCase() } returns Result.failure(Exception(errorMessage))

            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()

            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertTrue(state.errorMessage?.contains(errorMessage) == true)
            }
        }

    @Test
    fun `purchaseSubscription should update state correctly`() =
        runTest {
            // Given
            coEvery { getSubscriptionPlansUseCase() } returns Result.success(emptyList())
            val activity: Activity = mockk(relaxed = true)
            val productId = "subscription_first"
            coEvery { purchaseSubscriptionUseCase(activity, productId) } returns PurchaseResult.Success

            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()

            // When
            viewModel.purchaseSubscription(activity, productId)
            advanceUntilIdle()

            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertFalse(state.purchaseInProgress)
                assertEquals(PurchaseResult.Success, state.lastPurchaseResult)
            }

            coVerify { purchaseSubscriptionUseCase(activity, productId) }
        }

    @Test
    fun `clearError should clear error message`() =
        runTest {
            // Given
            coEvery { getSubscriptionPlansUseCase() } returns Result.failure(Exception("Error"))
            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()
            assertNotNull(viewModel.uiState.value.errorMessage)

            // When
            viewModel.clearError()

            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertNull(state.errorMessage)
            }
        }

    @Test
    fun `toggleLayoutMode should switch layout state`() =
        runTest {
            // Given (Initialized with default false/vertical)
            coEvery { getSubscriptionPlansUseCase() } returns Result.success(emptyList())
            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()

            // When
            viewModel.toggleLayoutMode()

            // Then
            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.isHorizontalLayout)
            }
        }

    @Test
    fun `clearPurchaseResult should reset purchase state`() =
        runTest {
            // Given
            coEvery { getSubscriptionPlansUseCase() } returns Result.success(emptyList())
            // Simulate a purchase result
            val activity: Activity = mockk(relaxed = true)
            coEvery { purchaseSubscriptionUseCase(activity, any()) } returns PurchaseResult.Success
            
            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            viewModel.purchaseSubscription(activity, "id")
            advanceUntilIdle()

            // Verify it is set
            assertEquals(PurchaseResult.Success, viewModel.uiState.value.lastPurchaseResult)

            // When
            viewModel.clearPurchaseResult()

            // Then
            assertNull(viewModel.uiState.value.lastPurchaseResult)
            assertFalse(viewModel.uiState.value.purchaseInProgress)
        }

    @Test
    fun `manageSubscription should call manageSubscriptionUseCase`() =
        runTest {
            // Given
            coEvery { getSubscriptionPlansUseCase() } returns Result.success(emptyList())
            val context: android.content.Context = mockk(relaxed = true)
            viewModel =
                SubscriptionViewModel(
                    getSubscriptionPlansUseCase,
                    purchaseSubscriptionUseCase,
                    getActiveSubscriptionsUseCase,
                    observePurchaseUpdatesUseCase,
                    manageSubscriptionUseCase
                )
            advanceUntilIdle()

            // When
            viewModel.manageSubscription(context, "some_id")

            // Then
            io.mockk.verify { manageSubscriptionUseCase(context, "some_id") }
        }
}
