# iOS release gate

This directory is the machine-checked release contract. A successful compile is
not a completed VIZIT release.

## What blocks a release

The `iOS secure IPA` workflow rejects a TestFlight upload unless all of these
conditions are true for the same 40-character commit SHA:

1. all portable, native integration and UI tests pass without skips;
2. every named UI screenshot in `release-manifest.json` exists in the xcresult;
3. all eight pinned Figma boards still match their recorded SHA-256 hashes;
4. the deterministic iPhone and iPad simulator models are available;
5. shared Supabase source/configuration checks and the isolated DEV E2E pass;
6. the complete physical-device checklist was recorded for the exact commit;
7. a human compared the named screenshot artifact with every pinned board for
   that exact commit;
8. the protected `testflight-production` environment approves the job;
9. the operator enters the exact upload confirmation phrase.

The workflow no longer reacts to commit-message markers and no repository-wide
boolean can approve future builds in advance.

## Review procedure

1. Run `iOS secure IPA` with `release_action=verify_only`.
2. Download `VIZIT-iOS-test-evidence` and compare every named state in
   `release-manifest.json` with the corresponding board under `Figma/`.
3. Test the unsigned source commit on a real iPhone using the checklist below.
4. Run the workflow again on the same commit with
   `release_action=upload_testflight`. Enter that commit into both approval SHA
   fields and type `UPLOAD VIZIT TO TESTFLIGHT`.
5. Approve the protected environment only after checking the prior evidence.

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
- camera permission and a real scan of the photo contact QR;
- save the scanned card to Contacts and verify its photo/data;
- photo picker, `.vcf` share and AirDrop;
- public profile link;
- permanent deletion of a dedicated test account.

## Intentional iOS constraints

The pinned Figma snapshot predates two explicit 8.1 product/platform decisions.
They are recorded in the manifest so they cannot be mistaken for forgotten UI:

- iOS cannot emulate an arbitrary Android-HCE NFC contact card. The shipping UI
  must explain that and expose QR, AirDrop, `.vcf` and HTTPS alternatives.
- the 8.1 primary QR is a photo-containing offline vCard. It fails closed if the
  photo cannot be included; the older three-mode QR board is not the release
  contract for this behavior.

Any future change to either decision must update the implementation, tests,
manifest and design source in the same reviewed commit.
