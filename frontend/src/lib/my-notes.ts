/**
 * Local registry of notes this browser created: slug -> edit key + display
 * metadata. This is what makes /n/{slug} open in edit mode and what the
 * sidebar lists. Clearing site data loses the keys (the notes survive on the
 * server); account sync can back this up later.
 */

import { ApiError, getNote, updateNote } from '@/lib/api'

export interface MyNote {
  slug: string
  editKey: string
  title: string
  createdAt: string
  expiresAt: string | null
  keyBannerDismissed?: boolean
}

const STORAGE_KEY = 'my-notes'

/** Key holding the unsaved scratchpad draft (home page and /n workspace). */
export const DRAFT_STORAGE_KEY = 'simple-editor-content'

function load(): Record<string, MyNote> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function save(notes: Record<string, MyNote>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(notes))
  } catch {
    // storage full or unavailable; the note still exists server-side
  }
}

export function addMyNote(note: MyNote): void {
  const notes = load()
  notes[note.slug] = note
  save(notes)
}

export function getMyNote(slug: string): MyNote | null {
  return load()[slug] ?? null
}

export function listMyNotes(): MyNote[] {
  return Object.values(load()).sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  )
}

export function renameMyNote(slug: string, title: string): void {
  const notes = load()
  if (notes[slug]) {
    notes[slug].title = title
    save(notes)
  }
}

export function removeMyNote(slug: string): void {
  const notes = load()
  delete notes[slug]
  save(notes)
}

export function dismissKeyBanner(slug: string): void {
  const notes = load()
  if (notes[slug]) {
    notes[slug].keyBannerDismissed = true
    save(notes)
  }
}

/**
 * Validates an edit key against the server and, if valid, records the note
 * locally so this browser can edit it. Used by the "Edit Note" dialog and by
 * the #key redemption on the viewer.
 *
 * Validation strategy: fetch with the key (a correct key gives owner access
 * and returns the real title even for protected notes; a wrong key on a
 * protected note gets the hidden title), then a no-op title update, which the
 * backend rejects with 403 when the key is wrong.
 *
 * @throws ApiError with status 403 when the key is invalid
 */
export async function adoptNote(slug: string, editKey: string): Promise<MyNote> {
  const probe = await getNote(slug, editKey)
  if (probe.title == null) {
    // Protected note whose title stayed hidden — the key must be wrong
    throw new ApiError('Invalid edit key', 403)
  }
  const updated = await updateNote(slug, editKey, { title: probe.title })
  const note: MyNote = {
    slug,
    editKey,
    title: updated.title,
    createdAt: updated.createdAt,
    expiresAt: updated.expiresAt,
    keyBannerDismissed: true,
  }
  addMyNote(note)
  return note
}
