package com.subscription.poc;

import com.subscription.poc.domain.repository.SubscriptionRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  public MainActivity_MembersInjector(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    return new MainActivity_MembersInjector(subscriptionRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSubscriptionRepository(instance, subscriptionRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.subscription.poc.MainActivity.subscriptionRepository")
  public static void injectSubscriptionRepository(MainActivity instance,
      SubscriptionRepository subscriptionRepository) {
    instance.subscriptionRepository = subscriptionRepository;
  }
}
