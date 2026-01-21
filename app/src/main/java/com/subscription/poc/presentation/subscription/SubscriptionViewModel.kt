package com.subscription.poc.presentation.subscription

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.domain.model.SubscriptionPlan
import com.subscription.poc.domain.usecase.GetActiveSubscriptionsUseCase
import com.subscription.poc.domain.usecase.GetSubscriptionPlansUseCase
import com.subscription.poc.domain.usecase.IsSandboxDemoModeUseCase
import com.subscription.poc.domain.usecase.ManageSubscriptionUseCase
import com.subscription.poc.domain.usecase.ObservePurchaseUpdatesUseCase
import com.subscription.poc.domain.usecase.PurchaseSubscriptionUseCase
import com.subscription.poc.domain.usecase.SetSandboxDemoModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    val activeSubscriptions: List<SubscriptionPlan> = emptyList(),
    val errorMessage: String? = null,
    val purchaseInProgress: Boolean = false,
    val lastPurchaseResult: PurchaseResult? = null,
    val isHorizontalLayout: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSandboxDemoMode: Boolean = true,
)

@HiltViewModel
class SubscriptionViewModel
@Inject
constructor(
    private val getSubscriptionPlansUseCase: GetSubscriptionPlansUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val getActiveSubscriptionsUseCase: GetActiveSubscriptionsUseCase,
    private val observePurchaseUpdatesUseCase: ObservePurchaseUpdatesUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    private val isSandboxDemoModeUseCase: IsSandboxDemoModeUseCase,
    private val setSandboxDemoModeUseCase: SetSandboxDemoModeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SubscriptionUiState(
        isSandboxDemoMode = isSandboxDemoModeUseCase()
    ))
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptionPlans()
        loadActiveSubscriptions()
        observePurchaseUpdates()
    }

    private fun observePurchaseUpdates() {
        viewModelScope.launch {
            observePurchaseUpdatesUseCase().collect { result ->
                 _uiState.update {
                    it.copy(
                        purchaseInProgress = result is PurchaseResult.Pending,
                        lastPurchaseResult = result
                    )
                }
                
                if (result is PurchaseResult.Success) {
                    loadSubscriptionPlans()
                    loadActiveSubscriptions()
                }
            }
        }
    }

    fun loadSubscriptionPlans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            getSubscriptionPlansUseCase()
                .onSuccess { plans ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            subscriptionPlans = plans,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load subscription plans",
                        )
                    }
                }
        }
    }

    fun loadActiveSubscriptions() {
        viewModelScope.launch {
            getActiveSubscriptionsUseCase()
                .onSuccess { subscriptions ->
                    _uiState.update {
                        it.copy(activeSubscriptions = subscriptions)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(activeSubscriptions = emptyList())
                    }
                }
        }
    }

    fun purchaseSubscription(
        activity: Activity,
        productId: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    purchaseInProgress = true,
                    errorMessage = null,
                    lastPurchaseResult = null,
                )
            }

            val result = purchaseSubscriptionUseCase(activity, productId)

            _uiState.update {
                it.copy(
                    purchaseInProgress = false,
                    lastPurchaseResult = result,
                    errorMessage = if (result is PurchaseResult.Error) result.message else it.errorMessage
                )
            }

            if (result is PurchaseResult.Success) {
                loadSubscriptionPlans()
                loadActiveSubscriptions()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleLayoutMode() {
        _uiState.update { it.copy(isHorizontalLayout = !it.isHorizontalLayout) }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            
            val plansDeferred = viewModelScope.launch { loadSubscriptionPlans() }
            val activeDeferred = viewModelScope.launch { loadActiveSubscriptions() }
            
            plansDeferred.join()
            activeDeferred.join()
            
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearPurchaseResult() {
        _uiState.update { it.copy(lastPurchaseResult = null) }
    }

    fun manageSubscription(context: Context, productId: String? = null) {
        manageSubscriptionUseCase(context, productId)
    }

    fun toggleSandboxMode(enabled: Boolean) {
        setSandboxDemoModeUseCase(enabled)
        _uiState.update { it.copy(isSandboxDemoMode = enabled) }
    }
}
