#!/usr/bin/env python3
"""Read back an uploaded TestFlight build with the App Store Connect API.

The script deliberately has no third-party dependencies. The private key is
only passed to OpenSSL for ES256 signing and is never printed or copied into
the resulting JSON evidence.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


API_ROOT = "https://api.appstoreconnect.apple.com/v1"
TERMINAL_FAILURE_STATES = {"FAILED", "INVALID"}


def required_environment(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def base64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def compact_json(value: Any) -> bytes:
    return json.dumps(value, separators=(",", ":"), sort_keys=True).encode("utf-8")


def der_length(data: bytes, offset: int) -> tuple[int, int]:
    if offset >= len(data):
        raise RuntimeError("Malformed ECDSA signature")
    first = data[offset]
    offset += 1
    if first < 0x80:
        return first, offset
    count = first & 0x7F
    if count == 0 or count > 4 or offset + count > len(data):
        raise RuntimeError("Malformed ECDSA signature length")
    return int.from_bytes(data[offset : offset + count], "big"), offset + count


def der_es256_to_raw(signature: bytes) -> bytes:
    offset = 0
    if not signature or signature[offset] != 0x30:
        raise RuntimeError("OpenSSL returned a non-DER ECDSA signature")
    length, offset = der_length(signature, offset + 1)
    if offset + length != len(signature):
        raise RuntimeError("Malformed ECDSA signature sequence")

    integers: list[bytes] = []
    for _ in range(2):
        if offset >= len(signature) or signature[offset] != 0x02:
            raise RuntimeError("Malformed ECDSA signature integer")
        size, offset = der_length(signature, offset + 1)
        value = signature[offset : offset + size]
        offset += size
        value = value.lstrip(b"\x00")
        if len(value) > 32:
            raise RuntimeError("ECDSA signature integer exceeds P-256 width")
        integers.append(value.rjust(32, b"\x00"))
    if offset != len(signature):
        raise RuntimeError("Unexpected trailing ECDSA signature data")
    return integers[0] + integers[1]


def make_token(key_id: str, issuer_id: str, key_path: Path) -> str:
    if not key_path.is_file():
        raise RuntimeError(f"App Store Connect key does not exist: {key_path}")
    now = int(time.time())
    header = base64url(compact_json({"alg": "ES256", "kid": key_id, "typ": "JWT"}))
    payload = base64url(
        compact_json(
            {
                "iss": issuer_id,
                "iat": now - 20,
                "exp": now + 1_100,
                "aud": "appstoreconnect-v1",
            }
        )
    )
    signing_input = f"{header}.{payload}".encode("ascii")
    with tempfile.NamedTemporaryFile() as message:
        message.write(signing_input)
        message.flush()
        result = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", str(key_path), message.name],
            check=False,
            capture_output=True,
        )
    if result.returncode != 0:
        detail = result.stderr.decode("utf-8", errors="replace").strip()
        raise RuntimeError(f"OpenSSL could not sign the App Store Connect token: {detail}")
    return f"{header}.{payload}.{base64url(der_es256_to_raw(result.stdout))}"


def api_get(path: str, token: str, query: dict[str, str] | None = None) -> dict[str, Any]:
    url = f"{API_ROOT}{path}"
    if query:
        url = f"{url}?{urllib.parse.urlencode(query)}"
    request = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raw = error.read(16_384).decode("utf-8", errors="replace")
        try:
            body = json.loads(raw)
            details = "; ".join(
                str(item.get("detail") or item.get("title") or "unknown error")
                for item in body.get("errors", [])
            )
        except json.JSONDecodeError:
            details = raw[:500]
        raise RuntimeError(f"App Store Connect API HTTP {error.code}: {details}") from error


def one_resource(payload: dict[str, Any], label: str) -> dict[str, Any]:
    resources = payload.get("data")
    if not isinstance(resources, list) or len(resources) != 1:
        count = len(resources) if isinstance(resources, list) else 0
        raise RuntimeError(f"Expected exactly one {label}, found {count}")
    return resources[0]


def find_build(
    token: str,
    app_id: str,
    build_number: str,
    marketing_version: str | None,
) -> dict[str, Any] | None:
    payload = api_get(
        "/builds",
        token,
        {
            "filter[app]": app_id,
            "filter[version]": build_number,
            "include": "preReleaseVersion",
            "limit": "10",
        },
    )
    builds = payload.get("data")
    if not isinstance(builds, list) or not builds:
        return None

    included = {
        item.get("id"): item
        for item in payload.get("included", [])
        if item.get("type") == "preReleaseVersions"
    }
    matches: list[dict[str, Any]] = []
    for build in builds:
        relationship = build.get("relationships", {}).get("preReleaseVersion", {}).get("data") or {}
        prerelease = included.get(relationship.get("id"), {})
        version = prerelease.get("attributes", {}).get("version")
        if marketing_version is None or version == marketing_version:
            matches.append({"build": build, "marketingVersion": version})
    if len(matches) > 1:
        raise RuntimeError(
            f"App Store Connect returned multiple builds for build number {build_number}; "
            "supply --marketing-version"
        )
    return matches[0] if matches else None


def beta_detail(token: str, build_id: str) -> dict[str, Any] | None:
    try:
        payload = api_get(
            "/buildBetaDetails",
            token,
            {"filter[build]": build_id, "limit": "1"},
        )
    except RuntimeError as error:
        return {"readbackError": str(error)}
    data = payload.get("data")
    if not isinstance(data, list) or not data:
        return None
    return data[0].get("attributes", {})


def public_report(
    app: dict[str, Any],
    match: dict[str, Any],
    detail: dict[str, Any] | None,
) -> dict[str, Any]:
    build = match["build"]
    attributes = build.get("attributes", {})
    return {
        "checkedAt": datetime.now(timezone.utc).isoformat(),
        "app": {
            "id": app.get("id"),
            "bundleId": app.get("attributes", {}).get("bundleId"),
            "name": app.get("attributes", {}).get("name"),
        },
        "build": {
            "id": build.get("id"),
            "buildNumber": attributes.get("version"),
            "marketingVersion": match.get("marketingVersion"),
            "processingState": attributes.get("processingState"),
            "uploadedDate": attributes.get("uploadedDate"),
            "expirationDate": attributes.get("expirationDate"),
            "expired": attributes.get("expired"),
            "minOsVersion": attributes.get("minOsVersion"),
        },
        "testFlight": detail,
    }


def write_report(report: dict[str, Any], output: Path | None) -> None:
    text = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text, encoding="utf-8")
    sys.stdout.write(text)


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--bundle-id")
    parser.add_argument("--build-number")
    parser.add_argument("--marketing-version")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--wait-for-valid", action="store_true")
    parser.add_argument("--timeout-seconds", type=int, default=1_800)
    parser.add_argument("--poll-seconds", type=int, default=30)
    return parser.parse_args()


def self_test() -> None:
    with tempfile.TemporaryDirectory() as directory:
        key_path = Path(directory) / "self-test.p8"
        result = subprocess.run(
            [
                "openssl",
                "ecparam",
                "-name",
                "prime256v1",
                "-genkey",
                "-noout",
                "-out",
                str(key_path),
            ],
            check=False,
            capture_output=True,
        )
        if result.returncode != 0:
            raise RuntimeError("OpenSSL could not create the temporary self-test key")
        token = make_token(
            "SELFTEST01",
            "00000000-0000-0000-0000-000000000000",
            key_path,
        )
        parts = token.split(".")
        if len(parts) != 3:
            raise RuntimeError("JWT self-test produced an invalid token envelope")
        padding = "=" * (-len(parts[2]) % 4)
        if len(base64.urlsafe_b64decode(parts[2] + padding)) != 64:
            raise RuntimeError("JWT self-test produced a non-raw ES256 signature")


def main() -> int:
    arguments = parse_arguments()
    if arguments.self_test:
        self_test()
        print("App Store Connect JWT signing self-test: PASS")
        return 0
    if not arguments.bundle_id or not arguments.build_number:
        raise RuntimeError("--bundle-id and --build-number are required")
    key_id = required_environment("APP_STORE_CONNECT_API_KEY_ID")
    issuer_id = required_environment("APP_STORE_CONNECT_API_ISSUER_ID")
    key_path = Path(required_environment("VIZIT_ASC_KEY_PATH"))
    token = make_token(key_id, issuer_id, key_path)

    app = one_resource(
        api_get(
            "/apps",
            token,
            {"filter[bundleId]": arguments.bundle_id, "limit": "1"},
        ),
        f"app with bundle id {arguments.bundle_id}",
    )

    deadline = time.monotonic() + max(arguments.timeout_seconds, 0)
    while True:
        match = find_build(
            token,
            app["id"],
            arguments.build_number,
            arguments.marketing_version,
        )
        if match is not None:
            state = match["build"].get("attributes", {}).get("processingState")
            report = public_report(app, match, beta_detail(token, match["build"]["id"]))
            write_report(report, arguments.output)
            if not arguments.wait_for_valid:
                return 0
            if state == "VALID":
                return 0
            if state in TERMINAL_FAILURE_STATES:
                raise RuntimeError(f"TestFlight build entered terminal state {state}")
        elif not arguments.wait_for_valid:
            raise RuntimeError(
                f"Build {arguments.marketing_version or '*'} ({arguments.build_number}) was not found"
            )

        if time.monotonic() >= deadline:
            raise RuntimeError(
                f"Timed out waiting for TestFlight build {arguments.marketing_version or '*'} "
                f"({arguments.build_number}) to become VALID"
            )
        time.sleep(max(arguments.poll_seconds, 5))
        token = make_token(key_id, issuer_id, key_path)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (RuntimeError, OSError, subprocess.SubprocessError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
