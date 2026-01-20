package com.subscription.poc.presentation.subscription;

import com.subscription.poc.domain.usecase.GetActiveSubscriptionsUseCase;
import com.subscription.poc.domain.usecase.GetSubscriptionPlansUseCase;
import com.subscription.poc.domain.usecase.ManageSubscriptionUseCase;
import com.subscription.poc.domain.usecase.ObservePurchaseUpdatesUseCase;
import com.subscription.poc.domain.usecase.PurchaseSubscriptionUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SubscriptionViewModel_Factory implements Factory<SubscriptionViewModel> {
  private final Provider<GetSubscriptionPlansUseCase> getSubscriptionPlansUseCaseProvider;

  private final Provider<PurchaseSubscriptionUseCase> purchaseSubscriptionUseCaseProvider;

  private final Provider<GetActiveSubscriptionsUseCase> getActiveSubscriptionsUseCaseProvider;

  private final Provider<ObservePurchaseUpdatesUseCase> observePurchaseUpdatesUseCaseProvider;

  private final Provider<ManageSubscriptionUseCase> manageSubscriptionUseCaseProvider;

  public SubscriptionViewModel_Factory(
      Provider<GetSubscriptionPlansUseCase> getSubscriptionPlansUseCaseProvider,
      Provider<PurchaseSubscriptionUseCase> purchaseSubscriptionUseCaseProvider,
      Provider<GetActiveSubscriptionsUseCase> getActiveSubscriptionsUseCaseProvider,
      Provider<ObservePurchaseUpdatesUseCase> observePurchaseUpdatesUseCaseProvider,
      Provider<ManageSubscriptionUseCase> manageSubscriptionUseCaseProvider) {
    this.getSubscriptionPlansUseCaseProvider = getSubscriptionPlansUseCaseProvider;
    this.purchaseSubscriptionUseCaseProvider = purchaseSubscriptionUseCaseProvider;
    this.getActiveSubscriptionsUseCaseProvider = getActiveSubscriptionsUseCaseProvider;
    this.observePurchaseUpdatesUseCaseProvider = observePurchaseUpdatesUseCaseProvider;
    this.manageSubscriptionUseCaseProvider = manageSubscriptionUseCaseProvider;
  }

  @Override
  public SubscriptionViewModel get() {
    return newInstance(getSubscriptionPlansUseCaseProvider.get(), purchaseSubscriptionUseCaseProvider.get(), getActiveSubscriptionsUseCaseProvider.get(), observePurchaseUpdatesUseCaseProvider.get(), manageSubscriptionUseCaseProvider.get());
  }

  public static SubscriptionViewModel_Factory create(
      Provider<GetSubscriptionPlansUseCase> getSubscriptionPlansUseCaseProvider,
      Provider<PurchaseSubscriptionUseCase> purchaseSubscriptionUseCaseProvider,
      Provider<GetActiveSubscriptionsUseCase> getActiveSubscriptionsUseCaseProvider,
      Provider<ObservePurchaseUpdatesUseCase> observePurchaseUpdatesUseCaseProvider,
      Provider<ManageSubscriptionUseCase> manageSubscriptionUseCaseProvider) {
    return new SubscriptionViewModel_Factory(getSubscriptionPlansUseCaseProvider, purchaseSubscriptionUseCaseProvider, getActiveSubscriptionsUseCaseProvider, observePurchaseUpdatesUseCaseProvider, manageSubscriptionUseCaseProvider);
  }

  public static SubscriptionViewModel newInstance(
      GetSubscriptionPlansUseCase getSubscriptionPlansUseCase,
      PurchaseSubscriptionUseCase purchaseSubscriptionUseCase,
      GetActiveSubscriptionsUseCase getActiveSubscriptionsUseCase,
      ObservePurchaseUpdatesUseCase observePurchaseUpdatesUseCase,
      ManageSubscriptionUseCase manageSubscriptionUseCase) {
    return new SubscriptionViewModel(getSubscriptionPlansUseCase, purchaseSubscriptionUseCase, getActiveSubscriptionsUseCase, observePurchaseUpdatesUseCase, manageSubscriptionUseCase);
  }
}
