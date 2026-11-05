# Publication preparation

This source candidate does not establish that a Play submission has passed.

## Signed release

Use the team's own protected upload key. Android Studio: Build → Generate Signed App Bundle / APK. The existing Gradle release configuration also accepts:

```text
BUDGETWISE_KEYSTORE=<absolute existing keystore path>
BUDGETWISE_STORE_PASSWORD=<private password>
BUDGETWISE_KEY_ALIAS=<alias>
BUDGETWISE_KEY_PASSWORD=<private password>
```

Run gradlew assembleRelease bundleRelease. Supply a **signed release APK** for the assessment and a signed **AAB** for Play Console. Without signing variables, release outputs are unsigned. Keep keys/passwords outside Git.

Verify the APK with Build Tools apksigner verify --verbose --print-certs PATH_TO_APK. Capture successful verification, version and SHA-256 checksum. Register the release fingerprints with Firebase and test SSO in the signed binary.

## Before claiming readiness

- Pass automated checks and the full physical-phone script on the signed build.
- Confirm the application ID with the owner before first publication; the prototype uses com.example.budgetwisesa.
- Target SDK is 36; recheck current Play Console rules at actual submission.
- Prepare real screenshots, listing artwork/descriptions and a support contact.
- Host a reviewed privacy policy and complete accurate Data safety disclosures for accounts, financial records, receipts, Firebase and device tokens.
- Complete in-app/web account deletion and retention/receipt cleanup before public publication; full account erasure is not yet implemented.
- Complete content rating, audience, developer-account testing/verification and other applicable Console requirements.
- Confirm Firebase service quotas/billing, production rules, Storage and function deployment.
- Upload through the owner's Console and capture actual evidence. A checklist or debug APK does not prove upload.

References: [signing](https://developer.android.com/studio/publish/app-signing), [target API](https://support.google.com/googleplay/android-developer/answer/11926878), [user data](https://support.google.com/googleplay/android-developer/answer/10144311), [account deletion](https://support.google.com/googleplay/android-developer/answer/13327111).
