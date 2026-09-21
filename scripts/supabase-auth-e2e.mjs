#!/usr/bin/env node

import { randomUUID } from 'node:crypto'

const required = (name) => {
  const value = process.env[name]?.trim()
  if (!value) throw new Error(`Missing required environment variable: ${name}`)
  return value
}

const environment = required('VIZIT_E2E_ENVIRONMENT').toUpperCase()
if (environment !== 'DEV') throw new Error('The E2E suite is restricted to a DEV environment')
if (required('VIZIT_E2E_CONFIRM_MUTATION') !== 'CREATE_AND_DELETE_TEMP_USERS') {
  throw new Error('The DEV mutation confirmation is missing')
}
const scope = (process.env.VIZIT_E2E_SCOPE ?? 'FULL').trim().toUpperCase()
if (scope !== 'CORE' && scope !== 'FULL') {
  throw new Error('VIZIT_E2E_SCOPE must be CORE or FULL')
}

const supabaseUrl = new URL(required('VIZIT_DEV_SUPABASE_URL'))
const projectRef = required('VIZIT_DEV_SUPABASE_PROJECT_REF')
if (supabaseUrl.protocol !== 'https:' || supabaseUrl.hostname !== `${projectRef}.supabase.co`) {
  throw new Error('The DEV project reference does not match the Supabase URL')
}
const productionUrl = process.env.VIZIT_PROD_SUPABASE_URL?.trim()
if (productionUrl && new URL(productionUrl).origin === supabaseUrl.origin) {
  throw new Error('The E2E suite refuses to run when DEV and PROD URLs match')
}

const publishableKey = required('VIZIT_DEV_SUPABASE_KEY')
const serviceRoleKey = required('VIZIT_DEV_SUPABASE_SERVICE_ROLE_KEY')
const privacyPolicyVersion = required('VIZIT_PRIVACY_POLICY_VERSION')
const termsVersion = required('VIZIT_TERMS_VERSION')
const runId = randomUUID()
const password = `Vizit-${randomUUID()}-A1!`
const createdUserIds = new Set()
const uploadedFiles = []
const avatarFixture = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2ZAAAAABJRU5ErkJggg==',
  'base64',
)

const safeCode = (value) => String(value ?? 'unknown')
  .replace(/[^a-zA-Z0-9_.-]/g, '')
  .slice(0, 64) || 'unknown'

