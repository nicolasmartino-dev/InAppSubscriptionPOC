package com.subscription.poc.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.subscription.poc.domain.repository.SubscriptionRepository
import javax.inject.Inject

class ManageSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(context: Context, productId: String? = null) {
        val packageName = context.packageName
        val url = if (productId != null) {
            "https://play.google.com/store/account/subscriptions?sku=$productId&package=$packageName"
        } else {
            "https://play.google.com/store/account/subscriptions"
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
