# Native ZIP source parity — 2026-10-03

Authoritative sources are the user-uploaded native archives, not the earlier HTML.
- VizitTeljesIOS(1).zip: SHA-256 af2b2c86c1792b39c57815209538cb7823a10a943d32cd11ce6f321e224f5288
- VizitTeljes(4).zip: SHA-256 de0bb755e8e5e8bf6277af564a150d1416bcbb9a16a86aa685c6faa2ca010819

## Confirmed discrepancy
The production app copied the navy intro and grouped progress but rendered the old
individual-step form and large card preview underneath. The archives instead provide
horizontal, independently scrolling colored data blocks with their own actions.

## Repaired scope
- Original personal/company/online/completion blocks, fields, images, transitions,
  optional-step behavior, keyboard action and exit confirmation on both platforms.
- Private flow: 3 blocks. Business flow: 4 blocks.
- Registration completion becomes a separate route; signup confirmation requires an
  explicit login. A persisted latch prevents a temporary signup session from opening
  the wizard on app restart.

## Deliberate production adapters
- Native UI namespaced; no prototype account/users/auth/cloud/timer-based publication.
- Real image selection only; no sample avatar/logo actions.
- Existing owner-isolated storage, sync, NFC, deletion and backend retained.
- Client slug format is not presented as authoritative global availability.
- iOS 17+ original scroll pager; iOS 16 fallback uses identical block contents.
- Local-first save returns to the real sync status, not a fake publication success.

## Evidence and limitations
Source parse alone is not an iOS build or a device E2E. Standard Android/iOS CI must
pass before merge/release. Native UI tests retain screenshot evidence. Physical email
callback, NFC and end-to-end cloud behavior still require device verification.
This change does not claim every Home/Settings/Analytics screen matches the archives.
