#!/usr/bin/env python3
"""Validate or apply the iOS marketing version without touching Android."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "config" / "ios-version.properties"
IOS_PROJECT_FILE = ROOT / "ios" / "VIZIT.xcodeproj" / "project.pbxproj"

SEMVER_RE = re.compile(r"^\d+\.\d+\.\d+$")
IOS_MARKETING_VERSION_RE = re.compile(r'(\bMARKETING_VERSION\s*=\s*")[^"]+(")')


def load_version() -> str:
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
        raise SystemExit("iOS versionName must use MAJOR.MINOR.PATCH numeric SemVer")
    return version


def expected_text(version: str) -> str:
    project = IOS_PROJECT_FILE.read_text(encoding="utf-8")
    return IOS_MARKETING_VERSION_RE.sub(rf"\g<1>{version}\g<2>", project)


def check(version: str) -> int:
    if IOS_PROJECT_FILE.read_text(encoding="utf-8") != expected_text(version):
        print(f"iOS version drift: expected MARKETING_VERSION={version}")
        print("Run: python3 scripts/sync-ios-version.py --apply")
        return 1
    print(f"iOS version sync: PASS — {version}")
    return 0


def apply(version: str) -> int:
    IOS_PROJECT_FILE.write_text(expected_text(version), encoding="utf-8")
    print(f"Synced iOS MARKETING_VERSION={version}. Android was not changed.")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--print-version", action="store_true")
    args = parser.parse_args()
    version = load_version()
    if args.print_version:
        print(version)
        return 0
    if args.check:
        return check(version)
    return apply(version)


if __name__ == "__main__":
    raise SystemExit(main())
