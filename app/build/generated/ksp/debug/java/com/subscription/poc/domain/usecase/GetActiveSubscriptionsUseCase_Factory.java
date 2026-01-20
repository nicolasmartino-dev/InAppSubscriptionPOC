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
public final class GetActiveSubscriptionsUseCase_Factory implements Factory<GetActiveSubscriptionsUseCase> {
  private final Provider<SubscriptionRepository> repositoryProvider;

  public GetActiveSubscriptionsUseCase_Factory(
      Provider<SubscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetActiveSubscriptionsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetActiveSubscriptionsUseCase_Factory create(
      Provider<SubscriptionRepository> repositoryProvider) {
    return new GetActiveSubscriptionsUseCase_Factory(repositoryProvider);
  }

  public static GetActiveSubscriptionsUseCase newInstance(SubscriptionRepository repository) {
    return new GetActiveSubscriptionsUseCase(repository);
  }
}
