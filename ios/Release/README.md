# iOS release gate

This directory is the machine-checked release contract. A successful compile is
not a completed VIZIT release.

## What blocks a release

The `iOS secure IPA` workflow rejects a TestFlight candidate unless all
automatable pre-upload conditions are true for the same 40-character commit
SHA. The release is not complete until the resulting TestFlight build also
passes the physical-device checklist:

1. all portable, native integration and UI tests pass without skips;
2. every named UI screenshot in `release-manifest.json` exists in the xcresult;
3. all eight pinned Figma boards still match their recorded SHA-256 hashes;
4. the deterministic iPhone and iPad simulator models are available;
5. shared Supabase source/configuration checks and the isolated DEV E2E pass;
6. a human compared the named screenshot artifact with every pinned board for
   that exact commit;
7. the protected `testflight-production` environment approves the job;
8. the operator enters the exact upload confirmation phrase;
9. after App Store Connect reports `VALID`, the exact TestFlight build passes
   the complete physical-device checklist without an open defect.

The workflow no longer reacts to commit-message markers and no repository-wide
boolean can approve future builds in advance.

## Review procedure

1. Run `iOS secure IPA` with `release_action=verify_only`.
2. Download `VIZIT-iOS-test-evidence` and compare every named state in
   `release-manifest.json` with the corresponding board under `Figma/`.
3. Run the workflow again on the same commit with
   `release_action=upload_testflight`. Enter that commit into the design-review
   SHA field and type `UPLOAD VIZIT TO TESTFLIGHT`.
4. Approve the protected environment only after checking the prior evidence.
5. Install that exact processed build from TestFlight and execute every item in
   the physical-device checklist. Any failure requires a new commit and build.

Repository administrators must configure `testflight-production` to require an
independent reviewer, prevent self-review and allow only the selected protected
release branch. The workflow name alone cannot create those repository-side
protection rules.

The upload creates an undistributed TestFlight candidate. It does not add beta
groups or testers. After upload, CI polls App Store Connect and succeeds only
when that exact version/build is returned as `VALID`.

## Physical-device checklist

- install and first launch;
- e-mail registration, confirmation and login;
- password recovery in the system browser;
- offline save, reconnect and two-device conflict handling;
- camera permission and a real scan of the Contact, Photo and Profile QR;
- save the scanned card to Contacts and verify its photo/data;
- photo picker, `.vcf` share and AirDrop;
- public profile link;
- permanent deletion of a dedicated test account.

## Intentional iOS constraints

The pinned Figma snapshot includes one iOS platform limitation that must not be
mistaken for forgotten UI:

- iOS cannot emulate an arbitrary Android-HCE NFC contact card. The shipping UI
  must explain that and expose QR, AirDrop, `.vcf` and HTTPS alternatives.
- the three QR modes follow board 18 exactly. High error correction, a
  four-module quiet zone, and the fixed-colour 14% VIZIT center mark are release
  invariants; no mode may silently fall back to weaker encoding.

Any future change to these constraints must update the implementation, tests,
manifest and design source in the same reviewed commit.
