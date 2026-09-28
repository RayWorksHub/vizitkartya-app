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


OLD_BUNDLE_ID="hu.rayworks.vizit.ios.dev"
old_apps=get("/apps",{"filter[bundleId]":OLD_BUNDLE_ID,"limit":"10"}).get("data",[])
print("OLD_APP_COUNT="+str(len(old_apps)))
if old_apps:
    old_app=old_apps[0]
    print("OLD_APP_ID="+old_app["id"])
    old_groups=get("/betaGroups",{
        "filter[app]":old_app["id"],
        "limit":"100",
        "fields[betaGroups]":"name,isInternalGroup,publicLinkEnabled,publicLink",
    }).get("data",[])
    print("OLD_BETA_GROUP_COUNT="+str(len(old_groups)))
    for i,g in enumerate(old_groups,1):
        a=g.get("attributes",{})
        print(f"OLD_BETA_GROUP_{i}_ID={g.get('id','')}")
        print(f"OLD_BETA_GROUP_{i}_NAME={a.get('name','')}")
        print(f"OLD_BETA_GROUP_{i}_INTERNAL={a.get('isInternalGroup','')}")
        testers=get(f"/betaGroups/{g['id']}/betaTesters",{
            "limit":"200",
            "fields[betaTesters]":"email,firstName,lastName,state",
        }).get("data",[])
        print(f"OLD_BETA_GROUP_{i}_TESTER_COUNT={len(testers)}")
        for j,t in enumerate(testers,1):
            ta=t.get("attributes",{})
            email=str(ta.get("email",""))
            masked=email
            if "@" in email:
                local,domain=email.split("@",1)
                masked=(local[:2]+"***@"+domain) if local else "***@"+domain
            print(f"OLD_BETA_GROUP_{i}_TESTER_{j}_ID={t.get('id','')}")
            print(f"OLD_BETA_GROUP_{i}_TESTER_{j}_EMAIL_MASKED={masked}")
            print(f"OLD_BETA_GROUP_{i}_TESTER_{j}_STATE={ta.get('state','')}")


if old_apps and old_groups:
    legacy_tester_ids=[]
    for g in old_groups:
        rel=get(f"/betaGroups/{g['id']}/relationships/betaTesters",{"limit":"200"}).get("data",[])
        legacy_tester_ids.extend(t["id"] for t in rel)
    legacy_tester_ids=sorted(set(legacy_tester_ids))
    if len(legacy_tester_ids)==1:
        tester=get(f"/betaTesters/{legacy_tester_ids[0]}",{
            "fields[betaTesters]":"email,firstName,lastName,state"
        }).get("data",{})
        email=str(tester.get("attributes",{}).get("email",""))
        users=get("/users",{
            "filter[username]":email,
            "limit":"50",
            "fields[users]":"username,firstName,lastName,roles,allAppsVisible,provisioningAllowed",
        }).get("data",[])
        exact_users=[u for u in users if u.get("attributes",{}).get("username")==email]
        print("MATCHING_ASC_USER_COUNT="+str(len(exact_users)))
        for i,u in enumerate(exact_users,1):
            ua=u.get("attributes",{})
            print(f"ASC_USER_{i}_ID={u.get('id','')}")
            print(f"ASC_USER_{i}_ALL_APPS_VISIBLE={ua.get('allAppsVisible','')}")
            print(f"ASC_USER_{i}_ROLES={','.join(ua.get('roles',[]) or [])}")
            visible=get(f"/users/{u['id']}/relationships/visibleApps",{"limit":"200"}).get("data",[])
            visible_ids={x["id"] for x in visible}
            print(f"ASC_USER_{i}_PROD_APP_VISIBLE={app['id'] in visible_ids}")
