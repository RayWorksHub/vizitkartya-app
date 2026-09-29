#!/usr/bin/env python3
from __future__ import annotations
import base64, datetime as dt, json, os, urllib.parse, urllib.request, urllib.error
import jwt

API="https://api.appstoreconnect.apple.com/v1"
PROD_BUNDLE="hu.rayworks.vizit"
OLD_BUNDLE="hu.rayworks.vizit.ios.dev"
GROUP_NAME="VIZIT belső tesztelők"

def token():
    now=int(dt.datetime.now(dt.timezone.utc).timestamp())
    key=base64.b64decode(os.environ["APP_STORE_CONNECT_API_KEY_BASE64"]).decode()
    return jwt.encode(
        {"iss":os.environ["APP_STORE_CONNECT_API_ISSUER_ID"],"iat":now,"exp":now+600,"aud":"appstoreconnect-v1"},
        key,algorithm="ES256",
        headers={"kid":os.environ["APP_STORE_CONNECT_API_KEY_ID"],"typ":"JWT"},
    )

def request(method,path,query=None,payload=None):
    url=API+path
    if query: url+="?"+urllib.parse.urlencode(query)
    headers={"Authorization":"Bearer "+token(),"Accept":"application/json"}
    data=None
    if payload is not None:
        headers["Content-Type"]="application/json"
        data=json.dumps(payload).encode()
    req=urllib.request.Request(url,method=method,headers=headers,data=data)
    try:
        with urllib.request.urlopen(req,timeout=60) as r:
            body=r.read()
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        detail=e.read().decode(errors="replace")
        raise SystemExit(f"{method} {path} failed HTTP {e.code}: {detail[:2000]}")

def get(path,query=None): return request("GET",path,query=query)
def post(path,payload): return request("POST",path,payload=payload)

def app_by_bundle(bundle):
    rows=get("/apps",{"filter[bundleId]":bundle,"limit":"50"}).get("data",[])
    exact=[row for row in rows if row.get("attributes",{}).get("bundleId")==bundle]
    if len(exact)!=1:
        raise SystemExit(f"Expected exactly one exact app for {bundle}, found {len(exact)}")
    return exact[0]

prod=app_by_bundle(PROD_BUNDLE)

builds=get("/builds",{
    "filter[app]":prod["id"],
    "sort":"-uploadedDate",
    "limit":"200",
    "fields[builds]":"version,processingState,uploadedDate",
}).get("data",[])
if not builds:
    raise SystemExit("No production TestFlight build found")
target_version=os.environ.get("VIZIT_TARGET_BUILD_VERSION", "").strip()
if target_version:
    matching_builds=[
        candidate for candidate in builds
        if str(candidate.get("attributes",{}).get("version",""))==target_version
    ]
    if not matching_builds:
        raise SystemExit(f"Production TestFlight build {target_version} is not visible yet")
    build=matching_builds[0]
else:
    build=builds[0]
attrs=build.get("attributes",{})
if attrs.get("processingState")!="VALID":
    raise SystemExit(f"Latest production build is not VALID: {attrs.get('processingState')}")
print("Production build:", attrs.get("version"), build["id"])

groups=get("/betaGroups",{
    "filter[app]":prod["id"],
    "filter[name]":GROUP_NAME,
    "limit":"10",
}).get("data",[])
if groups:
    group=groups[0]
    print("Using existing production beta group:", group["id"])
else:
    group=post("/betaGroups",{
        "data":{
            "type":"betaGroups",
            "attributes":{"name":GROUP_NAME,"isInternalGroup":True},
            "relationships":{"app":{"data":{"type":"apps","id":prod["id"]}}},
        }
    })["data"]
    print("Created production beta group:", group["id"])

post(f"/builds/{build['id']}/relationships/betaGroups",{
    "data":[{"type":"betaGroups","id":group["id"]}]
})
print("Build assigned to production beta group")

# Keep subsequent releases idempotent: once the production group already has
# its tester, reuse that relationship instead of trying to create a duplicate
# betaTester resource (which App Store Connect rejects with HTTP 409).
linked_testers=get(f"/betaGroups/{group['id']}/relationships/betaTesters",{"limit":"200"}).get("data",[])
if linked_testers:
    production_tester_id=linked_testers[0]["id"]
    print("Using existing production internal tester assignment:", production_tester_id)
else:
    old=app_by_bundle(OLD_BUNDLE)
    old_groups=get("/betaGroups",{
        "filter[app]":old["id"],
        "filter[isInternalGroup]":"true",
        "limit":"100",
    }).get("data",[])
    tester_ids=[]
    for old_group in old_groups:
        rel=get(
            f"/betaGroups/{old_group['id']}/relationships/betaTesters",
            {"limit":"200"},
        ).get("data",[])
        tester_ids.extend(tester["id"] for tester in rel)
    tester_ids=sorted(set(tester_ids))
    if len(tester_ids)!=1:
        raise SystemExit(f"Expected exactly one legacy internal tester, found {len(tester_ids)}")
    legacy_tester=get(f"/betaTesters/{tester_ids[0]}",{
        "fields[betaTesters]":"email,firstName,lastName,state"
    }).get("data",{})
    legacy_attrs=legacy_tester.get("attributes",{})
    tester_email=str(legacy_attrs.get("email","")).strip()
    if not tester_email:
        raise SystemExit("Legacy internal tester has no email")

    created_tester=post("/betaTesters",{
        "data":{
            "type":"betaTesters",
            "attributes":{
                "email":tester_email,
                "firstName":legacy_attrs.get("firstName") or "",
                "lastName":legacy_attrs.get("lastName") or "",
            },
            "relationships":{
                "betaGroups":{
                    "data":[{"type":"betaGroups","id":group["id"]}]
                }
            }
        }
    }).get("data",{})
    production_tester_id=created_tester.get("id")
    if not production_tester_id:
        raise SystemExit("Apple created no production beta tester resource")
    print("Production internal tester assignment created:", production_tester_id)

linked_builds=get(f"/betaGroups/{group['id']}/relationships/builds",{"limit":"200"}).get("data",[])
linked_testers=get(f"/betaGroups/{group['id']}/relationships/betaTesters",{"limit":"200"}).get("data",[])
if build["id"] not in {x["id"] for x in linked_builds}:
    raise SystemExit("Build relationship verification failed")
if production_tester_id not in {x["id"] for x in linked_testers}:
    raise SystemExit("Tester relationship verification failed")

print("PRODUCTION_INTERNAL_TESTFLIGHT=ENABLED")
print("GROUP_ID="+group["id"])
print("BUILD_VERSION="+str(attrs.get("version","")))
print("TESTER_COUNT="+str(len(linked_testers)))
