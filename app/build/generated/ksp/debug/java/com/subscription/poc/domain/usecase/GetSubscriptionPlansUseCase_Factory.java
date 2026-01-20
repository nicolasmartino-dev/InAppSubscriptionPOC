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
public final class GetSubscriptionPlansUseCase_Factory implements Factory<GetSubscriptionPlansUseCase> {
  private final Provider<SubscriptionRepository> repositoryProvider;

  public GetSubscriptionPlansUseCase_Factory(Provider<SubscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetSubscriptionPlansUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetSubscriptionPlansUseCase_Factory create(
      Provider<SubscriptionRepository> repositoryProvider) {
    return new GetSubscriptionPlansUseCase_Factory(repositoryProvider);
  }

  public static GetSubscriptionPlansUseCase newInstance(SubscriptionRepository repository) {
    return new GetSubscriptionPlansUseCase(repository);
  }
}
