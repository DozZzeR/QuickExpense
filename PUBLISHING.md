# Publishing QuickExpense on Google Play

Checklist for the first release from a new **personal** Play Console account (state as of
September 2026). Items marked 🧑 need you; everything else is already in the repo.

## Progress log

| Date | Step |
|---|---|
| 2026-09-22 | App created in Play Console: package `dev.keslorod.quickexpense`, **Free** (can add subscriptions later; can never become paid). Accepted Developer Program Policies, Play App Signing ToS, US export laws. |
| 2026-09-22 | Android developer verification: package `dev.keslorod.quickexpense` **Registered** (3 Play-managed keys), identity taken from the developer account. Contact e-mail/phone verified. Account created May 2026 ⇒ closed test (§7) required. |
| 2026-09-22 | Upload key created (`../quickexpense-upload.jks`, RSA 4096, valid to 2054; SHA-256 `2E:29:98:1D:…:C6:59`). First signed AAB built (versionCode 49) — not uploaded yet, waiting for the real launcher icon. |

Next up: launcher icon and store graphics (§3), then upload to closed testing; start recruiting 12 testers (§7).

### Plan for later: backend + LLM + subscription

Declarations describe the *current* version — nothing to pre-declare. Before shipping the
version that adds a backend/LLM/subscription:

- Payments profile + tax info (Serbia supports merchant registration); Google Play Billing only
  for the subscription (15% fee); clear price/period/trial/cancel terms.
- Rewrite the privacy policy (it currently promises "no internet access") and redo Data safety
  (financial info, photos if receipts go to the server, account IDs; encrypted in transit; LLM
  provider as a processor with a no-training agreement).
- Accounts ⇒ in-app **and** web account deletion.
- If an AI chat/generation is a central feature: in-app "report AI response". Personal
  financial advice from the LLM may require the "Financial advice" financial-features entry.
- Remove "offline / no account" claims from the store texts.
- EU DSA trader status: monetizing ⇒ likely a trader ⇒ name, address, phone, e-mail shown
  publicly in the EU. Consider a registered business + organization account first (apps can be
  transferred between accounts, package name stays).

## 1. Account (start first — it's mostly waiting)

Mandatory for every account, whatever its creation date — older accounts just had their own
deadline. All pending tasks are listed on the Play Console **Home** page.

- 🧑 **Name and address first:** Settings → Developer account → About you. They come from the
  Google payments profile (payments.google.com) and must match the ID document *exactly*
  (same Latin spelling as the passport) — the most common rejection reason.
- 🧑 **Identity:** Home → "Verify your identity" → View details → valid government ID (passport or
  ID card, full colour photo, all edges visible). A proof of address (bank statement, utility
  bill in the same name and address) may be requested separately.
- 🧑 **Contact e-mail and phone:** one-time codes, also from Home.
- 🧑 **Android device** (new personal accounts): Home → "Verify that you have access to an Android
  mobile device" → install the *Google Play Console* app on a non-rooted Android 10+ phone, sign in
  with the developer Google account, follow the steps there.
- Usually takes a few days; the result comes by e-mail. Until then you can upload builds and edit
  the store listing, but can't publish to any track (closed testing included).
- **How to tell it's done:** no verification tasks left on Home, Settings → Developer account
  shows the identity as verified, and the confirmation e-mail has arrived. A rejection e-mail
  names the reason — fix it and resubmit.
- **Package registration (Android developer verification):** new apps are registered
  automatically when created in Play Console, so the 30 Sep 2026 deadline for older apps doesn't
  apply here. A Home banner would say otherwise.

## 2. Build and signing

- 🧑 Create the upload key once and back it up (the .jks + both passwords):

  ```bash
  keytool -genkeypair -v -keystore ../quickexpense-upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
  ```

