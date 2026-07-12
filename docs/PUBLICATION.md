# Publication Plan

Pre-release checklist for shipping Bookline to Google Play.

> **Not legal advice.** Consult a lawyer before commercial release. The
> notes below summarize known risks and the conventional pattern other
> Goodreads-companion apps follow.

## Status

Bookline reads Goodreads RSS feeds — a per-user URL the user generates
themselves on goodreads.com — and renders the result. No affiliation
with Goodreads or Amazon. Personal use only at present.

## Blockers (must fix before public release)

### 1. Unaffiliated disclaimer

- App About screen and Play Store listing must state, prominently:
  > Bookline is an unofficial, third-party app. Not affiliated with,
  > endorsed by, or sponsored by Goodreads or Amazon.
- **In-app part done** — About screen (top bar dropdown menu) shows the
  disclaimer, license, app version, and privacy policy + GitHub links.
- Remaining: same disclaimer in the Play Store listing description.

### 2. Privacy policy

- Required by Google Play for any app collecting/storing user data.
- **Done** — live at https://pomeranssi.fi/bookline/privacy-policy.html
  (source in the pomeranssi.fi site repo). Covers: on-device-only
  storage (Keystore-encrypted feed URL, Room cache, sync timestamp),
  no analytics/telemetry/third-party SDKs, network only to
  Goodreads/Amazon, uninstall deletes all data. Linked from the
  About screen.
- Remaining: link from the Play Store listing.

### 3. Goodreads ToS review

Reviewed https://www.goodreads.com/about/terms (rev. 2021-04-28).
The ToS says nothing about RSS feeds, APIs, or third-party apps —
neither permitting nor prohibiting. Relevant clauses and how the app
sits against them:

- **§1 license: personal, non-commercial use only.** Aligned — app is
  free, each user reads only their own opt-in feed. Keep it free;
  monetization would void this footing.
- **§1 prohibits "collection and use of any book listings,
  descriptions, reviews", "derivative use", and "data mining, robots,
  or similar data gathering and extraction tools".** Read literally,
  this covers any RSS reader that caches. Counterweight: Goodreads
  publishes per-user RSS feeds expressly for external consumption
  (implied license), the data is the user's own library, and sync is
  modest (24 h auto-sync freshness window, single-flight, manual
  pull-to-refresh). Residual risk, accepted.
- **§4: no copying/reproducing/publicly displaying content.** Room
  cache + on-device display is private use, not public display or
  redistribution. Covers are hotlinked from Goodreads/Amazon CDN, not
  re-served (see "Cover image hotlinking" below).
- **§1 framing clause: may not "frame or utilize framing techniques
  to enclose any trademark, logo, or other proprietary information
  (including images, text, page layout, or form)".** Closest literal
  conflict: the in-app WebView (`GoodreadsScreen`) renders
  goodreads.com inside the app during RSS feed discovery, and its JS
  scans the page DOM for the feed link. Scope is narrow — one
  user-initiated setup flow, functionally an in-app browser, no
  Goodreads branding used as the app's own. Residual risk, accepted;
  book links elsewhere open the external browser (`ACTION_VIEW`).
- **§1 "never use another Member's account".** Fine — the user logs
  into their own account in the discovery WebView; credentials never
  touch app code.
- **§5 eligibility: 13+.** Match in the Play content rating /target
  audience declarations (do not target under-13s).
- **Disputes are governed by the Amazon.com Conditions of Use**
  (incorporated by reference) — skim before release.
- **Enforcement reality:** license terminates on breach; Goodreads
  may cut access at will. Worst case remains account/feed blocking or
  a takedown request → comply and pull the app.

## Trademark guidelines (apply consistently)

- App name **Bookline** — OK, contains no Goodreads reference.
- App icon — **verified**: cartoon owl with books on a dark green
  (#115129) background; no resemblance to Goodreads' brown/cream
  palette or "g" mark.
- Goodreads logo asset (`ic_goodreads.xml`) — **removed**; no
  Goodreads-branded drawables remain (checked `res/`).
- Plain-text references to the name "Goodreads" are OK when
  identifying the data source ("Fetches your Goodreads RSS feed",
  "View on Goodreads"). Do not imply endorsement — current strings
  comply.
- Never use the Goodreads logo, wordmark in their typeface, or any
  derivative.
- Store listing screenshots: avoid showing the Goodreads logo even
  inside the embedded WebView screenshot — frame around it.

## Cover image hotlinking

- Book covers are loaded from Goodreads / Amazon CDN URLs via Coil.
- Hotlinking copyrighted images at scale is a soft risk. Similar
  apps do this routinely; publishers rarely act.
- Mitigations if desired:
  - Cache aggressively (Coil already does).
  - Accept the risk for v1; revisit if cease-and-desist arrives.

## Distribution

- **Google Play only**, free release. Paid app monetizing third-party
  data strengthens any future legal complaint. Requires privacy
  policy + content rating + data safety form.

## Pre-release checklist

- [x] Replace `ic_goodreads.xml` with generic icon at both usage sites.
- [x] Delete `ic_goodreads.xml`.
- [x] Add About screen (in top bar dropdown menu) with unaffiliated
      disclaimer + license + privacy policy link + app version.
- [x] Write privacy policy: https://pomeranssi.fi/bookline/privacy-policy.html
- [x] Disable backups: `android:allowBackup="false"` plus exclude-all
      `data_extraction_rules.xml` / `backup_rules.xml` (Android 12+ /
      pre-12). Keeps the privacy policy's "uninstall deletes all data"
      claim true; Keystore-encrypted prefs would not survive a restore
      anyway.
- [x] Read Goodreads ToS end-to-end, note any clauses to comply with
      (see "Goodreads ToS review" above; residual risks accepted).
- [ ] Skim Amazon.com Conditions of Use (governs disputes, per ToS).
- [x] Verify launcher icon has no Goodreads visual resemblance
      (owl on dark green — nothing like the brown/cream "g" mark).
- [x] Show third-party license attributions in the app — static list
      in the About screen (all shipped libraries are Apache-2.0), link
      to the license text. List is hand-maintained; see CLAUDE.md.
- [ ] Bump `versionName` and `versionCode` in `app/build.gradle.kts`.
- [ ] Test release build with shrinking/minification on a real device.
- [ ] Build an App Bundle (`./gradlew bundleRelease`) — Play requires
      `.aab`, not APK — and enroll in Play App Signing (mandatory);
      keep a backup of the upload keystore.
- [ ] Fill in Play Console data safety form (declare: no data
      collected or shared) and content rating questionnaire.
- [ ] Prepare Play Store listing copy with unaffiliated disclaimer
      in the description + privacy policy link.
- [ ] Store listing assets: 512 px hi-res icon, 1024×500 feature
      graphic, public support email.
- [ ] (Optional) Add a `--no-network` debug toggle for screenshots.
- [ ] Screenshot set: no Goodreads logo visible in any image.

## Bottom line

Practical risk is low — many similar Goodreads-companion apps ship
on Play Store without incident. The blockers above (disclaimer,
privacy policy) are real and easy to fix. Operating
without explicit Goodreads permission remains a residual risk; the
worst realistic outcome is a takedown request and store removal.
