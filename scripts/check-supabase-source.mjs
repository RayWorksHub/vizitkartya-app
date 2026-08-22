#!/usr/bin/env node

import { readFileSync } from 'node:fs'

const edgeFunction = readFileSync(
  new URL('../supabase/functions/delete-account/index.ts', import.meta.url),
  'utf8',
)
const withoutRemoteImport = edgeFunction.replace(/^import[^\n]+\n/, '')

// The Edge Function intentionally stays valid JavaScript inside its .ts entrypoint, so Node can
// provide a dependency-free parser check while Deno resolves the remote Supabase import at deploy.
new Function(withoutRemoteImport)

console.log('Supabase source syntax: PASS')
