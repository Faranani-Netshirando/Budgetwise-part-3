# BudgetWise SA 1.1.1 — final PoE candidate

5 November 2026. Version code 3. Compared with the supplied prototype; no earlier release tag or lecturer feedback was supplied. Version 1.1.1 replaces the earlier candidate's UI with Android Views.

## Added

- Replaced the UI with XML layouts, Android Views, View Binding, RecyclerView, native edit dialogs and a native Canvas chart. Removed all Compose UI code, compiler configuration and dependencies. Added day/night layout-inflation and native navigation/form tests.

- Actual authenticated Firestore REST reads/writes, pagination and stable UUID documents.
- Account-scoped Room storage, pending revisions, deletion tombstones, safe acknowledgements and WorkManager reconnection/retry/periodic sync.
- Firebase Storage receipt uploads, private local copies, export, and transaction/goal date pickers.
- Android FCM handling, notification opt-in/permission/channel, device registration and a deployable account-change push function.
- Persistent theme/language/threshold/notification settings and English/Tshivenda/isiZulu resources.
- Financial, offline Room, backend and security-rule tests with GitHub Actions report/APK artifacts.
- Environment-based release signing and publication/evidence instructions.
- Wallet/rand launcher artwork and architecture documentation.

## Fixed

Unsaved auto-generated IDs no longer cause cloud documents to overwrite each other. Local records are isolated by account and guest. Transaction edits/deletes update calendar budget totals. Invalid, negative, zero, non-finite and excessively large amounts cannot be saved. Badges require activity. Settings/manual sync perform real work. Activity recreation no longer signs the user out. Google SSO reads the configured web client ID. Unused automatic Analytics collection was removed.

## Assessment additions and limits

The additions over the prototype include completed offline reconciliation, trusted push backend/permission-aware handling, another language, durable blob receipts, activity-based badges and tested account separation. These are source changes, not proof of cloud deployment or physical-device demonstration.

Remaining: owner Firebase deployment/testing, language review, Part 1/lecturer feedback review, real screenshots, new narrated phone video, owner-signed release/Play preparation and production privacy/account-deletion/retention work. The owner subsequently requested upload to Faranani-Netshirando/Budgetwise-part-3. Hosted Actions results and the final PoE tag must be verified separately.
