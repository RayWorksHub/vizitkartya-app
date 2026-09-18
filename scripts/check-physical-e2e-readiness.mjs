#!/usr/bin/env node

import { readFile } from 'node:fs/promises'

const expectedVersion = process.argv[2] ?? '0.4.3'

const files = await Promise.all([
  readFile(new URL('../app/build.gradle.kts', import.meta.url), 'utf8'),
  readFile(new URL('../ios/VIZIT.xcodeproj/project.pbxproj', import.meta.url), 'utf8'),
  readFile(new URL('../.github/workflows/android.yml', import.meta.url), 'utf8'),
  readFile(new URL('../.github/workflows/ios.yml', import.meta.url), 'utf8'),
  readFile(new URL('../config/sharing.properties', import.meta.url), 'utf8'),
])

const [androidBuild, iosProject, androidWorkflow, iosWorkflow, sharing] = files

function capture(source, pattern, label) {
  const value = source.match(pattern)?.[1]?.trim()
  if (!value) throw new Error(`Nem található: ${label}`)
  return value
}

function assertEqual(actual, expected, label) {
  if (actual !== expected) throw new Error(`${label}: várt ${expected}, kapott ${actual}`)
}

const androidVersion = capture(androidBuild, /versionName\s*=\s*"([^"]+)"/, 'Android verzió')
const iosVersions = [...iosProject.matchAll(/MARKETING_VERSION\s*=\s*"?([^";]+)"?;/g)]
  .map((match) => match[1].trim())
const androidSupabase = capture(
  androidWorkflow,
  /ORG_GRADLE_PROJECT_VIZIT_DEV_SUPABASE_URL:\s*(https:\/\/[^\s]+)/,
  'Android DEV Supabase URL',
)
const iosSupabase = capture(
  iosWorkflow,
  /VIZIT_SUPABASE_URL:\s*(https:\/\/[^\s]+)/,
  'iOS DEV Supabase URL',
)
const publicProfileBase = capture(
  sharing,
  /^publicProfileBaseUrl=(https:\/\/[^\s]+)$/m,
  'nyilvános profil alap URL',
)

assertEqual(androidVersion, expectedVersion, 'Android verzióeltérés')
if (iosVersions.length < 2 || iosVersions.some((version) => version !== expectedVersion)) {
  throw new Error(`iOS verzióeltérés: ${[...new Set(iosVersions)].join(', ') || 'hiányzik'}`)
}
assertEqual(iosSupabase, androidSupabase, 'Az iOS és Android DEV Supabase projektje eltér')

const profileURL = new URL(publicProfileBase)
if (profileURL.protocol !== 'https:' || profileURL.username || profileURL.password) {
  throw new Error('A nyilvános profil URL nem biztonságos HTTPS-cím')
}

console.log('Fizikai E2E előellenőrzés: PASS')
console.log(`Verzió: ${expectedVersion}`)
console.log(`Közös DEV Supabase: ${androidSupabase}`)
console.log(`Nyilvános profil: ${publicProfileBase}`)
