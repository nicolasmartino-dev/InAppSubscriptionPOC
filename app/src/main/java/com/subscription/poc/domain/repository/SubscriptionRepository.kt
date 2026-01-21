package com.subscription.poc.domain.repository

import android.app.Activity
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    suspend fun getSubscriptionPlans(): Result<List<SubscriptionPlan>>

    suspend fun purchaseSubscription(
        activity: Activity,
        productId: String,
    ): PurchaseResult

    suspend fun getActiveSubscriptions(): Result<List<SubscriptionPlan>>

    fun observePurchaseUpdates(): Flow<PurchaseResult>

    fun saveLastSelectedPlanId(planId: String)
    
    suspend fun startConnection(): Result<Unit>

    fun isSandboxDemoMode(): Boolean
    fun setSandboxDemoMode(enabled: Boolean)
    fun endConnection()
}
