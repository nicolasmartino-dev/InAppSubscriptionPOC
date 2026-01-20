package com.subscription.poc.domain.usecase

import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePurchaseUpdatesUseCase
    @Inject
    constructor(
        private val repository: SubscriptionRepository,
    ) {
        operator fun invoke(): Flow<PurchaseResult> {
            return repository.observePurchaseUpdates()
        }
    }
