package com.subscription.poc.domain.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.subscription.poc.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ManageSubscriptionUseCaseTest {

    private val repository = mockk<SubscriptionRepository>()
    private val context = mockk<Context>(relaxed = true)
    private lateinit var manageSubscriptionUseCase: ManageSubscriptionUseCase

    @Before
    fun setup() {
        manageSubscriptionUseCase = ManageSubscriptionUseCase(repository)
        every { context.packageName } returns "com.subscription.poc"
    }

    @Test
    fun `invoke with productId starts activity with specific SKU URL`() {
        // Given
        val productId = "premium_plan"
        val intentSlot = slot<Intent>()
        
        // When
        manageSubscriptionUseCase(context, productId)

        // Then
        verify { context.startActivity(capture(intentSlot)) }
        val capturedIntent = intentSlot.captured
        assert(capturedIntent.action == Intent.ACTION_VIEW)
        assert(capturedIntent.data.toString().contains("sku=premium_plan"))
        assert(capturedIntent.data.toString().contains("package=com.subscription.poc"))
    }

    @Test
    fun `invoke without productId starts activity with general subscriptions URL`() {
        // Given
        val intentSlot = slot<Intent>()
        
        // When
        manageSubscriptionUseCase(context, null)

        // Then
        verify { context.startActivity(capture(intentSlot)) }
        val capturedIntent = intentSlot.captured
        assert(capturedIntent.action == Intent.ACTION_VIEW)
        assert(capturedIntent.data.toString() == "https://play.google.com/store/account/subscriptions")
    }
}
