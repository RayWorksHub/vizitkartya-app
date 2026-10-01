# Android 10.0.2: account and profile isolation

The 10.0.1 UI joined a default-profile catalog entry with an independently loaded, account-wide Room snapshot. Swipes changed the server default, reordered the catalog and deleted/reloaded the same local row. Independent responses could therefore combine different profile IDs, names, avatars and QR URLs or briefly trigger onboarding. The menu also displayed the active profile's public contact email as the account email.

## Final behavior

- Auth supplies the account ID, login email and account display name. Contact email belongs exclusively to a profile.
- A single account state contains complete owned profile records and an explicit local active profile ID. Catalog order is stable by creation time and ID.
- Swipes change the local selection. They never change `is_default` or the authenticated user.
- Room v4 stores content, appearance, sync status and queued mutations under `(ownerId, profileId)`. Selection is persisted per account.
- Background writes target `/api/profiles/:profileId`, validate owner and returned ID, and never use the default-profile write endpoint. Late confirmations retain/rebase newer edits for the same profile.
- Initial onboarding requires a successful empty catalog result. Additional profiles use the same authenticated owner and a new server profile ID. Moving onto the add-profile card does not open the wizard.
- Account lifetime tokens reject responses from a previous login. Logout resets profiles, drafts, wizard and navigation.
- QR, NFC/contact sharing, appearance, deletion and analytics use the selected profile's data or explicit ID.
- ZIP screen layout, animations and UI elements are retained. Android remains independently versioned; this change does not edit iOS.

## Upgrade

Migration 3→4 adds the new scoped tables. Existing legacy snapshots and queued edits are retained intact, but ambiguous legacy mutations lacking a profile ID are never sent by the production worker. The cloud catalog supplies the new scoped cache. No live SQL rows are changed or removed by this release.

## Verification scope

Unit tests exercise local selection, explicit write targets, two-account isolation, late account responses, cold start/failed load onboarding gates and persisted selection. Android instrumentation uses actual Room/SQLite and the actual Compose pager/UI with controlled remote responses to reproduce delayed writes, repeated swipes, profile creation/editing, account changes, app restart and migration.

GitHub Actions runs the unit/lint/build checks and Android 35 emulator instrumentation before merge. These tests do not claim a physical-device check or a live Google OAuth login.
