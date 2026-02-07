package com.subscription.poc.presentation.subscription.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subscription.poc.domain.model.SubscriptionPlan

@Composable
fun CurrentSubscriptionCard(
    activeSubscriptions: List<SubscriptionPlan>,
    onManageClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Current Subscriptions. ")
                    activeSubscriptions.forEach { plan ->
                        append("${plan.name}. ")
                        if (plan.priceFormatted.isNotEmpty()) {
                            append("${plan.priceFormatted} per ${plan.billingPeriod ?: "period"}. ")
                        }
                        if (!plan.isAutoRenewing) append("Canceled. ")
                        if (plan.isPaused) append("Paused. ")
                        if (plan.isOnHold) append("On Hold. ")
                    }
                    if (activeSubscriptions.size > 1) {
                        append("Multiple plans active. Review needed.")
                    }
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Current Subscription",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            activeSubscriptions.forEach { plan ->
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.name, 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer, 
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (plan.priceFormatted.isNotEmpty()) {
                            Text(
                                text = "${plan.priceFormatted} / ${plan.billingPeriod ?: "Total"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    if (!plan.isAutoRenewing) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "CANCELED", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (plan.isPaused) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "PAUSED", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSecondaryContainer, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (plan.isOnHold) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "ON HOLD", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onTertiaryContainer, 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            if (activeSubscriptions.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: You have multiple active plans. Please cancel the old one in Google Play.", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { onManageClick(activeSubscriptions.firstOrNull()?.productId) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = "Manage Subscription / Unsubscribe")
            }
        }
    }
}
