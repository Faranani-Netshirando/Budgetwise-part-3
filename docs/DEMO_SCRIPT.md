# Narrated physical-phone demonstration

Record the revised app on a **physical Android phone**, explaining each action in your own voice. Use fictional records and a test account. Hide credentials, emails, tokens and private receipts.

| Approximate time | Action | Voice-over/evidence |
|---|---|---|
| 0:00–0:30 | Launcher/dashboard | Purpose, phone model and app version; rand budgeting, expenses and savings |
| 0:30–1:20 | Registration/email login/Google SSO | Invalid input, successful registration, sign out and both actual login methods; Firebase manages credentials |
| 1:20–2:20 | Transactions | Add income R5,000 and Food expense R450; choose date; attach harmless receipt; search/filter/edit/delete another item |
| 2:20–3:00 | Budgets/goals | Food budget R500 shows 90% warning; edit expense to demonstrate recalculation; add savings target/deadline/progress |
| 3:00–3:40 | Reports/settings | Explain chart and earned badges; change theme, threshold and languages; restart to show persistence |
| 3:40–4:50 | Offline and reconnect | Stay signed in, airplane mode, create/edit/delete, close/reopen and show pending records; restore connectivity, sync and show pending count reaching zero |
| 4:50–5:40 | Firebase console | Show Authentication test user, separate Firestore UUID records and receipt Storage object; explain REST/user rules without revealing tokens |
| 5:40–6:20 | Real FCM push | With opt-in enabled, change cloud financial data from another device/authorized console; show received push foreground/background; explain trusted backend |
| 6:20–7:00 | GitHub/release preparation | Show source, README, release notes, successful Actions, final tag, signed artifact and any completed Play Console preparation |

A local budget warning is not proof of remote push. An emulator is not proof of physical-phone execution. Show a second account/guest with no access to the first account's data. Include invalid inputs without crashing.

Capture actual screenshots of launcher/auth, dashboard, financial screens/receipt, both additional languages, dark theme, offline pending/completed sync, Firebase Auth/Firestore/Storage, real FCM notification, successful Actions/tag, signed release verification and any completed Play preparation.

Put captures in docs/evidence/, embed selected images in README, and update the README video URL after recording. Do not substitute diagrams for runtime evidence.

The owner subsequently requested upload to Faranani-Netshirando/Budgetwise-part-3. Capture the actual repository, hosted Actions result and final tag when available; do not substitute local test results for a hosted run.
