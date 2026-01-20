package com.subscription.poc.presentation.subscription

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.subscription.poc.domain.model.PurchaseResult
import com.subscription.poc.presentation.subscription.components.CurrentSubscriptionCard
import com.subscription.poc.presentation.subscription.components.SubscriptionCard

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun SubscriptionScreen(viewModel: SubscriptionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadActiveSubscriptions()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastPurchaseResult) {
        uiState.lastPurchaseResult?.let { result ->
            val message = when (result) {
                is PurchaseResult.Success -> "Subscription purchased successfully!"
                is PurchaseResult.Error -> "Error: ${result.message}"
                is PurchaseResult.UserCancelled -> "Purchase cancelled"
                is PurchaseResult.Pending -> "Purchase pending..."
            }
            snackbarHostState.showSnackbar(message)
            viewModel.clearPurchaseResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subscription Plans",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                        Icon(
                            imageVector = if (uiState.isHorizontalLayout) 
                                Icons.AutoMirrored.Filled.List 
                            else 
                                Icons.Default.ViewCarousel,
                            contentDescription = if (uiState.isHorizontalLayout) "List Layout" else "Carousel Layout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("LOADING_INDICATOR"),
                    )
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSubscriptionPlans() }) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    if (uiState.isHorizontalLayout) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Choose Your Plan",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select a subscription plan to get started",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            
                             if (uiState.activeSubscriptions.isNotEmpty()) {
                                  CurrentSubscriptionCard(
                                     activeSubscriptions = uiState.activeSubscriptions,
                                     onManageClick = { productId -> viewModel.manageSubscription(context, productId) }
                                  )
                             }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                items(uiState.subscriptionPlans) { plan ->
                                    Box(modifier = Modifier.width(280.dp)) {
                                        SubscriptionCard(
                                            subscriptionPlan = plan,
                                            onPurchaseClick = { activity?.let { viewModel.purchaseSubscription(it, plan.productId) } },
                                            isPurchasing = uiState.purchaseInProgress,
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "• Subscriptions auto-renew monthly\n• Cancel anytime from your Google Play account\n• Prices include applicable taxes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            item {
                                Column {
                                    Text(
                                        text = "Choose Your Plan",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Select a subscription plan to get started",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (uiState.activeSubscriptions.isNotEmpty()) {
                                item {
                                     CurrentSubscriptionCard(
                                         activeSubscriptions = uiState.activeSubscriptions,
                                         onManageClick = { productId -> viewModel.manageSubscription(context, productId) }
                                     )
                                }
                            }

                            items(uiState.subscriptionPlans) { plan ->
                                SubscriptionCard(
                                    subscriptionPlan = plan,
                                    onPurchaseClick = {
                                        activity?.let {
                                            viewModel.purchaseSubscription(it, plan.productId)
                                        }
                                    },
                                    isPurchasing = uiState.purchaseInProgress,
                                )
                            }

                            item {
                                Text(
                                    text = "• Subscriptions auto-renew monthly\n" +
                                            "• Cancel anytime from your Google Play account\n" +
                                            "• Prices include applicable taxes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
