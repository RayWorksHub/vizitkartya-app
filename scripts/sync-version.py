#!/usr/bin/env python3
"""Validate or apply Android and iOS versions independently."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID_VERSION_FILE = ROOT / "config" / "android-version.properties"
IOS_VERSION_FILE = ROOT / "config" / "ios-version.properties"
ANDROID_BUILD_FILE = ROOT / "app" / "build.gradle.kts"
IOS_PROJECT_FILE = ROOT / "ios" / "VIZIT.xcodeproj" / "project.pbxproj"

SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
ANDROID_VERSION_NAME_RE = re.compile(r'(\bversionName\s*=\s*")[^"]+(")')
ANDROID_VERSION_CODE_RE = re.compile(r"(\bversionCode\s*=\s*)\d+")
IOS_MARKETING_VERSION_RE = re.compile(r'(\bMARKETING_VERSION\s*=\s*")[^"]+(")')


def load_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if not sep:
            raise SystemExit(f"Invalid line in {path}: {raw_line!r}")
        props[key.strip()] = value.strip()
    return props


def require_semver(value: str, label: str) -> str:
    if not SEMVER_RE.fullmatch(value):
        raise SystemExit(f"{label} must use MAJOR.MINOR.PATCH numeric SemVer")
    return value


def load_android_version() -> tuple[str, int]:
    props = load_properties(ANDROID_VERSION_FILE)
    version = require_semver(props.get("versionName", ""), "Android versionName")
    try:
        version_code = int(props.get("versionCode", ""))
    except ValueError as error:
        raise SystemExit("Android versionCode must be an integer") from error
    if not 1 <= version_code <= 2_100_000_000:
        raise SystemExit("Android versionCode is outside the supported range")
    return version, version_code


def load_ios_version() -> str:
    props = load_properties(IOS_VERSION_FILE)
    return require_semver(props.get("marketingVersion", ""), "iOS marketingVersion")


def expected_android_text(version: str, version_code: int) -> str:
    text = ANDROID_BUILD_FILE.read_text(encoding="utf-8")
    text = ANDROID_VERSION_NAME_RE.sub(rf"\g<1>{version}\g<2>", text)
    return ANDROID_VERSION_CODE_RE.sub(rf"\g<1>{version_code}", text)


def expected_ios_text(version: str) -> str:
    text = IOS_PROJECT_FILE.read_text(encoding="utf-8")
    return IOS_MARKETING_VERSION_RE.sub(rf"\g<1>{version}\g<2>", text)


def check_android() -> int:
    version, version_code = load_android_version()
    if ANDROID_BUILD_FILE.read_text(encoding="utf-8") != expected_android_text(version, version_code):
        print(f"Android version drift: expected versionName={version}, versionCode={version_code}")
        print("Run: python3 scripts/sync-version.py --apply-android")
        return 1
    print(f"Android version: PASS — {version} ({version_code})")
    return 0


def check_ios() -> int:
    version = load_ios_version()
    if IOS_PROJECT_FILE.read_text(encoding="utf-8") != expected_ios_text(version):
        print(f"iOS version drift: expected MARKETING_VERSION={version}")
        print("Run: python3 scripts/sync-version.py --apply-ios")
        return 1
    print(f"iOS version: PASS — {version}")
    return 0


def apply_android() -> int:
    version, version_code = load_android_version()
    ANDROID_BUILD_FILE.write_text(expected_android_text(version, version_code), encoding="utf-8")
    print(f"Applied Android {version} ({version_code}); iOS was not changed.")
    return 0


def apply_ios() -> int:
    version = load_ios_version()
    IOS_PROJECT_FILE.write_text(expected_ios_text(version), encoding="utf-8")
    print(f"Applied iOS {version}; Android was not changed.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check-android", action="store_true")
    mode.add_argument("--apply-android", action="store_true")
    mode.add_argument("--print-android-version", action="store_true")
    mode.add_argument("--print-android-code", action="store_true")
    mode.add_argument("--check-ios", action="store_true")
    mode.add_argument("--apply-ios", action="store_true")
    mode.add_argument("--print-ios-version", action="store_true")
    args = parser.parse_args()

    if args.check_android:
        return check_android()
    if args.apply_android:
        return apply_android()
    if args.print_android_version:
        print(load_android_version()[0])
        return 0
    if args.print_android_code:
        print(load_android_version()[1])
        return 0
    if args.check_ios:
        return check_ios()
    if args.apply_ios:
        return apply_ios()
    print(load_ios_version())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
