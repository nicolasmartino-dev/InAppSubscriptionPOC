package com.subscription.poc.domain.usecase

import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.repository.SubscriptionRepository
import javax.inject.Inject

class GetSubscriptionPlansUseCase
@Inject
constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Result<List<SubscriptionPlan>> {
        return repository.getSubscriptionPlans()
    }
}
