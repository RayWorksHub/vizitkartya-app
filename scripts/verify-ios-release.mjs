#!/usr/bin/env node

import { createHash } from 'node:crypto'
import { existsSync, readdirSync, readFileSync, realpathSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDirectory = dirname(fileURLToPath(import.meta.url))
const repository = realpathSync(resolve(scriptDirectory, '..'))
const manifestPath = join(repository, 'ios/Release/release-manifest.json')
const uiTestPath = join(repository, 'ios/UITests/VizitUITests.swift')
const workflowPath = join(repository, '.github/workflows/ios.yml')

function fail(message) {
  throw new Error(`iOS release gate: ${message}`)
}

function readJSON(path) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'))
  } catch (error) {
    fail(`cannot read JSON ${relative(repository, path)} (${error.message})`)
  }
}

function requireString(value, label) {
  if (typeof value !== 'string' || !value.trim()) fail(`${label} must be a non-empty string`)
  return value.trim()
}

function requireUnique(values, label) {
  const duplicate = values.find((value, index) => values.indexOf(value) !== index)
  if (duplicate !== undefined) fail(`${label} contains duplicate ${duplicate}`)
}

function repositoryFile(path, label) {
  const absolute = resolve(repository, requireString(path, label))
  if (!existsSync(absolute) || !statSync(absolute).isFile()) fail(`${label} is missing: ${path}`)
  const canonical = realpathSync(absolute)
  if (relative(repository, canonical).startsWith('..')) fail(`${label} escapes the repository: ${path}`)
  return canonical
}

function existingFile(path, label) {
  const absolute = resolve(path)
  if (!existsSync(absolute) || !statSync(absolute).isFile()) fail(`${label} is missing: ${path}`)
  return realpathSync(absolute)
}

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex')
}

function swiftTestCount(directory) {
  return readdirSync(directory, { withFileTypes: true }).reduce((total, entry) => {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) return total + swiftTestCount(path)
    if (!entry.isFile() || !entry.name.endsWith('.swift')) return total
    const source = readFileSync(path, 'utf8')
    return total + [...source.matchAll(/\bfunc\s+(test[A-Za-z0-9_]+)\s*\(/g)].length
  }, 0)
}

function validateSource() {
  const manifest = readJSON(manifestPath)
  if (manifest.schemaVersion !== 1) fail(`unsupported manifest schema ${manifest.schemaVersion}`)
  requireString(manifest.releaseContract, 'releaseContract')
  requireString(manifest.figmaFileKey, 'figmaFileKey')

  if (!Array.isArray(manifest.boards) || manifest.boards.length !== 8) {
    fail(`exactly 8 pinned iOS Figma boards are required, found ${manifest.boards?.length ?? 0}`)
  }
  const boardIDs = manifest.boards.map((board, index) => requireString(board.id, `boards[${index}].id`))
  requireUnique(boardIDs, 'board ids')
  for (const [index, board] of manifest.boards.entries()) {
    const file = repositoryFile(board.path, `boards[${index}].path`)
    const expected = requireString(board.sha256, `boards[${index}].sha256`).toLowerCase()
    if (!/^[a-f0-9]{64}$/.test(expected)) fail(`invalid SHA-256 for ${board.id}`)
    const actual = sha256(file)
    if (actual !== expected) fail(`${board.id} changed: expected ${expected}, got ${actual}`)
  }

  if (!Array.isArray(manifest.uiEvidence) || manifest.uiEvidence.length < 18) {
    fail('at least 18 named UI evidence captures are required')
  }
  const evidenceIDs = manifest.uiEvidence.map((item, index) => requireString(item.id, `uiEvidence[${index}].id`))
  const attachmentNames = manifest.uiEvidence.map((item, index) => requireString(item.attachment, `uiEvidence[${index}].attachment`))
  requireUnique(evidenceIDs, 'UI evidence ids')
  requireUnique(attachmentNames, 'UI attachment names')

  const uiSource = readFileSync(uiTestPath, 'utf8')
  const coveredBoards = new Set()
  for (const [index, item] of manifest.uiEvidence.entries()) {
    const method = requireString(item.testMethod, `uiEvidence[${index}].testMethod`)
    if (!uiSource.includes(`func ${method}(`)) fail(`${item.id} references missing UI test ${method}`)
    if (!uiSource.includes(`"${item.attachment}"`)) fail(`${item.id} attachment is not emitted by the UI test`)
    if (!Array.isArray(item.boardIds) || item.boardIds.length === 0) fail(`${item.id} has no Figma board mapping`)
    for (const boardID of item.boardIds) {
      if (!boardIDs.includes(boardID)) fail(`${item.id} references unknown board ${boardID}`)
      coveredBoards.add(boardID)
    }
  }
  const uncovered = boardIDs.filter((id) => !coveredBoards.has(id))
  if (uncovered.length) fail(`boards without UI evidence: ${uncovered.join(', ')}`)

  if (!Array.isArray(manifest.approvedPlatformConstraints) || manifest.approvedPlatformConstraints.length < 2) {
    fail('iOS platform/product constraints must be explicit')
  }
  for (const [index, constraint] of manifest.approvedPlatformConstraints.entries()) {
    requireString(constraint.id, `approvedPlatformConstraints[${index}].id`)
    requireString(constraint.reason, `approvedPlatformConstraints[${index}].reason`)
    repositoryFile(constraint.source, `approvedPlatformConstraints[${index}].source`)
  }
  if (!Array.isArray(manifest.physicalDeviceChecks) || manifest.physicalDeviceChecks.length < 9) {
    fail('the complete physical-device checklist is required')
  }
  requireUnique(manifest.physicalDeviceChecks, 'physical-device checks')

  const workflow = readFileSync(workflowPath, 'utf8')
  for (const forbidden of ['VIZIT_PHYSICAL_DEVICE_RELEASE_APPROVED', "contains(github.event.head_commit.message, '[testflight]')"]) {
    if (workflow.includes(forbidden)) fail(`unsafe legacy release trigger remains in ios.yml: ${forbidden}`)
  }
  for (const required of ['environment: testflight-production', 'release_commit', 'physical_device_commit', 'design_review_commit']) {
    if (!workflow.includes(required)) fail(`ios.yml is missing release control: ${required}`)
  }

  const nativeTests = swiftTestCount(join(repository, 'ios/NativeTests'))
  const uiTests = swiftTestCount(join(repository, 'ios/UITests'))
  return { manifest, nativeTests, uiTests, expectedXcodeTests: nativeTests + uiTests }
}

