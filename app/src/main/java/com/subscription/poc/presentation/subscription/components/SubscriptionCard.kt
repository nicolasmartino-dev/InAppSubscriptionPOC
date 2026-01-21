package com.subscription.poc.presentation.subscription.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subscription.poc.domain.model.SubscriptionPlan

@Suppress("ktlint:standard:function-naming")
@Composable
fun SubscriptionCard(
    subscriptionPlan: SubscriptionPlan,
    onPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    buttonText: String = "Subscribe",
    buttonColor: Color? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (subscriptionPlan.isActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        border =
            if (subscriptionPlan.isActive) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = subscriptionPlan.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (subscriptionPlan.billingPeriod != null) {
                    "${subscriptionPlan.priceFormatted} / ${subscriptionPlan.billingPeriod}"
                } else {
                    subscriptionPlan.priceFormatted
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subscriptionPlan.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (subscriptionPlan.isActive) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "✓ Active",
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { 
                                contentDescription = "Currently active subscription plan" 
                            },
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Button(
                    onClick = onPurchaseClick,
                    enabled = !isPurchasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { 
                            contentDescription = "Purchase ${subscriptionPlan.name} plan" 
                        },
                    colors = if (buttonColor != null) {
                        ButtonDefaults.buttonColors(containerColor = buttonColor)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isPurchasing) "Purchasing..." else buttonText,
                    )
                }
            }
        }
    }
}
