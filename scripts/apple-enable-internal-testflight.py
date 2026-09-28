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
old=app_by_bundle(OLD_BUNDLE)

builds=get("/builds",{
    "filter[app]":prod["id"],
    "sort":"-uploadedDate",
    "limit":"10",
    "fields[builds]":"version,processingState,uploadedDate",
}).get("data",[])
if not builds:
    raise SystemExit("No production TestFlight build found")
build=builds[0]
attrs=build.get("attributes",{})
if attrs.get("processingState")!="VALID":
    raise SystemExit(f"Latest production build is not VALID: {attrs.get('processingState')}")
print("Production build:", attrs.get("version"), build["id"])

old_groups=get("/betaGroups",{
    "filter[app]":old["id"],
    "filter[isInternalGroup]":"true",
    "limit":"100",
}).get("data",[])
tester_ids=[]
for g in old_groups:
    rel=get(f"/betaGroups/{g['id']}/relationships/betaTesters",{"limit":"200"}).get("data",[])
    tester_ids.extend(t["id"] for t in rel)
tester_ids=sorted(set(tester_ids))
if len(tester_ids)!=1:
    raise SystemExit(f"Expected exactly one legacy internal tester, found {len(tester_ids)}")
tester_id=tester_ids[0]
print("Reusing one legacy internal tester:", tester_id)

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

post(f"/betaGroups/{group['id']}/relationships/betaTesters",{
    "data":[{"type":"betaTesters","id":tester_id}]
})
print("Legacy internal tester assigned to production beta group")

linked_builds=get(f"/betaGroups/{group['id']}/relationships/builds",{"limit":"200"}).get("data",[])
linked_testers=get(f"/betaGroups/{group['id']}/relationships/betaTesters",{"limit":"200"}).get("data",[])
if build["id"] not in {x["id"] for x in linked_builds}:
    raise SystemExit("Build relationship verification failed")
if tester_id not in {x["id"] for x in linked_testers}:
    raise SystemExit("Tester relationship verification failed")

print("PRODUCTION_INTERNAL_TESTFLIGHT=ENABLED")
print("GROUP_ID="+group["id"])
print("BUILD_VERSION="+str(attrs.get("version","")))
print("TESTER_COUNT="+str(len(linked_testers)))
