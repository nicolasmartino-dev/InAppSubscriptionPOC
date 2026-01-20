package com.subscription.poc.data.repository;

import android.content.Context;
import com.subscription.poc.data.billing.BillingClientWrapper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SubscriptionRepositoryImpl_Factory implements Factory<SubscriptionRepositoryImpl> {
  private final Provider<BillingClientWrapper> billingClientWrapperProvider;

  private final Provider<Context> contextProvider;

  public SubscriptionRepositoryImpl_Factory(
      Provider<BillingClientWrapper> billingClientWrapperProvider,
      Provider<Context> contextProvider) {
    this.billingClientWrapperProvider = billingClientWrapperProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public SubscriptionRepositoryImpl get() {
    return newInstance(billingClientWrapperProvider.get(), contextProvider.get());
  }

  public static SubscriptionRepositoryImpl_Factory create(
      Provider<BillingClientWrapper> billingClientWrapperProvider,
      Provider<Context> contextProvider) {
    return new SubscriptionRepositoryImpl_Factory(billingClientWrapperProvider, contextProvider);
  }

  public static SubscriptionRepositoryImpl newInstance(BillingClientWrapper billingClientWrapper,
      Context context) {
    return new SubscriptionRepositoryImpl(billingClientWrapper, context);
  }
}
