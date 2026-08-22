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
  const response = await fetch(new URL(path, supabaseUrl), {
    method,
    headers: {
      apikey: apiKey,
      Authorization: `Bearer ${token}`,
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
  const code = result.data?.code ?? result.data?.error_code ?? result.data?.error
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

async function rpc(name, token, parameters = {}) {
  return request(`/rest/v1/rpc/${name}`, {
    method: 'POST',
    token,
    json: parameters,
  })
}

async function upload(bucket, path, token) {
  const result = await request(`/storage/v1/object/${bucket}/${path}`, {
    method: 'POST',
    token,
    rawBody: `VIZIT DEV E2E ${runId}`,
    headers: {
      'Content-Type': 'text/plain; charset=utf-8',
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

const emptySnapshot = (displayName) => ({
  firstName: 'E2E',
  lastName: 'User',
  displayName,
  company: '',
  jobTitle: '',
  bio: '',
  displayImagePath: null,
  contactImagePath: null,
  logoPath: null,
  publicSlug: null,
  isPublic: false,
  fieldOrder: [],
  fieldVisibility: {},
  contacts: [],
  addresses: [],
  links: [],
})

try {
  const acceptedUser = await createConfirmedUser('accepted', {
    privacy_policy_version: privacyPolicyVersion,
    terms_version: termsVersion,
  })
  const gatedUser = await createConfirmedUser('gated', {})
  const acceptedToken = await signIn(acceptedUser.email)
  const gatedToken = await signIn(gatedUser.email)

  const acceptedCheck = expectOk(
    await rpc('has_legal_acceptance', acceptedToken, {
      p_privacy_policy_version: privacyPolicyVersion,
      p_terms_version: termsVersion,
    }),
    'accepted legal check',
  )
  assert(acceptedCheck === true, 'signup metadata records legal acceptance')

  const gatedPull = await rpc('get_my_profile_snapshot', gatedToken)
  assert(!gatedPull.response.ok && gatedPull.data?.code === '42501', 'profile pull is legally gated')

  expectOk(
    await rpc('accept_legal_documents', gatedToken, {
      p_privacy_policy_version: privacyPolicyVersion,
      p_terms_version: termsVersion,
    }),
    'accept legal documents',
  )

  const operationId = randomUUID()
  const applied = expectOk(
    await rpc('sync_profile_snapshot', gatedToken, {
      p_operation_id: operationId,
      p_base_version: 0,
      p_snapshot: emptySnapshot('VIZIT E2E'),
    }),
    'profile sync',
  )
  assert(applied?.status === 'applied' && applied?.serverVersion === 1, 'profile sync applies version 1')

  const retry = expectOk(
    await rpc('sync_profile_snapshot', gatedToken, {
      p_operation_id: operationId,
      p_base_version: 0,
      p_snapshot: emptySnapshot('VIZIT E2E'),
    }),
    'profile sync retry',
  )
  assert(JSON.stringify(retry) === JSON.stringify(applied), 'profile sync retry is idempotent')

  const conflict = expectOk(
    await rpc('sync_profile_snapshot', gatedToken, {
      p_operation_id: randomUUID(),
      p_base_version: 0,
      p_snapshot: emptySnapshot('Stale client'),
    }),
    'profile conflict',
  )
  assert(conflict?.status === 'conflict' && conflict?.serverVersion === 1, 'stale sync returns conflict')

  const foreignRows = expectOk(
    await request(`/rest/v1/profiles?select=user_id&user_id=eq.${acceptedUser.id}`, {
      token: gatedToken,
    }),
    'cross-user profile read',
  )
  assert(Array.isArray(foreignRows) && foreignRows.length === 0, 'RLS hides another user profile')

  await upload('profile-private', `${acceptedUser.id}/e2e/nested/private.txt`, acceptedToken)
  await upload('profile-public', `${acceptedUser.id}/e2e/nested/public.txt`, acceptedToken)

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
    await request(`/rest/v1/profiles?select=user_id&user_id=eq.${acceptedUser.id}`, {
      apiKey: serviceRoleKey,
      token: serviceRoleKey,
    }),
    'deleted profile lookup',
  )
  assert(Array.isArray(remainingProfiles) && remainingProfiles.length === 0, 'profile rows cascade on deletion')
  assert((await listStorage('profile-private', acceptedUser.id)).length === 0, 'private media is deleted')
  assert((await listStorage('profile-public', acceptedUser.id)).length === 0, 'public media is deleted')

  console.log('VIZIT Supabase DEV Auth/Profile/RLS/Storage/Delete E2E: PASS')
} finally {
  await removeStorageFixtures().catch(() => undefined)
  for (const userId of createdUserIds) {
    await deleteUserAsAdmin(userId).catch(() => undefined)
  }
}
