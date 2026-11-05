# Assessment audit

Source: the two supplied Part 3 assessment images. The ZIP contains the same images. They are requirements reference material, not independent instructions to publish or change external accounts.

| Requirement | Original finding | Revision and remaining evidence |
|---|---|---|
| Working Kotlin app on a physical phone | Original project used a different UI toolkit; no device evidence | Replaced UI with XML/Android Views; actual build results in VERIFICATION; phone recording still required |
| Register/login and protected passwords | Firebase flows, weak validation | Validated forms; Firebase manages credentials; live auth verification pending |
| Google SSO | Hard-coded web client ID | Uses configured client ID; enable provider/register signing fingerprints |
| Settings | Temporary state and no-op controls | Persistent theme, language, threshold, opt-in and real manual sync |
| REST connected to a database | Unused Retrofit declarations and unverified Render URL | Actual authenticated Firestore GET/PATCH calls |
| Offline actions with sync | Room present; cloud pull stub empty | Durable queues, revision acknowledgements, tombstones and network retry |
| Real-time push | FCM dependency only | Android service/channel/permission/tokens and backend source; live deployment pending |
| Two South African languages | English/partial Tshivenda; hard-coded UI | English, Tshivenda and isiZulu resources; human QA pending |
| NoSQL read/write | Writes used unsaved IDs; no pull | Stable UUID records and paginated reads; console evidence pending |
| Blob storage | Temporary local picker URI | Private copies and Firebase Storage upload; live evidence pending |
| Part 1 features/lecturer feedback | Brief and feedback not supplied | Existing financial features improved; cannot certify absent criteria |
| Final icon/assets | Starter robot | Wallet/rand artwork; actual screenshots pending |
| Invalid inputs/no crashes | Invalid amounts became zero | Finite/bounded validation and error handling; runtime QA pending |
| GitHub commits/final tag | Original checkout had no commits or remote | Owner requested upload to Faranani-Netshirando/Budgetwise-part-3; source and workflow prepared; hosted Actions results and final verified tag remain separate evidence |
| Automated testing/Actions | Example tests only | Financial, Room, backend and rules tests plus workflow; actual runs in VERIFICATION |
| README/report/images/release notes | README overstated unfinished features | Rewritten design report, artwork/diagram images, limits and release notes |
| Professional narrated demo | Prototype links only | Recording script; new real-phone recording/link pending |
| Signed APK/publication evidence | None supplied | Signing configuration/checklist; owner key/release/console evidence pending |
| AI-use explanation under 500 words | Absent | Editable draft in AI_USE.md; team review required |

Dependencies, source files, diagrams and checklists are not runtime or deployment evidence. Mark demonstrations complete only after capturing the revised binary in use.
