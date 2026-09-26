"""Submit the user's requested production release. No tests or review bypasses.
Credentials are read from the repository's authorized encrypted secret, never logs.
"""
import base64, json, os, pathlib, subprocess, tempfile, time
import urllib.error, urllib.parse, urllib.request
out = pathlib.Path('release-output/play-upload.json')
def report(status, detail):
    value = {'status':status,'detail':detail,'package':'hu.rayworks.vizit','track':'production'}
    out.write_text(json.dumps(value,ensure_ascii=False,indent=2))
    print(status + ': ' + detail)
raw = os.environ.get('GOOGLE_PLAY_SERVICE_ACCOUNT_JSON','')
if not raw or os.environ.get('SIGNED') != 'true':
    missing = []
    if not raw: missing.append('Google Play API authorization is not configured')
    if os.environ.get('SIGNED') != 'true': missing.append('production signing credentials are not configured')
    report('BLOCKED', '; '.join(missing))
    raise SystemExit(0)
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, hdrs, newurl): return None
opener = urllib.request.build_opener(NoRedirect)
def request(url, method='POST', payload=None, token=None, binary=None):
    assert urllib.parse.urlsplit(url).hostname in {'oauth2.googleapis.com','androidpublisher.googleapis.com'}
    headers={'Content-Type':'application/json'}
    data=json.dumps(payload).encode() if payload is not None else None
    if binary is not None: data=binary; headers['Content-Type']='application/octet-stream'
    if token: headers['Authorization']='Bearer '+token
    req=urllib.request.Request(url,data=data,headers=headers,method=method)
    with opener.open(req,timeout=120) as response: return json.load(response)
def encoded(value): return base64.urlsafe_b64encode(value).rstrip(b'=')
try:
    credentials=json.loads(raw)
    assert credentials['type']=='service_account'
    assert credentials['client_email'].endswith('.gserviceaccount.com')
    now=int(time.time())
    claims={'iss':credentials['client_email'],'scope':'https://www.googleapis.com/auth/androidpublisher',
            'aud':'https://oauth2.googleapis.com/token','iat':now,'exp':now+300}
    signing=encoded(json.dumps({'alg':'RS256','typ':'JWT'}).encode())+b'.'+encoded(json.dumps(claims).encode())
    with tempfile.TemporaryDirectory() as temp:
        key=pathlib.Path(temp)/'key.pem';key.write_text(credentials['private_key']);key.chmod(0o600)
        sig=subprocess.run(['openssl','dgst','-sha256','-sign',str(key)],input=signing,capture_output=True,check=True).stdout
    assertion=(signing+b'.'+encoded(sig)).decode()
    body=urllib.parse.urlencode({'grant_type':'urn:ietf:params:oauth:grant-type:jwt-bearer','assertion':assertion}).encode()
    req=urllib.request.Request('https://oauth2.googleapis.com/token',data=body,headers={'Content-Type':'application/x-www-form-urlencoded'})
    with opener.open(req,timeout=30) as response: token=json.load(response)['access_token']
    root='https://androidpublisher.googleapis.com/androidpublisher/v3/applications/hu.rayworks.vizit'
    edit=request(root+'/edits',payload={},token=token)['id']
    aab=next(pathlib.Path('release-output').glob('*.aab'))
    bundle=request('https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/hu.rayworks.vizit/edits/'+edit+'/bundles?uploadType=media',token=token,binary=aab.read_bytes())
    version=str(bundle['versionCode'])
    assert version=='7030001'
    request(root+'/edits/'+edit+'/tracks/production',method='PUT',token=token,payload={
        'track':'production','releases':[{'name':'7.3.1','versionCodes':[version],'status':'completed',
        'releaseNotes':[{'language':'hu-HU','text':'A profiladatok és a belépés az új központi backendhez kapcsolódnak.'}]}]})
    request(root+'/edits/'+edit+':commit',token=token,payload={})
    report('SUBMITTED','Google Play accepted the production release submission; Google review/publication state is managed by Play Console.')
except urllib.error.HTTPError as error:
    # Only the provider's bounded human-readable message is recorded, never credentials.
    try: detail=json.loads(error.read(8192)).get('error',{})
    except Exception: detail={}
    message=detail.get('message','Google Play rejected the request') if isinstance(detail,dict) else 'Google authorization failed'
    report('BLOCKED',str(error.code)+': '+str(message)[:600])
    raise SystemExit(1)
except Exception as error:
    report('BLOCKED','Release submission failed: '+type(error).__name__)
    raise SystemExit(1)
