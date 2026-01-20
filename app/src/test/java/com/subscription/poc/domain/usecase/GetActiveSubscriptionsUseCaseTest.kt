package com.subscription.poc.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetActiveSubscriptionsUseCaseTest {

    private val repository: SubscriptionRepository = mockk()
    private val useCase = GetActiveSubscriptionsUseCase(repository)

    @Test
    fun `invoke calls repository and returns result`() = runTest {
        // Given
        val expectedPlans =
            listOf(
                SubscriptionPlan(
                    productId = "premium_access",
                    name = "First Plan",
                    description = "Active",
                    priceFormatted = "$9.99",
                    priceMicros = 9990000,
                    currencyCode = "USD",
                    isActive = true,
                ),
            )
        coEvery { repository.getActiveSubscriptions() } returns Result.success(expectedPlans)

        // When
        val result = useCase()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedPlans)
        coVerify(exactly = 1) { repository.getActiveSubscriptions() }
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { repository.getActiveSubscriptions() } returns Result.failure(exception)

        // When
        val result = useCase()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
        coVerify(exactly = 1) { repository.getActiveSubscriptions() }
    }
}
