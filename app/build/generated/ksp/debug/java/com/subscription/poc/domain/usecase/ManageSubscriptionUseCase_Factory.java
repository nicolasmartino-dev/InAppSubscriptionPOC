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
public final class ManageSubscriptionUseCase_Factory implements Factory<ManageSubscriptionUseCase> {
  private final Provider<SubscriptionRepository> repositoryProvider;

  public ManageSubscriptionUseCase_Factory(Provider<SubscriptionRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ManageSubscriptionUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ManageSubscriptionUseCase_Factory create(
      Provider<SubscriptionRepository> repositoryProvider) {
    return new ManageSubscriptionUseCase_Factory(repositoryProvider);
  }

  public static ManageSubscriptionUseCase newInstance(SubscriptionRepository repository) {
    return new ManageSubscriptionUseCase(repository);
  }
}
