#!/usr/bin/env python3
"""Validate or apply the Android release version without touching iOS."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "config" / "android-version.properties"
ANDROID_FILE = ROOT / "app" / "build.gradle.kts"

SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
ANDROID_VERSION_NAME_RE = re.compile(r'(\bversionName\s*=\s*")[^"]+(")')
ANDROID_VERSION_CODE_RE = re.compile(r"(\bversionCode\s*=\s*)\d+")


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
    if not SEMVER_RE.fullmatch(version):
        raise SystemExit("Android versionName must use MAJOR.MINOR.PATCH numeric SemVer")

    raw_code = props.get("versionCode", "")
    if not raw_code.isdigit():
        raise SystemExit("Android versionCode must be a positive integer")
    version_code = int(raw_code)
    if not 1 <= version_code <= 2_100_000_000:
        raise SystemExit("Android versionCode is outside the supported range")
    return version, version_code


def expected_text(version: str, version_code: int) -> str:
    android = ANDROID_FILE.read_text(encoding="utf-8")
    android = ANDROID_VERSION_NAME_RE.sub(rf"\g<1>{version}\g<2>", android)
    return ANDROID_VERSION_CODE_RE.sub(rf"\g<1>{version_code}", android)


def check(version: str, version_code: int) -> int:
    if ANDROID_FILE.read_text(encoding="utf-8") != expected_text(version, version_code):
        print(f"Android version drift: expected versionName={version}, versionCode={version_code}")
        print("Run: python3 scripts/sync-android-version.py --apply")
        return 1
    print(f"Android version sync: PASS — {version} ({version_code})")
    return 0


def apply(version: str, version_code: int) -> int:
    ANDROID_FILE.write_text(expected_text(version, version_code), encoding="utf-8")
    print(f"Synced Android {version}: versionCode={version_code}. iOS was not changed.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--print-version", action="store_true")
    mode.add_argument("--print-android-code", action="store_true")
    args = parser.parse_args()

    version, version_code = load_version()
    if args.print_version:
        print(version)
        return 0
    if args.print_android_code:
        print(version_code)
        return 0
    if args.check:
        return check(version, version_code)
    return apply(version, version_code)


if __name__ == "__main__":
    raise SystemExit(main())
