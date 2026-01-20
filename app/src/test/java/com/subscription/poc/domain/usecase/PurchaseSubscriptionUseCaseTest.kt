package com.subscription.poc.domain.usecase

import android.app.Activity
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PurchaseSubscriptionUseCase
 */
class PurchaseSubscriptionUseCaseTest {
    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: PurchaseSubscriptionUseCase
    private lateinit var activity: Activity

    @Before
    fun setup() {
        repository = mockk()
        // Mock the new save method which returns Unit
        io.mockk.every { repository.saveLastSelectedPlanId(any()) } returns Unit
        useCase = PurchaseSubscriptionUseCase(repository)
        activity = mockk(relaxed = true)
    }

    @Test
    fun `invoke should return success when purchase succeeds`() = runTest {
        // Given
        val productId = "subscription_first"
        io.mockk.coEvery { repository.purchaseSubscription(activity, productId) } returns PurchaseResult.Success

        // When
        val result = useCase(activity, productId)

        // Then
        assertEquals(PurchaseResult.Success, result)
        io.mockk.coVerify { repository.purchaseSubscription(activity, productId) }
    }

    @Test
    fun `invoke should return error when purchase fails`() = runTest {
        // Given
        val productId = "subscription_first"
        val errorResult = PurchaseResult.Error("Payment failed")
        io.mockk.coEvery { repository.purchaseSubscription(activity, productId) } returns errorResult

        // When
        val result = useCase(activity, productId)

        // Then
        assertEquals(errorResult, result)
    }

    @Test
    fun `invoke should return user cancelled when user cancels`() = runTest {
        // Given
        val productId = "subscription_first"
        io.mockk.coEvery { repository.purchaseSubscription(activity, productId) } returns PurchaseResult.UserCancelled

        // When
        val result = useCase(activity, productId)

        // Then
        assertEquals(PurchaseResult.UserCancelled, result)
    }
}
