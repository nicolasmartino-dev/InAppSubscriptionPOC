package com.subscription.poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.subscription.poc.domain.repository.SubscriptionRepository
import com.subscription.poc.presentation.subscription.SubscriptionScreen
import com.subscription.poc.presentation.theme.InAppSubscriptionPOCTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity hosting the Subscription UI
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize billing connection
        lifecycleScope.launch {
            subscriptionRepository.startConnection()
        }

        setContent {
            InAppSubscriptionPOCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SubscriptionScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptionRepository.endConnection()
    }
}
