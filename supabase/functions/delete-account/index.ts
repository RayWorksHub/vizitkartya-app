import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

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

  for (const bucket of ['profile-private', 'profile-public']) {
    const { data: files } = await admin.storage.from(bucket).list(userId, { limit: 1000 })
    if (files?.length) {
      await admin.storage.from(bucket).remove(files.map((file) => `${userId}/${file.name}`))
    }
  }

  const { error: deleteError } = await admin.auth.admin.deleteUser(userId)
  if (deleteError) {
    console.error('delete-account failed', deleteError.message)
    return new Response('Account deletion failed', { status: 500 })
  }
  return new Response(null, { status: 204 })
})
