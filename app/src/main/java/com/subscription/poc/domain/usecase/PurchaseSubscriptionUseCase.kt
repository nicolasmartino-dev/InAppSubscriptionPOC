package com.subscription.poc.domain.usecase

import android.app.Activity
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.repository.SubscriptionRepository
import javax.inject.Inject

class PurchaseSubscriptionUseCase
@Inject
constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(
        activity: Activity,
        productId: String,
    ): PurchaseResult {
        return repository.purchaseSubscription(activity, productId)
    }
}
