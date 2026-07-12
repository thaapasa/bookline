# Publication Plan

Pre-release checklist for shipping Bookline to a public Android store
(Play Store, F-Droid, etc).

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
- Settings screen is the natural home for an About section.

### 2. Privacy policy

- Required by Google Play for any app collecting/storing user data.
- **Done** — live at https://pomeranssi.fi/bookline/privacy-policy.html
  (source in the pomeranssi.fi site repo). Covers: on-device-only
  storage (Keystore-encrypted feed URL, Room cache, sync timestamp),
  no analytics/telemetry/third-party SDKs, network only to
  Goodreads/Amazon, uninstall deletes all data.
- Remaining: link from Play Store listing and from the About screen.

### 3. Read Goodreads ToS

- https://www.goodreads.com/about/terms
- Look for clauses on:
  - Automated access / scraping
  - RSS feed permissible use
  - Derivative works / display of content
  - Trademark and branding
- The RSS feed is per-user opt-in (each user has a private `key=`
  URL), which strengthens the "user accessing their own data"
  framing — but Amazon reserves the right to disagree. Worst case
  is a takedown request: comply and pull the app.

## Trademark guidelines (apply consistently)

- App name **Bookline** — keep, contains no Goodreads reference.
- App icon — current launcher icon is neutral. Verify no resemblance
  to Goodreads' brown/cream palette or "G" mark.
- Never use the Goodreads logo, wordmark in their typeface, or any
  derivative.
- Plain-text references to the name "Goodreads" are OK when
  identifying the data source ("Fetches your Goodreads RSS feed",
  "View on Goodreads"). Do not imply endorsement.
- Store listing screenshots: avoid showing the Goodreads logo even
  inside the embedded WebView screenshot — frame around it.

## Cover image hotlinking

- Book covers are loaded from Goodreads / Amazon CDN URLs via Coil.
- Hotlinking copyrighted images at scale is a soft risk. Similar
  apps do this routinely; publishers rarely act.
- Mitigations if desired:
  - Cache aggressively (Coil already does).
  - Accept the risk for v1; revisit if cease-and-desist arrives.

## Distribution choices

- **Free release recommended.** Paid app monetizing third-party data
  strengthens any future legal complaint.
- **Play Store** — main target, requires privacy policy + content
  rating + data safety form.
- **F-Droid** — viable alternative, more permissive but requires
  fully open-source build (already MIT-licensed).
- **GitHub Releases (APK sideload)** — simplest, no review process,
  but limited audience.

## Pre-release checklist

- [x] Replace `ic_goodreads.xml` with generic icon at both usage sites.
- [x] Delete `ic_goodreads.xml`.
- [ ] Add About screen (linked from Settings) with unaffiliated
      disclaimer + license + privacy policy link.
- [x] Write privacy policy: https://pomeranssi.fi/bookline/privacy-policy.html
- [ ] Read Goodreads ToS end-to-end, note any clauses to comply with.
- [ ] Verify launcher icon has no Goodreads visual resemblance.
- [ ] Bump `versionName` and `versionCode` in `app/build.gradle.kts`.
- [ ] Test release build with shrinking/minification on a real device.
- [ ] Prepare Play Store listing copy with unaffiliated disclaimer
      in the description.
- [ ] (Optional) Add a `--no-network` debug toggle for screenshots.
- [ ] Screenshot set: no Goodreads logo visible in any image.

## Bottom line

Practical risk is low — many similar Goodreads-companion apps ship
on Play Store without incident. The blockers above (disclaimer,
privacy policy) are real and easy to fix. Operating
without explicit Goodreads permission remains a residual risk; the
worst realistic outcome is a takedown request and store removal.
