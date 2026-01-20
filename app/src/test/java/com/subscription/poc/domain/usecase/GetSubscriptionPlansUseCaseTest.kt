package com.subscription.poc.domain.usecase

import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetSubscriptionPlansUseCase
 */
class GetSubscriptionPlansUseCaseTest {
    private lateinit var repository: SubscriptionRepository
    private lateinit var useCase: GetSubscriptionPlansUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetSubscriptionPlansUseCase(repository)
    }

    @Test
    fun `invoke should return subscription plans from repository`() =
        runTest {
            // Given
            val expectedPlans =
                listOf(
                    SubscriptionPlan(
                        productId = "subscription_first",
                        name = "First",
                        description = "Basic plan",
                        priceFormatted = "$4.99",
                        priceMicros = 4990000,
                        currencyCode = "CAD",
                        isActive = false,
                    ),
                )
            coEvery { repository.getSubscriptionPlans() } returns Result.success(expectedPlans)

            // When
            val result = useCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(expectedPlans, result.getOrNull())
            coVerify { repository.getSubscriptionPlans() }
        }

    @Test
    fun `invoke should return failure when repository fails`() =
        runTest {
            // Given
            val exception = Exception("Network error")
            coEvery { repository.getSubscriptionPlans() } returns Result.failure(exception)

            // When
            val result = useCase()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
