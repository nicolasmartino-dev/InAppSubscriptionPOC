package com.subscription.poc.domain.usecase

import com.subscription.poc.domain.repository.SubscriptionRepository
import javax.inject.Inject

class IsSandboxDemoModeUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(): Boolean = repository.isSandboxDemoMode()
}
