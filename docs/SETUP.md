# Development and Firebase setup

## Local build

Use JDK 17, SDK platform 36 and Build Tools 35.0.0. Keep machine paths in ignored local.properties. Task-local tools/cache are under ignored .tools/ and .gradle-user-home/ and must not be submitted. Android Studio's installed JDK 25 does not match this pinned build setup; select 17.

The existing real app/google-services.json is retained locally and ignored. New clones need their own Firebase Android configuration. The example file allows compilation/guest mode only.

## Owner configuration and deployment

1. Confirm the intended Firebase project. Existing local configuration identifies **smartbudgetsa-57a23**; verify it belongs to the team before changing it.
2. Register package **com.example.budgetwisesa**, add debug/release SHA-1 and SHA-256 fingerprints from gradlew signingReport, and enable Email/Password and Google Authentication. Enable Phone if demonstrating SMS; review service quotas/billing.
3. Download updated google-services.json after enabling Google. Its web OAuth client generates default_web_client_id.
4. Provision default Firestore and a Storage bucket; verify current service/billing/region requirements.
5. Review and deploy owner-only rules using an authorized Firebase CLI session:

```text
firebase deploy --only firestore:rules,storage --project YOUR_PROJECT_ID
```

6. Install/deploy the notification backend:

```text
npm ci --prefix firebase/functions
firebase deploy --only functions:notifyAccountChange --project YOUR_PROJECT_ID
```

These commands change cloud state; this review has not run them. Never put service-account files, FCM server keys or passwords in Android source.

7. Sign in on a physical phone and enable notifications in Settings; grant Android permission. Tokens register under users/{uid}/devices/{tokenHash}.
8. Create two transactions and a receipt. Show distinct UUID records in Firestore and an actual Storage object.
9. Change financial data from a second signed-in device or authorized console to demonstrate cloud-originated push. The backend sends **data-only** messages; the client checks UID and supplies localized text. Notification-only console messages may bypass background app filtering; use the supplied backend for evidence.

## Data and synchronization

Firestore: users/{uid}/transactions/{uuid}, budgets/{uuid}, goals/{uuid}. Records contain revision and deleted. Images: users/{uid}/receipts/{transactionId}/{revision}, up to 10 MiB, JPG/PNG/WebP. Financial REST requests use a Firebase user ID token and obey Security Rules.

Original-device receipt copies stay private and available offline. Storage download URLs contain bearer tokens; keep them out of public recordings/documents. A new device needs internet for its first receipt view.

Sign in online once; airplane mode then allows financial create/edit/delete. Room persists changes across process death. Network-constrained WorkManager uploads pending revisions, then pulls paginated remote records. Correct configuration errors and press Sync now to request another attempt. Android may delay background work.

The last successful server write wins when devices edit the same record. Pending local changes are protected during pulls. Tombstones/older image versions are retained. A logout attempts token removal; offline stale registrations can remain, but old-account data messages are ignored by the client.

The old prototype database has no ownership field and is preserved untouched. Revised accounts use new files. A supervised migration must first establish ownership. Surviving legacy cloud documents can be read with metadata defaults; overwritten records need backups to recover.

## Tests

```text
gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
gradlew connectedDebugAndroidTest
node --test firebase/functions/push.test.js
npm ci --prefix firebase/rules-tests
```

From firebase/rules-tests:

```text
npx firebase emulators:exec --config ../../firebase.json --project demo-budgetwise --only firestore,storage "npm test"
```

The demo project/emulators keep rule tests away from production. Emulator tests do not replace the assessment's physical-phone evidence.
