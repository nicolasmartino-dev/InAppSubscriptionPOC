# Google Play Console Setup Guide

This guide walks you through setting up in-app subscriptions in Google Play Console for the Subscription POC app.

## Prerequisites

- Google Play Developer account ($25 one-time registration fee)
- APK or AAB file of your app (signed with release keystore)
- Valid payment information in your Google Play account

## Step 1: Create Your App in Google Play Console
## 1. Create App in Google Play Console

1. Log in to [Google Play Console](https://play.google.com/console).
2. Click **Create app**.
3. **App Name**: `InAppSubscriptionPOC`
4. **Default language**: `English (United States)`
5. **App or game**: `App`
6. **Free or paid**: `Free`
7. **Declarations**: Check all boxes (Developer Program Policies, US export laws).
8. Click **Create app`.

## 2. Generate and Upload App Bundle (Required First!)

**CRITICAL**: You MUST upload a binary with the `com.android.vending.BILLING` permission before Google Play allows you to create subscription products.

### A. Generate Signed Bundle
I have already configured the signing for you. Run this command to generate the release bundle:
```bash
./gradlew bundleRelease
```
**File Location**: `app/build/outputs/bundle/release/app-release.aab`

### B. Upload to Internal Testing
1. In Play Console sidebar, go to **Testing** > **Internal testing**.
2. Click **Create new release**.
3. **Signing key**: Click "Choose signing key" -> "Use Google-generated key" (easiest for POC).
4. **App bundles**: Drag and drop the `app-release.aab` file you just generated.
5. **Release Name**: `1.0 POC`
6. Click **Next** -> **Save** -> **Publish release** (or "Start rollout to Internal testing").

> **Note**: Even for internal testing, you might need to fill out some "Dashboard" tasks (like Data Safety, Privacy Policy) if the console blocks you. For a POC, you can often pick "Draft" or fill dummy values.

## 3. Create Subscription Products

1. **Navigate to Monetization**
   - In left sidebar: **Monetize** → **Products** → **Subscriptions**
   - Click "Create subscription"

2. **Create First Subscription - "First"**
   - **Product ID**: `subscription_first` (MUST be exactly this)
   - **Name**: First
   - **Description**: Basic subscription plan
   - Click "Save"

3. **Add Base Plan**
   - Click "Add base plan"
   - **Base plan ID**: first-monthly (or your choice)
   - **Billing period**: 1 Month
   - **Price**: Create new price
     - Select countries (add Canada)
     - Set price: $4.99 CAD
   - **Subscription benefits** (optional): Add benefits description
   - Click "Save"

4. **Activate the Product**
   - Click "Activate" on the subscription product page

5. **Repeat for Second and Bundle Subscriptions**

   **Second Subscription:**
   - **Product ID**: `subscription_second`
   - **Name**: Second
   - **Description**: Premium subscription plan
   - **Base plan**: second-monthly
   - **Price**: $5.99 CAD
   - Activate

   **Bundle Subscription:**
   - **Product ID**: `subscription_bundle`
   - **Name**: Bundle
   - **Description**: Complete bundle subscription
   - **Base plan**: bundle-monthly
   - **Price**: $8.99 CAD
   - Activate

## Step 3: Generate a Release Keystore (If You Don't Have One)

```bash
keytool -genkey -v -keystore subscription-poc-release.keystore \
  -alias subscription-poc -keyalg RSA -keysize 2048 -validity 10000
```

**IMPORTANT**: Save the keystore file and password securely. You'll need this for all future updates.

## Step 4: Build a Signed Release APK/AAB

1. **In Android Studio**:
   - **Build** → **Generate Signed Bundle / APK**
   - Select "Android App Bundle" (recommended) or "APK"
   - Select your keystore file
   - Enter keystore password and key password
   - Select "release" build variant
   - Click "Finish"

2. **Locate the build output**:
   - AAB: `app/build/outputs/bundle/release/app-release.aab`
   - APK: `app/build/outputs/apk/release/app-release.apk`

## Step 5: Upload to Internal Testing Track

1. **Create an Internal Testing Release**
   - In Play Console: **Testing** → **Internal testing**
   - Click "Create new release"

2. **Upload Your App**
   - Click "Upload" and select your AAB or APK
   - Wait for upload to complete
   - Add release notes (e.g., "Initial test release for subscriptions")
   - Click "Save"

3. **Add Testers to the Track (CRITICAL STEP)**
   - In the "Internal testing" page, switch to the **Testers** tab.
   - You will see your email list (e.g., "Testers").
   - **YOU MUST TICK THE CHECKBOX** next to your list name. If this box is empty, you will see "App not available".
   - Click **Save**.

4. **Review and Roll Out**
   - Go back to the **Releases** tab.
   - Click **Edit release** (if needed) -> **Next** -> **Start rollout to Internal testing**.

## Step 6: Add License Testers

1. **Navigate to License Testing (Account Level)**
   - Click **All apps** (Toutes les applications) at the top left to return to the main dashboard.
   - In the main sidebar (not the app sidebar), scroll down to **Setup** (Configuration).
   - Click **License testing** (Test des licences).

2. **Add Test Accounts**
   - In "License testers" section, click "Add license testers"
   - Enter Gmail addresses (comma-separated):
     ```
     your.email@gmail.com, tester1@gmail.com, tester2@gmail.com
     ```
   - Click "Save changes"

3. **Important Notes**:
   - Testers must use these exact Gmail accounts on their devices
   - Changes may take up to 24 hours to propagate
   - Test purchases are not charged (they're free)

## Step 7: Install the Test App

1. **Share the Internal Testing Link**
   - Go to **Testing** → **Internal testing**
   - Copy the "Copy opt-in URL"
   - Open this URL on your test device (must be logged in with a tester account)

2. **Opt-in and Install**
   - You'll see a page to become a tester
   - Click "Become a tester"
   - Click "Download it on Google Play"
   - Install the app

## Step 8: Test Subscription Flow

1. **Launch the App**
   - Open "Subscription POC" on your test device
   - You should see three subscription options

2. **Test a Purchase**
   - Tap "Subscribe" on any plan
   - Google Play purchase dialog will appear
   - **For testers**: You'll see a message indicating this is a test purchase
   - Complete the purchase (you won't be charged)

3. **Verify Purchase**
   - The subscription should show as "Active" in the app
   - Check **Monetize** → **Orders** in Play Console to see test orders

## Troubleshooting

### "Item not available for purchase"
- Ensure subscription products are **activated** in Play Console
- Wait 24 hours after creating products
- Check product IDs match exactly in code and Play Console

### "This version of the app is not configured for billing"
- App must be signed with the same certificate uploaded to Play Console
- Package name must match Play Console exactly (`com.subscription.poc`)
- App must be installed from Play Store internal testing link

### Tester Account Not Working
- Ensure Gmail account is added to license testers
- Wait 24 hours after adding
- Tester must opt-in via the internal testing link
- Tester must be logged into that Gmail account on the device

### Products Not Loading in App
- Check internet connection
- Verify app has `BILLING` permission in manifest
- Check logcat for billing errors:
  ```bash
  adb logcat | grep -i billing
  ```

## Managing Test Subscriptions

1. **View Test Purchases**
   - Play Console → **Monetize** → **Orders**
   - Filter by test purchases

2. **Cancel Test Subscriptions**
   - On test device: Google Play Store → Account → Payments & subscriptions → Subscriptions
   - Select subscription and cancel

3. **Testing Renewal**
   - Test subscriptions have accelerated renewal cycles:
     - 1 month subscription renews every 5 minutes
     - This allows quick testing of renewal flows

## Production Checklist

Before releasing to production:

- [ ] Test all three subscription tiers
- [ ] Test purchase cancelation
- [ ] Test renewal flow
- [ ] Verify receipt validation (if implemented)
- [ ] Complete all Play Console requirements
- [ ] Set up taxes and pricing for all target countries
- [ ] Review subscription descriptions and benefits
- [ ] Test on multiple devices and Android versions
- [ ] Implement proper error handling for all billing states
- [ ] Add analytics to track subscription metrics

## Additional Resources

- [Google Play Billing Overview](https://developer.android.com/google/play/billing/integrate)
- [Testing In-App Purchases](https://developer.android.com/google/play/billing/test)
- [Subscription Best Practices](https://developer.android.com/google/play/billing/subscriptions)
- [Play Console Help Center](https://support.google.com/googleplay/android-developer)

## Support

For billing-related issues:
- Check [Play Console Help](https://support.google.com/googleplay/android-developer)
- Review [Billing Library documentation](https://developer.android.com/google/play/billing/integrate)
- Search [Stack Overflow](https://stackoverflow.com/questions/tagged/google-play-billing) with tag `google-play-billing`
