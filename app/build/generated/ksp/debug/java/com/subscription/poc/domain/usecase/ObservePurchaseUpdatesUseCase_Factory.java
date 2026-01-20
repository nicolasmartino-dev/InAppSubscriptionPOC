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
public final class ObservePurchaseUpdatesUseCase_Factory implements Factory<ObservePurchaseUpdatesUseCase> {
  private final Provider<SubscriptionRepository> repositoryProvider;

  public ObservePurchaseUpdatesUseCase_Factory(
      Provider<SubscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ObservePurchaseUpdatesUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ObservePurchaseUpdatesUseCase_Factory create(
      Provider<SubscriptionRepository> repositoryProvider) {
    return new ObservePurchaseUpdatesUseCase_Factory(repositoryProvider);
  }

  public static ObservePurchaseUpdatesUseCase newInstance(SubscriptionRepository repository) {
    return new ObservePurchaseUpdatesUseCase(repository);
  }
}
