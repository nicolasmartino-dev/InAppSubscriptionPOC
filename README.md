# Android In-App Subscription POC

A proof of concept Android application demonstrating Google Play in-app subscriptions using clean architecture, Hilt dependency injection, and Jetpack Compose.

## 📋 Features

- **Clean Architecture** - Separation of concerns with Domain, Data, and Presentation layers
- **Mock Mode**: Built-in fallback to mock data if Google Play connection fails or products aren't set up.
- **Dependency Injection** - Hilt for easy testing and maintainability
- **Modern UI** - Jetpack Compose with Material 3 design
- **Google Play Billing Library 7.1.1** - Latest billing API for subscriptions
- **Comprehensive Testing** - Unit tests with MockK, Turbine, and Coroutine testing
- **Three Subscription Tiers**:
  - **First** - $4.99 CAD/month
  - **Second** - $5.99 CAD/month
  - **Bundle** - $8.99 CAD/month

## 🏗️ Architecture

```
app/
├── domain/              # Business logic layer
│   ├── model/           # Domain models (SubscriptionPlan, PurchaseResult)
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Business use cases
├── data/                # Data layer
│   ├── billing/         # BillingClientWrapper
│   └── repository/      # Repository implementations
├── presentation/        # UI layer
│   ├── subscription/    # Subscription screen & ViewModel
│   └── theme/           # Material 3 theme
└── di/                  # Hilt modules
```

### Key Components

- **BillingClientWrapper**: Manages Google Play Billing client lifecycle and operations
- **SubscriptionRepository**: Interface for subscription operations
- **SubscriptionViewModel**: Manages UI state and coordinates use cases
- **SubscriptionScreen**: Jetpack Compose UI for subscription selection

## 🚀 Setup

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 35
- JDK 17
- Gradle 8.5+

### Building the App

1. Clone or open the project in Android Studio
2. Sync Gradle dependencies
3. Build the project:
   ```bash
   ./gradlew build
   ```

## 🧪 Running Tests

Run all unit tests:
```bash
./gradlew test
```

Run tests with coverage:
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

## 📱 Google Play Console Setup

**IMPORTANT**: To test in-app subscriptions, you must configure products in Google Play Console and upload the app to internal testing. See [GOOGLE_PLAY_SETUP.md](GOOGLE_PLAY_SETUP.md) for detailed step-by-step instructions.

### Quick Overview

1. Create a Google Play Console account
2. Create a new app
3. Create three subscription products with these **exact** IDs:
   - `subscription_first`
   - `subscription_second`
   - `subscription_bundle`
4. Configure base plans with monthly pricing
5. Upload app to internal testing track
6. Add license testers
7. Test the subscription flow

## 🔧 Technology Stack

| Component | Library | Version |
|-----------|---------|---------|
| Language | Kotlin | 2.0.21 |
| Build | Gradle | 8.5 |
| UI | Jetpack Compose | 2024.12.01 (BOM) |
| DI | Hilt | 2.53.1 |
| Billing | Google Play Billing | 7.1.1 |
| Coroutines | Kotlinx Coroutines | 1.9.0 |
| Testing | JUnit 4 | 4.13.2 |
| Mocking | MockK | 1.13.13 |
| Flow Testing | Turbine | 1.2.0 |

## 📝 Subscription Product IDs

The app is configured with these product IDs (must match Google Play Console):

```kotlin
subscription_first    // $4.99 CAD - First package
subscription_second   // $5.99 CAD - Second package
subscription_bundle   // $8.99 CAD - Bundle package
```

## 🎯 Usage

1. **Launch the app** - The app will automatically connect to Google Play Billing
2. **View available subscriptions** - Three subscription tiers are displayed
3. **Select a subscription** - Tap "Subscribe" to initiate purchase flow
4. **Complete purchase** - Follow Google Play payment flow
5. **View active subscriptions** - Active subscriptions are highlighted with a checkmark

## 🐛 Troubleshooting

### Billing Client Not Ready
- Ensure the app is signed with the same certificate as uploaded to Play Console
- Verify the app package name matches Play Console configuration
- Check that billing permission is in AndroidManifest.xml

### Products Not Found
- Verify product IDs in Google Play Console match exactly
- Ensure products are activated in Play Console
- Confirm app is published to internal testing track

### Test Purchases Not Working
- Add your Google account to license testers in Play Console
- Wait 24 hours after adding license testers
- Ensure you're using a real device (not emulator for billing)

## 📖 Additional Resources

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)

## 📄 License

This is a proof of concept application for demonstration purposes.
