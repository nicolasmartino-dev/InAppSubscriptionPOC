package com.subscription.poc.domain.model

sealed class PurchaseResult {
    data object Success : PurchaseResult()

    data class Error(val message: String) : PurchaseResult()

    data object UserCancelled : PurchaseResult()

    data object Pending : PurchaseResult()
}
