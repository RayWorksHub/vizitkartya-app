#!/usr/bin/env python3
"""Keep Android and iOS public versions aligned with config/version.properties."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "config" / "version.properties"
ANDROID_FILE = ROOT / "app" / "build.gradle.kts"
IOS_PROJECT_FILE = ROOT / "ios" / "VIZIT.xcodeproj" / "project.pbxproj"

SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
ANDROID_VERSION_NAME_RE = re.compile(r'(\bversionName\s*=\s*")[^"]+(")')
ANDROID_VERSION_CODE_RE = re.compile(r"(\bversionCode\s*=\s*)\d+")
IOS_MARKETING_VERSION_RE = re.compile(r'(\bMARKETING_VERSION\s*=\s*")[^"]+(")')


def load_version() -> tuple[str, int]:
    props: dict[str, str] = {}
    for raw_line in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if not sep:
            raise SystemExit(f"Invalid line in {VERSION_FILE}: {raw_line!r}")
        props[key.strip()] = value.strip()

    version = props.get("versionName", "")
    match = SEMVER_RE.fullmatch(version)
    if not match:
        raise SystemExit("versionName must use MAJOR.MINOR.PATCH numeric SemVer")

    major, minor, patch = map(int, match.groups())
    if minor > 99:
        raise SystemExit("MINOR must be <= 99 for the Android versionCode mapping")
    if patch > 9999:
        raise SystemExit("PATCH must be <= 9999 for the Android versionCode mapping")

    # Preserve the existing VIZIT encoding: 7.3.4 -> 7,030,004.
    android_version_code = major * 1_000_000 + minor * 10_000 + patch
    if not 1 <= android_version_code <= 2_100_000_000:
        raise SystemExit("Derived Android versionCode is outside the supported range")

    return version, android_version_code


def expected_texts(version: str, android_version_code: int) -> tuple[str, str]:
    android = ANDROID_FILE.read_text(encoding="utf-8")
    ios = IOS_PROJECT_FILE.read_text(encoding="utf-8")

    android = ANDROID_VERSION_NAME_RE.sub(rf"\g<1>{version}\g<2>", android)
    android = ANDROID_VERSION_CODE_RE.sub(rf"\g<1>{android_version_code}", android)
    ios = IOS_MARKETING_VERSION_RE.sub(rf"\g<1>{version}\g<2>", ios)
    return android, ios


def check(version: str, android_version_code: int) -> int:
    expected_android, expected_ios = expected_texts(version, android_version_code)
    current_android = ANDROID_FILE.read_text(encoding="utf-8")
    current_ios = IOS_PROJECT_FILE.read_text(encoding="utf-8")

    problems: list[str] = []
    if current_android != expected_android:
        problems.append(
            f"Android version drift: expected versionName={version}, "
            f"versionCode={android_version_code}"
        )
    if current_ios != expected_ios:
        problems.append(f"iOS version drift: expected MARKETING_VERSION={version}")

    if problems:
        for problem in problems:
            print(problem)
        print("Run: python3 scripts/sync-version.py --apply")
        return 1

    print(
        f"VIZIT version sync: PASS — public version {version}, "
        f"Android versionCode {android_version_code}"
    )
    return 0


def apply(version: str, android_version_code: int) -> int:
    android, ios = expected_texts(version, android_version_code)
    ANDROID_FILE.write_text(android, encoding="utf-8")
    IOS_PROJECT_FILE.write_text(ios, encoding="utf-8")
    print(
        f"Synced VIZIT {version}: Android versionCode={android_version_code}; "
        "iOS MARKETING_VERSION aligned. "
        "iOS CURRENT_PROJECT_VERSION remains an independent build number."
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true", help="fail when platform versions drift")
    mode.add_argument("--apply", action="store_true", help="write the shared version into platform files")
    mode.add_argument("--print-version", action="store_true", help="print the shared public version")
    mode.add_argument("--print-android-code", action="store_true", help="print the derived Android versionCode")
    args = parser.parse_args()

    version, android_version_code = load_version()
    if args.print_version:
        print(version)
        return 0
    if args.print_android_code:
        print(android_version_code)
        return 0
    if args.check:
        return check(version, android_version_code)
    return apply(version, android_version_code)


if __name__ == "__main__":
    raise SystemExit(main())