- 🧑 Copy `keystore.properties.example` → `keystore.properties` (gitignored) and fill it in.
- Build the bundle: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
- `versionCode` = git commit count, so every new commit gives a higher one automatically.
  Bump `versionName` in `app/build.gradle.kts` yourself when releasing.
- Play App Signing is on by default for new apps: Google holds the app signing key, your
  key is only the upload key (a lost upload key can be reset through Play support).
- Debug builds are `dev.keslorod.quickexpense.debug` ("QuickExpense Dev") and install next
  to the Play build.
- Target API 36 (required for new apps since 31 Aug 2026) ✅ · 16 KB page size ✅ ·
  R8 release build smoke-tested ✅

## 3. Assets still missing

- 🧑 **Launcher icon** — the app still uses the Android Studio template icon
  (`res/drawable/ic_launcher_*`, `res/mipmap-*`). Replace it (Android Studio → New → Image Asset).
- 🧑 Store icon 512×512 PNG, feature graphic 1024×500, at least 2 phone screenshots
  (per language, if the UI text on them should match).

## 4. Store listing

Texts for en-US / ru-RU / sr are in `fastlane/metadata/android/<locale>/`
(`title.txt` ≤30, `short_description.txt` ≤80, `full_description.txt` ≤4000 — all within limits).
Paste them into Play Console → Grow → Store presence → Main store listing (add ru-RU and sr
as translations).

- Category: **Finance** (or Productivity). Tags: budget, expense tracker.
- 🧑 Public contact e-mail.

## 5. Privacy policy

`docs/privacy-policy.html` (en/ru/sr in one page).

- 🧑 Replace `{{CONTACT_EMAIL}}` (6 places) with the contact address.
- 🧑 After this branch is merged to `main`: GitHub → Settings → Pages → Deploy from branch →
  `main` / `/docs`. URL will be `https://dozzzer.github.io/QuickExpense/privacy-policy.html`.
- Update the policy **before** shipping any version that handles data differently
  (e.g. adds INTERNET, backup, sync, analytics).

## 6. App content declarations (Policy → App content)

| Declaration | Answer |
|---|---|
| Privacy policy | URL from step 5 |
| Ads | No ads |
| App access | All functionality available without special access (no login) |
| Content rating (IARC) | Utility/productivity; no violence, no user interaction/sharing between users, no gambling → expected rating: Everyone / PEGI 3 |
| Target audience | 18+ (or 13+). Don't include under-13 — that pulls in the Families policy |
| Data safety | **No data collected, no data shared.** The app has no INTERNET permission; everything stays on device. User-initiated export via the share sheet is not "collection" by the developer. Voice audio is processed by the system speech recognizer, not by the app. "Data encrypted in transit": not applicable (nothing transmitted). "Can users request deletion": uninstall/clear storage removes everything |
| Financial features | **My app doesn't provide any financial features** (there is no budgeting category; no payments, loans or trading) |
| Government / Health / News / COVID | No |

## 7. Closed testing (new personal accounts only)

Applies only to **personal accounts created after 13 Nov 2023**. Check: if the production track
is locked and Dashboard shows "Apply for production access", it applies to you; if you can
create a production release directly, skip this section.

- Create a **closed testing** track, upload the AAB, add testers (Google Group or e-mail list),
  share the opt-in link.
- 🧑 **At least 12 testers opted in continuously for 14 days.** Anyone who opts out early
  doesn't count, and re-opting in restarts their 14 days.
- Collect feedback and ship at least one update during the test — the production-access
  questionnaire asks what you changed based on testing.
- Then Dashboard → **Apply for production** (questionnaire about the test, the app and your
  readiness). Review usually ≤ 7 days.

## 8. Production

- Create a production release with the same (or newer) AAB, roll out.
- First review of a new app can take several days.

## Nice to have before or soon after launch

- Enable Android backup (`allowBackup=false` today → data is lost on phone change) or add
  import from the CSV export. Update the privacy policy if you do.
- UI to delete an expense.
- Dependency updates (lint `GradleDependency`).
