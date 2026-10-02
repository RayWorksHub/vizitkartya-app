#!/usr/bin/env python3
from __future__ import annotations

import base64
import datetime as dt
import json
import os
import urllib.error
import urllib.parse
import urllib.request

import jwt

API = "https://api.appstoreconnect.apple.com/v1"
BUNDLE_ID = "hu.rayworks.vizit"
TARGET_MARKETING_VERSION = os.environ.get("VIZIT_TARGET_MARKETING_VERSION", "").strip()
TARGET_BUILD_VERSION = os.environ.get("VIZIT_TARGET_BUILD_VERSION", "").strip()

if not TARGET_MARKETING_VERSION or not TARGET_BUILD_VERSION:
    raise SystemExit(
        "VIZIT_TARGET_MARKETING_VERSION and VIZIT_TARGET_BUILD_VERSION are required; "
        "refusing to expire any TestFlight builds"
    )


def token() -> str:
    now = int(dt.datetime.now(dt.timezone.utc).timestamp())
    key = base64.b64decode(os.environ["APP_STORE_CONNECT_API_KEY_BASE64"]).decode()
    return jwt.encode(
        {
            "iss": os.environ["APP_STORE_CONNECT_API_ISSUER_ID"],
            "iat": now,
            "exp": now + 600,
            "aud": "appstoreconnect-v1",
        },
        key,
        algorithm="ES256",
        headers={"kid": os.environ["APP_STORE_CONNECT_API_KEY_ID"], "typ": "JWT"},
    )


def request(method: str, path: str, query: dict[str, str] | None = None, payload: dict | None = None) -> dict:
    url = API + path
    if query:
        url += "?" + urllib.parse.urlencode(query)
    headers = {"Authorization": "Bearer " + token(), "Accept": "application/json"}
    data = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode()
    try:
        with urllib.request.urlopen(
            urllib.request.Request(url, method=method, headers=headers, data=data),
            timeout=60,
        ) as response:
            body = response.read()
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as error:
        detail = error.read(8192).decode("utf-8", errors="replace")
        raise SystemExit(f"{method} {path} failed HTTP {error.code}: {detail[:2000]}") from error


apps = request("GET", "/apps", {"filter[bundleId]": BUNDLE_ID, "limit": "10"}).get("data", [])
exact_apps = [app for app in apps if app.get("attributes", {}).get("bundleId") == BUNDLE_ID]
if len(exact_apps) != 1:
    raise SystemExit(f"Expected one {BUNDLE_ID} app, found {len(exact_apps)}")

response = request(
    "GET",
    "/builds",
    {
        "filter[app]": exact_apps[0]["id"],
        "sort": "-uploadedDate",
        "limit": "200",
        "include": "preReleaseVersion",
        "fields[builds]": "version,uploadedDate,expired,processingState,preReleaseVersion",
        "fields[preReleaseVersions]": "version",
    },
)
versions = {
    item["id"]: str(item.get("attributes", {}).get("version", ""))
    for item in response.get("included", [])
    if item.get("type") == "preReleaseVersions"
}

targets: list[tuple[str, str, str]] = []
active_target_found = False
for build in response.get("data", []):
    relation = build.get("relationships", {}).get("preReleaseVersion", {}).get("data") or {}
    marketing_version = versions.get(str(relation.get("id", "")), "")
    build_number = str(build.get("attributes", {}).get("version", ""))
    try:
        major = int(marketing_version.split(".", 1)[0])
    except ValueError:
        continue
    if major >= 9 and not bool(build.get("attributes", {}).get("expired")):
        if marketing_version == TARGET_MARKETING_VERSION and build_number == TARGET_BUILD_VERSION:
            active_target_found = True
            print(f"Preserving current TestFlight build {marketing_version} ({build_number})")
            continue
        targets.append((build["id"], marketing_version, build_number))

if not active_target_found:
    raise SystemExit(
        f"Active target TestFlight build {TARGET_MARKETING_VERSION} ({TARGET_BUILD_VERSION}) was not found; "
        "refusing to expire any builds"
    )

for build_id, marketing_version, build_number in targets:
    request(
        "PATCH",
        f"/builds/{build_id}",
        payload={"data": {"type": "builds", "id": build_id, "attributes": {"expired": True}}},
    )
    verified = request("GET", f"/builds/{build_id}", {"fields[builds]": "version,expired"})
    if not bool(verified.get("data", {}).get("attributes", {}).get("expired")):
        raise SystemExit(f"Build {marketing_version} ({build_number}) was not expired")
    print(f"Expired TestFlight build {marketing_version} ({build_number})")

print(f"EXPIRED_POST_V8_BUILD_COUNT={len(targets)}")
