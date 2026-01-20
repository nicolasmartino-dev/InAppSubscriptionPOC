package com.subscription.poc.presentation.subscription.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.subscription.poc.domain.model.SubscriptionPlan
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SubscriptionCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun subscriptionCard_displaysDetails() {
        val plan = SubscriptionPlan(
            productId = "test_id",
            name = "Monthly Premium",
            description = "Unlock all features for a month",
            priceFormatted = "$4.99",
            priceMicros = 4990000L,
            currencyCode = "USD",
            isActive = false
        )

        composeTestRule.setContent {
            SubscriptionCard(
                subscriptionPlan = plan,
                onPurchaseClick = {},
                isPurchasing = false
            )
        }

        composeTestRule.onNodeWithText("Monthly Premium").assertIsDisplayed()
        composeTestRule.onNodeWithText("$4.99").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unlock all features for a month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subscribe").assertIsDisplayed()
    }

    @Test
    fun subscriptionCard_showsActiveBadge_whenActive() {
        val plan = SubscriptionPlan(
            productId = "test_id",
            name = "Monthly Premium",
            description = "Unlock all features for a month",
            priceFormatted = "$4.99",
            priceMicros = 4990000L,
            currencyCode = "USD",
            isActive = true
        )

        composeTestRule.setContent {
            SubscriptionCard(
                subscriptionPlan = plan,
                onPurchaseClick = {},
                isPurchasing = false
            )
        }

        composeTestRule.onNodeWithText("✓ Active").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Subscribe").assertCountEquals(0)
    }

    @Test
    fun subscriptionCard_triggersOnClick() {
        var clicked = false
        val plan = SubscriptionPlan(
            productId = "test_id",
            name = "Monthly Premium",
            description = "Unlock all features for a month",
            priceFormatted = "$4.99",
            priceMicros = 4990000L,
            currencyCode = "USD",
            isActive = false
        )

        composeTestRule.setContent {
            SubscriptionCard(
                subscriptionPlan = plan,
                onPurchaseClick = { clicked = true },
                isPurchasing = false
            )
        }

        composeTestRule.onNodeWithText("Subscribe").performClick()
        assert(clicked)
    }

    @Test
    fun subscriptionCard_showsPurchasingState() {
        val plan = SubscriptionPlan(
            productId = "test_id",
            name = "Monthly Premium",
            description = "Unlock all features for a month",
            priceFormatted = "$4.99",
            priceMicros = 4990000L,
            currencyCode = "USD",
            isActive = false
        )

        composeTestRule.setContent {
            SubscriptionCard(
                subscriptionPlan = plan,
                onPurchaseClick = {},
                isPurchasing = true
            )
        }

        composeTestRule.onNodeWithText("Purchasing...").assertIsDisplayed()
    }
}