function argument(name) {
  const index = process.argv.indexOf(name)
  if (index === -1 || index + 1 >= process.argv.length) fail(`missing ${name}`)
  return process.argv[index + 1]
}

function validateEvidence(source) {
  const summaryPath = existingFile(argument('--summary'), '--summary')
  const resultJSONPath = existingFile(argument('--xcresult-json'), '--xcresult-json')
  const attachmentsDirectory = resolve(argument('--attachments'))
  if (!existsSync(attachmentsDirectory) || !statSync(attachmentsDirectory).isDirectory()) {
    fail(`attachments directory is missing: ${attachmentsDirectory}`)
  }

  const summary = readJSON(summaryPath)
  const passed = Number(summary.passedTests)
  const failed = Number(summary.failedTests)
  const skipped = Number(summary.skippedTests ?? 0)
  if (passed !== source.expectedXcodeTests || failed !== 0 || skipped !== 0) {
    fail(`invalid Xcode result: expected ${source.expectedXcodeTests} passed; got ${passed} passed, ${failed} failed, ${skipped} skipped`)
  }

  const resultText = readFileSync(resultJSONPath, 'utf8')
  const missingNames = source.manifest.uiEvidence
    .map((item) => item.attachment)
    .filter((name) => !resultText.includes(name))
  if (missingNames.length) fail(`xcresult is missing named evidence: ${missingNames.join(', ')}`)

  const pngs = readdirSync(attachmentsDirectory, { recursive: true })
    .filter((path) => path.toLowerCase().endsWith('.png'))
  if (pngs.length < source.manifest.uiEvidence.length) {
    fail(`expected at least ${source.manifest.uiEvidence.length} screenshots, found ${pngs.length}`)
  }
  return { passed, screenshots: pngs.length }
}

const mode = process.argv[2] ?? 'source'
const source = validateSource()
if (mode === 'source') {
  console.log(`iOS release source gate: PASS (${source.manifest.boards.length} boards, ${source.manifest.uiEvidence.length} UI captures, ${source.expectedXcodeTests} Xcode tests)`)
} else if (mode === 'evidence') {
  const evidence = validateEvidence(source)
  console.log(`iOS release evidence gate: PASS (${evidence.passed} tests, ${evidence.screenshots} screenshots)`)
} else {
  fail(`unknown mode ${mode}; expected source or evidence`)
}
