package com.subscription.poc.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObservePurchaseUpdatesUseCaseTest {

    private val repository: SubscriptionRepository = mockk()
    private val useCase = ObservePurchaseUpdatesUseCase(repository)

    @Test
    fun `invoke returns flow from repository`() = runTest {
        // Given
        val expectedResult = PurchaseResult.Success
        every { repository.observePurchaseUpdates() } returns flowOf(expectedResult)

        // When
        useCase().test {
            // Then
            val item = awaitItem()
            assertThat(item).isEqualTo(expectedResult)
            awaitComplete()
        }

        verify(exactly = 1) { repository.observePurchaseUpdates() }
    }
}
