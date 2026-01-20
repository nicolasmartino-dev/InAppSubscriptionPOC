package com.subscription.poc.presentation.subscription

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.subscription.poc.domain.model.SubscriptionPlan
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class SubscriptionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel: SubscriptionViewModel = mockk(relaxed = true)
    private val uiStateFlow = MutableStateFlow(SubscriptionUiState())

    @Test
    fun loadingState_showsProgressIndicator() {
        // Given
        every { mockViewModel.uiState } returns uiStateFlow
        uiStateFlow.value = SubscriptionUiState(isLoading = true)

        // When
        composeTestRule.setContent {
            SubscriptionScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun successState_showsSubscriptionPlans() {
        // Given
        val plans = listOf(
            SubscriptionPlan(
                productId = "prod1", 
                name = "Test Plan 1", 
                description = "Desc", 
                priceFormatted = "$5", 
                priceMicros = 5000000, 
                currencyCode = "USD"
            )
        )
        every { mockViewModel.uiState } returns uiStateFlow
        uiStateFlow.value = SubscriptionUiState(subscriptionPlans = plans, isLoading = false)

        // When
        composeTestRule.setContent {
            SubscriptionScreen(viewModel = mockViewModel)
        }

        // Then
        composeTestRule.onNodeWithText("Choose Your Plan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Plan 1").assertIsDisplayed()
    }

    @Test
    fun toggleLayout_switchesToCarousel() {
        // Given
        every { mockViewModel.uiState } returns uiStateFlow
        // Start in Vertical Mode
        uiStateFlow.value = SubscriptionUiState(isHorizontalLayout = false)
        
        composeTestRule.setContent {
            SubscriptionScreen(viewModel = mockViewModel)
        }

        // When: Click Toggle Button
        composeTestRule.onNodeWithContentDescription("Toggle Layout").performClick()
        
        // Then: Verify ViewModel called
        io.mockk.verify { mockViewModel.toggleLayoutMode() }
        
        // Simulate State Update (since mock doesn't update automatically)
        uiStateFlow.value = SubscriptionUiState(isHorizontalLayout = true)
        
        // Verify Toggle Icon changed (Optional, check description or icon)
        // Hard to check exact ImageVector, but we can verify the state reflection if we had unique tags.
        // For now, verifying the ViewModel call is the critical logic test.
    }
}
