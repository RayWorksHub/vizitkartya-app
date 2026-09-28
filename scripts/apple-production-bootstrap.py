#!/usr/bin/env python3
"""Bootstrap the production Apple Bundle ID and App Store provisioning profile."""

from __future__ import annotations

import argparse
import base64
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

import jwt

API = "https://api.appstoreconnect.apple.com/v1"


def normalize_serial(value: str) -> str:
    value = value.strip().upper().replace(":", "")
    value = value.removeprefix("0X")
    return value.lstrip("0") or "0"


def token() -> str:
    key_id = os.environ["APP_STORE_CONNECT_API_KEY_ID"].strip()
    issuer_id = os.environ["APP_STORE_CONNECT_API_ISSUER_ID"].strip()
    key_b64 = os.environ["APP_STORE_CONNECT_API_KEY_BASE64"].strip()
    key = base64.b64decode(key_b64).decode("utf-8")
    now = int(dt.datetime.now(dt.timezone.utc).timestamp())
    return jwt.encode(
        {"iss": issuer_id, "iat": now, "exp": now + 600, "aud": "appstoreconnect-v1"},
        key,
        algorithm="ES256",
        headers={"kid": key_id, "typ": "JWT"},
    )


AUTH_TOKEN = None


def request(method: str, path: str, *, query: dict[str, str] | None = None, payload=None):
    global AUTH_TOKEN
    if AUTH_TOKEN is None:
        AUTH_TOKEN = token()
    url = API + path
    if query:
        url += "?" + urllib.parse.urlencode(query)
    headers = {
        "Authorization": "Bearer " + AUTH_TOKEN,
        "Accept": "application/json",
    }
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, method=method, headers=headers, data=data)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = response.read()
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as exc:
        body = exc.read(20000)
        detail = body.decode("utf-8", errors="replace")
        try:
            parsed = json.loads(detail)
            errors = parsed.get("errors") or []
            summary = "; ".join(
                str(item.get("detail") or item.get("title") or item.get("code") or "")
                for item in errors
            ).strip("; ")
            if summary:
                detail = summary
        except Exception:
            pass
        raise RuntimeError(f"Apple API {method} {path} failed: HTTP {exc.code}: {detail[:1500]}") from None


def get_bundle(identifier: str):
    response = request(
        "GET",
        "/bundleIds",
        query={"filter[identifier]": identifier, "limit": "10"},
    )
    return next((item for item in response.get("data", []) if item.get("attributes", {}).get("identifier") == identifier), None)


def ensure_bundle(identifier: str, name: str):
    existing = get_bundle(identifier)
    if existing:
        print(f"Bundle ID already registered: {identifier}")
        return existing
    payload = {
        "data": {
            "type": "bundleIds",
            "attributes": {
                "name": name,
                "identifier": identifier,
                "platform": "IOS",
            },
        }
    }
    try:
        created = request("POST", "/bundleIds", payload=payload)["data"]
    except RuntimeError:
        # Handle a concurrent/idempotent registration by checking once more.
        existing = get_bundle(identifier)
        if existing:
            return existing
        raise
    print(f"Registered production Bundle ID: {identifier}")
    return created


def find_distribution_certificate(serial: str):
    expected = normalize_serial(serial)
    response = request(
        "GET",
        "/certificates",
        query={"limit": "200", "fields[certificates]": "displayName,serialNumber,certificateType,expirationDate"},
    )
    candidates = []
    for item in response.get("data", []):
        attrs = item.get("attributes", {})
        if attrs.get("certificateType") not in {"IOS_DISTRIBUTION", "DISTRIBUTION"}:
            continue
        if normalize_serial(str(attrs.get("serialNumber", ""))) == expected:
            candidates.append(item)
    if len(candidates) != 1:
        raise RuntimeError(
            f"Expected exactly one Apple distribution certificate matching serial {serial}; found {len(candidates)}"
        )
    item = candidates[0]
    print("Matched Apple distribution certificate:", item.get("attributes", {}).get("displayName") or item["id"])
    return item


def profile_matches(item, bundle_id: str, certificate_id: str) -> bool:
    attrs = item.get("attributes", {})
    if attrs.get("profileState") != "ACTIVE" or attrs.get("profileType") != "IOS_APP_STORE":
        return False
    rel = item.get("relationships", {})
    bundle = rel.get("bundleId", {}).get("data") or {}
    certs = rel.get("certificates", {}).get("data") or []
    return bundle.get("id") == bundle_id and any(c.get("id") == certificate_id for c in certs)


def list_profiles(name: str):
    response = request(
        "GET",
        "/profiles",
        query={"filter[name]": name, "limit": "200"},
    )
    return response.get("data", [])


def create_profile(name: str, bundle_id: str, certificate_id: str):
    payload = {
        "data": {
            "type": "profiles",
            "attributes": {"name": name, "profileType": "IOS_APP_STORE"},
            "relationships": {
                "bundleId": {"data": {"type": "bundleIds", "id": bundle_id}},
                "certificates": {
                    "data": [{"type": "certificates", "id": certificate_id}]
                },
            },
        }
    }
    result = request("POST", "/profiles", payload=payload)["data"]
    print(f"Created App Store provisioning profile: {name}")
    return result


def ensure_profile(name: str, bundle_id: str, certificate_id: str):
    profiles = list_profiles(name)
    for item in profiles:
        if profile_matches(item, bundle_id, certificate_id):
            print(f"Using existing active provisioning profile: {name}")
            return item

    # Remove stale profiles with this dedicated production name before recreating.
    for item in profiles:
        request("DELETE", f"/profiles/{item['id']}")
        print("Deleted stale provisioning profile:", item["id"])

    return create_profile(name, bundle_id, certificate_id)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle-id", required=True)
    parser.add_argument("--bundle-name", required=True)
    parser.add_argument("--profile-name", required=True)
    parser.add_argument("--certificate-serial", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    bundle = ensure_bundle(args.bundle_id, args.bundle_name)
    cert = find_distribution_certificate(args.certificate_serial)
    profile = ensure_profile(args.profile_name, bundle["id"], cert["id"])

    content = profile.get("attributes", {}).get("profileContent")
    if not content:
        # Re-read profile if a list response omitted profileContent.
        profile = request("GET", f"/profiles/{profile['id']}")["data"]
        content = profile.get("attributes", {}).get("profileContent")
    if not content:
        raise RuntimeError("Apple returned no provisioning profile content")

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(base64.b64decode(content))
    print("Production provisioning profile ready:", output)
    print("Bundle resource id:", bundle["id"])
    print("Profile resource id:", profile["id"])

    apps = request(
        "GET",
        "/apps",
        query={"limit": "200", "fields[apps]": "name,bundleId,sku"},
    ).get("data", [])
    matching_apps = [
        item for item in apps
        if item.get("attributes", {}).get("bundleId") == args.bundle_id
    ]
    if matching_apps:
        app = matching_apps[0]
        attrs = app.get("attributes", {})
        print("App Store Connect app record: PRESENT")
        print("App Store Connect app name:", attrs.get("name") or "")
        print("App Store Connect SKU:", attrs.get("sku") or "")
        print("App Store Connect app resource id:", app.get("id") or "")
    else:
        print("App Store Connect app record: MISSING")

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
