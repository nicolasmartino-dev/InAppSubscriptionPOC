package com.subscription.poc.domain.model

data class SubscriptionPlan(
    val productId: String,
    val name: String,
    val description: String,
    val priceFormatted: String,
    val priceMicros: Long,
    val currencyCode: String,
    val isActive: Boolean = false,
    val isAutoRenewing: Boolean = true,
    val billingPeriod: String? = null,
    val isPaused: Boolean = false,
    val isOnHold: Boolean = false,
)
