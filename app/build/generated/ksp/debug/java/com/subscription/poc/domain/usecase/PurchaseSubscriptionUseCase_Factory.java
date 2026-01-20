package com.subscription.poc.domain.usecase;

import com.subscription.poc.domain.repository.SubscriptionRepository;
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
public final class PurchaseSubscriptionUseCase_Factory implements Factory<PurchaseSubscriptionUseCase> {
  private final Provider<SubscriptionRepository> repositoryProvider;

  public PurchaseSubscriptionUseCase_Factory(Provider<SubscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public PurchaseSubscriptionUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static PurchaseSubscriptionUseCase_Factory create(
      Provider<SubscriptionRepository> repositoryProvider) {
    return new PurchaseSubscriptionUseCase_Factory(repositoryProvider);
  }

  public static PurchaseSubscriptionUseCase newInstance(SubscriptionRepository repository) {
    return new PurchaseSubscriptionUseCase(repository);
  }
}