async function request(
  path,
  {
    method = 'GET',
    apiKey = publishableKey,
    token = apiKey,
    json,
    rawBody,
    headers = {},
  } = {},
) {
  const isOpaqueApiKey = token === apiKey && /^sb_(?:publishable|secret)_/.test(token)
  const response = await fetch(new URL(path, supabaseUrl), {
    method,
    headers: {
      apikey: apiKey,
      ...(token && !isOpaqueApiKey ? { Authorization: `Bearer ${token}` } : {}),
      ...(json === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...headers,
    },
    body: json === undefined ? rawBody : JSON.stringify(json),
  })
  const raw = await response.text()
  let data = null
  if (raw) {
    try {
      data = JSON.parse(raw)
    } catch {
      data = null
    }
  }
  return { response, data }
}

function expectOk(result, label) {
  if (result.response.ok) return result.data
  const code = result.data?.error_code ?? result.data?.code ?? result.data?.error
  throw new Error(`${label} failed (HTTP ${result.response.status}, code ${safeCode(code)})`)
}

function assert(condition, label) {
  if (!condition) throw new Error(`Assertion failed: ${label}`)
}

async function createConfirmedUser(label, userMetadata) {
  const result = await request('/auth/v1/admin/users', {
    method: 'POST',
    apiKey: serviceRoleKey,
    token: serviceRoleKey,
    json: {
      email: `vizit-e2e-${label}-${runId}@example.invalid`,
      password,
      email_confirm: true,
      user_metadata: userMetadata,
    },
  })
  const user = expectOk(result, `create ${label} user`)
  assert(typeof user?.id === 'string', `${label} user has an id`)
  createdUserIds.add(user.id)
  return user
}

async function signIn(email) {
  const result = await request('/auth/v1/token?grant_type=password', {
    method: 'POST',
    json: { email, password },
  })
  const session = expectOk(result, 'password sign-in')
  assert(typeof session?.access_token === 'string', 'sign-in returns an access token')
  return session.access_token
}

async function upload(bucket, path, token) {
  const result = await request(`/storage/v1/object/${bucket}/${path}`, {
    method: 'POST',
    token,
    rawBody: avatarFixture,
    headers: {
      'Content-Type': 'image/png',
      'cache-control': 'max-age=60',
      'x-upsert': 'false',
    },
  })
  expectOk(result, `upload ${bucket} fixture`)
  uploadedFiles.push({ bucket, path })
}

async function listStorage(bucket, prefix) {
  const result = await request(`/storage/v1/object/list/${bucket}`, {
    method: 'POST',
    apiKey: serviceRoleKey,
    token: serviceRoleKey,
    json: { prefix, limit: 100, offset: 0, sortBy: { column: 'name', order: 'asc' } },
  })
  return expectOk(result, `list ${bucket}`) ?? []
}

async function removeStorageFixtures() {
  const byBucket = new Map()
  for (const file of uploadedFiles) {
    const files = byBucket.get(file.bucket) ?? []
    files.push(file)
    byBucket.set(file.bucket, files)
  }
  for (const [bucket, files] of byBucket) {
    await request(`/storage/v1/object/${bucket}`, {
      method: 'DELETE',
      apiKey: serviceRoleKey,
      token: serviceRoleKey,
      json: { prefixes: files.map((file) => file.path) },
    })
  }
}

async function deleteUserAsAdmin(userId) {
  return request(`/auth/v1/admin/users/${userId}`, {
    method: 'DELETE',
    apiKey: serviceRoleKey,
    token: serviceRoleKey,
  })
}

try {
  // The workflow validates that public signup and email confirmation are
  // enabled before this suite runs. Create the disposable account through the
  // admin endpoint so repeated releases do not depend on the provider's email
  // delivery quota; auth.users triggers still receive the exact client
  // metadata and exercise the same consent persistence path.
  const acceptedUser = await createConfirmedUser('accepted', {
    display_name: 'VIZIT E2E',
    privacy_version: privacyPolicyVersion,
    terms_version: termsVersion,
  })
  const isolationUser = await createConfirmedUser('isolation', {})
  const acceptedToken = await signIn(acceptedUser.email)
  const isolationToken = await signIn(isolationUser.email)

  assert(
    acceptedUser.user_metadata?.privacy_version === privacyPolicyVersion
      && acceptedUser.user_metadata?.terms_version === termsVersion,
    'release fixture stores the legal document versions used by the iOS client',
  )

  const consentRows = expectOk(
    await request(`/rest/v1/privacy_consents?select=user_id,privacy_version&user_id=eq.${acceptedUser.id}`, {
      token: acceptedToken,
    }),
    'release consent lookup',
  )
  assert(
    Array.isArray(consentRows)
      && consentRows.length === 1
      && consentRows[0]?.privacy_version === privacyPolicyVersion,
    'auth trigger records the accepted privacy version',
  )

  const profileId = randomUUID()
  const profileSlug = `vizit-e2e-${runId}`
  const customDomain = `e2e-${runId}.vizit.invalid`
  const profileRows = expectOk(
    await request('/rest/v1/profiles?select=id,owner_id,slug,display_name,is_public,custom_domain,custom_domain_verified', {
      method: 'POST',
      token: acceptedToken,
      headers: { Prefer: 'return=representation' },
      json: {
        id: profileId,
        owner_id: acceptedUser.id,
        slug: profileSlug,
        display_name: 'VIZIT E2E',
        job_title: 'Tesztelő',
        company: 'VIZIT',
        bio: 'Izolált DEV release E2E profil.',
        public_email: acceptedUser.email,
        phone: '+36 30 000 0000',
        website: 'https://vizit.hu',
        address: 'Budapest',
        is_public: false,
        custom_domain: customDomain,
      },
    }),
    'create private profile',
  )
  const profile = profileRows?.[0]
  assert(
    profile?.id === profileId
      && profile?.owner_id === acceptedUser.id
      && profile?.slug === profileSlug
      && profile?.is_public === false
      && profile?.custom_domain === customDomain
      && profile?.custom_domain_verified === false,
    'owner creates a private profile with protected custom-domain state',
  )

  const socialRows = expectOk(
    await request('/rest/v1/social_links?select=id,profile_id,platform,url', {
      method: 'POST',
      token: acceptedToken,
      headers: { Prefer: 'return=representation' },
      json: {
        profile_id: profileId,
        platform: 'linkedin',
        label: 'LinkedIn',
        url: 'https://www.linkedin.com/in/vizit-e2e',
        sort_order: 0,
        enabled: true,
      },
    }),
    'create social link',
  )
  const socialLinkId = socialRows?.[0]?.id
  assert(typeof socialLinkId === 'string', 'social link is persisted')

  const foreignRows = expectOk(
    await request(`/rest/v1/profiles?select=id&id=eq.${profileId}`, {
      token: isolationToken,
    }),
    'cross-user profile read',
  )
  assert(Array.isArray(foreignRows) && foreignRows.length === 0, 'RLS hides another private profile')

  const updatedRows = expectOk(
    await request(`/rest/v1/profiles?id=eq.${profileId}&owner_id=eq.${acceptedUser.id}&select=id,display_name,is_public,views_count`, {
      method: 'PATCH',
      token: acceptedToken,
      headers: { Prefer: 'return=representation' },
      json: { display_name: 'VIZIT E2E Frissítve', is_public: true },
    }),
    'publish profile',
  )
  assert(
    updatedRows?.[0]?.display_name === 'VIZIT E2E Frissítve'
      && updatedRows?.[0]?.is_public === true,
    'owner update publishes the profile',
  )

  const publicRows = expectOk(
    await request(`/rest/v1/profiles?select=id,slug,display_name&id=eq.${profileId}`, { token: null }),
    'anonymous public profile read',
  )
  assert(Array.isArray(publicRows) && publicRows.length === 1, 'published profile is readable by the website')

  const publicSocialRows = expectOk(
    await request(`/rest/v1/social_links?select=id,platform,url&id=eq.${socialLinkId}`, { token: null }),
    'anonymous social link read',
  )
  assert(Array.isArray(publicSocialRows) && publicSocialRows.length === 1, 'published social link is readable by the website')

  expectOk(
    await request('/rest/v1/profile_events', {
      method: 'POST',
      token: null,
      // Anonymous visitors may create events for public profiles, but the
      // events themselves are intentionally readable only by the owner.
      // Asking PostgREST to return the inserted row would therefore apply the
      // SELECT policy and turn a valid INSERT into a 401/42501 response.
      headers: { Prefer: 'return=minimal' },
      json: { profile_id: profileId, event_type: 'view', link_key: 'e2e' },
    }),
    'record public profile view',
  )

  const restoredToken = await signIn(acceptedUser.email)
  const restoredRows = expectOk(
    await request(`/rest/v1/profiles?select=id,display_name,views_count&id=eq.${profileId}`, {
      token: restoredToken,
    }),
    'profile restore after a new login',
  )
  assert(
    restoredRows?.[0]?.display_name === 'VIZIT E2E Frissítve'
      && Number(restoredRows?.[0]?.views_count) === 1,
    'a new login restores the profile and the public view counter is updated',
  )

  await upload('avatars', `${acceptedUser.id}/e2e/nested/avatar.png`, acceptedToken)

  if (scope === 'FULL') {
    const deletion = await request('/functions/v1/delete-account', {
      method: 'POST',
      token: acceptedToken,
      json: {},
    })
    assert(deletion.response.status === 204, 'delete-account returns 204')
    createdUserIds.delete(acceptedUser.id)

    const deletedUser = await request(`/auth/v1/admin/users/${acceptedUser.id}`, {
      apiKey: serviceRoleKey,
      token: serviceRoleKey,
    })
    assert(deletedUser.response.status === 404, 'the Auth user is deleted')

    const remainingProfiles = expectOk(
      await request(`/rest/v1/profiles?select=id&owner_id=eq.${acceptedUser.id}`, {
        apiKey: serviceRoleKey,
        token: serviceRoleKey,
      }),
      'deleted profile lookup',
    )
    assert(Array.isArray(remainingProfiles) && remainingProfiles.length === 0, 'profile rows cascade on deletion')

    const remainingConsents = expectOk(
      await request(`/rest/v1/privacy_consents?select=user_id&user_id=eq.${acceptedUser.id}`, {
        apiKey: serviceRoleKey,
        token: serviceRoleKey,
      }),
      'deleted consent lookup',
    )
    assert(Array.isArray(remainingConsents) && remainingConsents.length === 0, 'consent rows cascade on deletion')

    const remainingSocialLinks = expectOk(
      await request(`/rest/v1/social_links?select=id&profile_id=eq.${profileId}`, {
        apiKey: serviceRoleKey,
        token: serviceRoleKey,
      }),
      'deleted social-link lookup',
    )
    assert(Array.isArray(remainingSocialLinks) && remainingSocialLinks.length === 0, 'social links cascade on deletion')
    assert((await listStorage('avatars', acceptedUser.id)).length === 0, 'avatar media is deleted')
  }

  console.log(`VIZIT Supabase DEV ${scope} E2E: PASS`)
} finally {
  await removeStorageFixtures().catch(() => undefined)
  for (const userId of createdUserIds) {
    await deleteUserAsAdmin(userId).catch(() => undefined)
  }
}
