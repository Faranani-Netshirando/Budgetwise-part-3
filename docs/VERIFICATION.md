# Verification — 5 November 2026

Current app: **1.1.1, version code 3**, using XML layouts, Android Views and View Binding. Compose source, compiler plugins and dependencies have been removed. The final resolved runtime graph has no Compose artifacts, and inspection of all 13 APK DEX files found **zero Compose classes among 19,864 defined classes**. See evidence/COMPOSE_REMOVAL.txt.

Actual results only; prepared workflows are not counted as passed runs.

- Backend notification helper: **3 tests passed** using Node's test runner. No real message sent.
- Android build: **BUILD SUCCESSFUL** using JDK 17, Gradle 8.11.1 and SDK 36. Commands: testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest.
- Android JVM tests after the XML/Views migration: **12 passed**, zero failures/errors (6 financial rules, 4 Firestore codec/legacy records, 2 native UI theme tests). Both UI tests inflate all 11 XML layouts and draw the native chart on Android API 35 through Robolectric, in light and dark modes.
- Android lint after migration: **0 errors**, 66 warnings. Remaining warnings include dependency-update suggestions, text formatting, icon location, allocations in chart drawing and unused resources. The inherited GoogleSignIn flow also produces deprecation warnings; live SSO still needs owner testing.
- Debug APK and Android test APK: built. The app APK's signature was verified with apksigner; see evidence/DEBUG_APK_VERIFICATION.txt. This is debug signing, not a production release signature.
- Device instrumentation: 3 Room tests and 2 native navigation/form tests compile into the test APK; **not executed**, because no phone/device was supplied. Robolectric layout tests do not replace physical-phone interaction testing.
- Firestore/Storage emulator: **3 tests passed**, covering owner access, cross-account denial, invalid writes, tombstones and receipt content/access rules. The demo project was used; no production data was accessed.
- Backend dependency install/module load: passed. Production dependency audit after nonbreaking fixes: **0 reported vulnerabilities**. The separate development emulator/CLI toolchain still reports 18 transitive advisories (13 moderate, 5 high); it is not packaged with Android or the deployed function. No forced dependency downgrade was applied.
- Live Firebase auth/SSO, cloud sync, receipt upload and FCM: not verified in an owner's account.
- Physical phone/narrated video, owner-signed release and Play Console evidence: not supplied.
- GitHub: the owner subsequently requested upload to Faranani-Netshirando/Budgetwise-part-3. This record describes local checks; hosted Actions results and the final PoE tag must be verified separately.

The first Android build ran out of disk space while unpacking dependencies. Task-created temporary downloads/obsolete caches were cleared before retrying with API 36.

The first full lint report also found an obsolete empty values-ven folder and an unescaped local SDK drive path. Both were corrected; the final build and lint passed. No Firebase deployment, physical-phone run, recording or Play Console upload is claimed.

The first XML/Views build encountered a Windows Gradle transform-cache rename failure. The single affected task-local cache entry was cleared; the subsequent full build and final incremental verification both passed.
