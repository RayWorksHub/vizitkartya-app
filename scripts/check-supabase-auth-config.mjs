#!/usr/bin/env node

import { readFileSync } from 'node:fs'

const publicConfig = Object.fromEntries(
  readFileSync(new URL('../config/android-node.properties', import.meta.url), 'utf8')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .map((line) => {
      const separator = line.indexOf('=')
      return [line.slice(0, separator).trim(), line.slice(separator + 1).trim()]
    }),
)
const supabaseUrl = process.env.ORG_GRADLE_PROJECT_VIZIT_DEV_SUPABASE_URL?.trim() || publicConfig.authUrl
const publishableKey = process.env.ORG_GRADLE_PROJECT_VIZIT_DEV_SUPABASE_KEY?.trim() || publicConfig.authPublishableKey
const googleClientId = process.env.ORG_GRADLE_PROJECT_VIZIT_DEV_GOOGLE_WEB_CLIENT_ID?.trim()
if (!supabaseUrl || !publishableKey) throw new Error('Missing DEV Supabase configuration')

const response = await fetch(new URL('/auth/v1/settings', supabaseUrl), {
  headers: { apikey: publishableKey },
})
if (!response.ok) throw new Error(`Supabase Auth settings check failed: HTTP ${response.status}`)

const settings = await response.json()
if (settings.disable_signup !== false) throw new Error('Supabase user signup is disabled')
if (settings.mailer_autoconfirm !== false) throw new Error('Supabase email confirmation is disabled')
if (settings.external?.email !== true) throw new Error('Supabase email/password provider is disabled')
const googleExpected = Boolean(googleClientId)
if (googleExpected && settings.external?.google !== true) {
  throw new Error('Google sign-in is configured in the app but disabled in Supabase')
}
if (!googleExpected && settings.external?.google === true) {
  throw new Error('Google sign-in is enabled in Supabase but hidden by the app build')
}

console.log('Supabase Auth configuration: PASS')
