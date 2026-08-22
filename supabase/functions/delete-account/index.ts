import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const STORAGE_BUCKETS = ['profile-private', 'profile-public']
const STORAGE_PAGE_SIZE = 100
const STORAGE_REMOVE_BATCH_SIZE = 100
const MAX_STORAGE_ENTRIES = 10_000

async function listUserFiles(
  admin,
  bucket,
  userId,
) {
  const directories = [userId]
  const visited = new Set()
  const files = []
  let inspectedEntries = 0

  while (directories.length > 0) {
    const directory = directories.shift()
    if (!directory) continue
    if (visited.has(directory)) continue
    visited.add(directory)

    let offset = 0
    while (true) {
      const { data: entries, error } = await admin.storage.from(bucket).list(directory, {
        limit: STORAGE_PAGE_SIZE,
        offset,
        sortBy: { column: 'name', order: 'asc' },
      })
      if (error) throw new Error(`storage-list-failed:${bucket}`)

      const page = entries ?? []
      inspectedEntries += page.length
      if (inspectedEntries > MAX_STORAGE_ENTRIES) {
        throw new Error(`storage-entry-limit-exceeded:${bucket}`)
      }

      for (const entry of page) {
        const path = `${directory}/${entry.name}`
        if (entry.id == null && entry.metadata == null) directories.push(path)
        else files.push(path)
      }

      if (page.length < STORAGE_PAGE_SIZE) break
      offset += page.length
    }
  }

  return files
}

async function deleteUserFiles(
  admin,
  bucket,
  userId,
) {
  const files = await listUserFiles(admin, bucket, userId)
  for (let offset = 0; offset < files.length; offset += STORAGE_REMOVE_BATCH_SIZE) {
    const { error } = await admin.storage
      .from(bucket)
      .remove(files.slice(offset, offset + STORAGE_REMOVE_BATCH_SIZE))
    if (error) throw new Error(`storage-remove-failed:${bucket}`)
  }
}

async function deleteAllUserFiles(admin, userId) {
  for (const bucket of STORAGE_BUCKETS) {
    await deleteUserFiles(admin, bucket, userId)
  }
}

Deno.serve(async (req) => {
  if (req.method !== 'POST') return new Response('Method not allowed', { status: 405 })

  const supabaseUrl = Deno.env.get('SUPABASE_URL')
  const publishableKey = Deno.env.get('SUPABASE_ANON_KEY')
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
  const authorization = req.headers.get('Authorization')
  if (!supabaseUrl || !publishableKey || !serviceRoleKey || !authorization) {
    return new Response('Unauthorized', { status: 401 })
  }

  const userClient = createClient(supabaseUrl, publishableKey, {
    global: { headers: { Authorization: authorization } },
    auth: { persistSession: false, autoRefreshToken: false },
  })
  const { data: userData, error: userError } = await userClient.auth.getUser()
  if (userError || !userData.user) return new Response('Unauthorized', { status: 401 })

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  })
  const userId = userData.user.id

  try {
    await deleteAllUserFiles(admin, userId)
  } catch (error) {
    const failure = error instanceof Error ? error.message : 'storage-cleanup-failed'
    console.error('delete-account storage cleanup failed', failure)
    return new Response('Account deletion failed', { status: 500 })
  }

  const { error: deleteError } = await admin.auth.admin.deleteUser(userId)
  if (deleteError) {
    console.error('delete-account auth deletion failed', deleteError.code ?? deleteError.status)
    return new Response('Account deletion failed', { status: 500 })
  }

  try {
    await deleteAllUserFiles(admin, userId)
  } catch (error) {
    const failure = error instanceof Error ? error.message : 'post-delete-storage-cleanup-failed'
    console.error('delete-account post-delete storage cleanup failed', failure)
    return new Response('Account deletion failed', { status: 500 })
  }
  return new Response(null, { status: 204 })
})
