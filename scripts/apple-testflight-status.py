#!/usr/bin/env python3
from __future__ import annotations
import base64, datetime as dt, json, os, urllib.parse, urllib.request
import jwt

API="https://api.appstoreconnect.apple.com/v1"
BUNDLE_ID="hu.rayworks.vizit"

def token():
    now=int(dt.datetime.now(dt.timezone.utc).timestamp())
    key=base64.b64decode(os.environ["APP_STORE_CONNECT_API_KEY_BASE64"]).decode()
    return jwt.encode(
        {"iss":os.environ["APP_STORE_CONNECT_API_ISSUER_ID"],"iat":now,"exp":now+600,"aud":"appstoreconnect-v1"},
        key,algorithm="ES256",
        headers={"kid":os.environ["APP_STORE_CONNECT_API_KEY_ID"],"typ":"JWT"},
    )

def get(path, query=None):
    url=API+path
    if query: url+="?"+urllib.parse.urlencode(query)
    req=urllib.request.Request(url,headers={"Authorization":"Bearer "+token(),"Accept":"application/json"})
    with urllib.request.urlopen(req,timeout=60) as r:
        return json.load(r)

apps=get("/apps",{"filter[bundleId]":BUNDLE_ID,"limit":"10"}).get("data",[])
if not apps:
    raise SystemExit("PRODUCTION_APP_RECORD=MISSING")
app=apps[0]
print("PRODUCTION_APP_RECORD=PRESENT")
print("APP_ID="+app["id"])
print("APP_NAME="+str(app.get("attributes",{}).get("name","")))
print("BUNDLE_ID="+str(app.get("attributes",{}).get("bundleId","")))

builds=get("/builds",{
    "filter[app]":app["id"],
    "sort":"-uploadedDate",
    "limit":"10",
    "fields[builds]":"version,uploadedDate,expirationDate,expired,minOsVersion,processingState",
}).get("data",[])
print("BUILD_COUNT="+str(len(builds)))
for i,b in enumerate(builds,1):
    a=b.get("attributes",{})
    print(f"BUILD_{i}_VERSION={a.get('version','')}")
    print(f"BUILD_{i}_PROCESSING_STATE={a.get('processingState','')}")
    print(f"BUILD_{i}_UPLOADED_DATE={a.get('uploadedDate','')}")
    print(f"BUILD_{i}_EXPIRED={a.get('expired','')}")


groups=get("/betaGroups",{
    "filter[app]":app["id"],
    "limit":"100",
    "fields[betaGroups]":"name,isInternalGroup,publicLinkEnabled,publicLink",
}).get("data",[])
print("BETA_GROUP_COUNT="+str(len(groups)))
for i,g in enumerate(groups,1):
    a=g.get("attributes",{})
    print(f"BETA_GROUP_{i}_ID={g.get('id','')}")
    print(f"BETA_GROUP_{i}_NAME={a.get('name','')}")
    print(f"BETA_GROUP_{i}_INTERNAL={a.get('isInternalGroup','')}")
    print(f"BETA_GROUP_{i}_PUBLIC_LINK_ENABLED={a.get('publicLinkEnabled','')}")
