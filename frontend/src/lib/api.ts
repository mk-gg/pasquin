/**
 * Client for the notes backend API.
 * Set PUBLIC_API_URL in .env to point at a deployed backend.
 */

const API_BASE: string = import.meta.env.PUBLIC_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export interface CreateNotePayload {
  content: unknown
  title: string | null
  autoExpire: string | null
  password: string | null
}

export interface CreateNoteResult {
  slug: string
  title: string
  expiresAt: string | null
  passwordProtected: boolean
  /** Returned exactly once; the server keeps only a hash. */
  editKey: string
}

export interface NoteResult {
  slug: string
  title: string
  content?: unknown
  passwordProtected: boolean
  createdAt: string
  updatedAt: string
  expiresAt: string | null
}

export interface UpdateNotePayload {
  content?: unknown
  title?: string
  password?: string
  autoExpire?: string
  removeExpiry?: boolean
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    })
  } catch {
    throw new ApiError('Could not reach the server. Please try again.', 0)
  }
  if (!response.ok) {
    // The backend returns RFC 9457 problem details with a human-readable `detail`
    let detail: string | undefined
    try {
      detail = (await response.json()).detail
    } catch {
      // fall through to the generic message
    }
    throw new ApiError(detail ?? `Request failed (${response.status})`, response.status)
  }
  // 202/204 (and any empty body) carry no JSON to parse
  if (response.status === 204 || response.status === 202) {
    return undefined as T
  }
  return response.json() as Promise<T>
}

export function createNote(payload: CreateNotePayload): Promise<CreateNoteResult> {
  return request('/api/notes', { method: 'POST', body: JSON.stringify(payload) })
}

export function getNote(slug: string, editKey?: string): Promise<NoteResult> {
  return request(`/api/notes/${encodeURIComponent(slug)}`, {
    headers: editKey
      ? { 'Content-Type': 'application/json', 'X-Edit-Key': editKey }
      : undefined,
  })
}

export function updateNote(
  slug: string,
  editKey: string,
  payload: UpdateNotePayload
): Promise<NoteResult> {
  return request(`/api/notes/${encodeURIComponent(slug)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'X-Edit-Key': editKey },
    body: JSON.stringify(payload),
  })
}

export function deleteNote(slug: string, editKey: string): Promise<void> {
  return request(`/api/notes/${encodeURIComponent(slug)}`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', 'X-Edit-Key': editKey },
  })
}

export function unlockNote(slug: string, password: string): Promise<NoteResult> {
  return request(`/api/notes/${encodeURIComponent(slug)}/unlock`, {
    method: 'POST',
    body: JSON.stringify({ password }),
  })
}

export interface ContactPayload {
  name: string
  email: string
  reason: string
  message: string
  website?: string
}

export interface ReportPayload {
  type: string
  email: string
  details?: string
  links?: string
  noteSlug?: string
  website?: string
}

export function submitContact(payload: ContactPayload): Promise<void> {
  return request('/api/contact', { method: 'POST', body: JSON.stringify(payload) })
}

export function submitReport(payload: ReportPayload): Promise<void> {
  return request('/api/reports', { method: 'POST', body: JSON.stringify(payload) })
}

// --- Auth & account note sync ---

export interface AuthResult {
  token: string
  email: string
  name: string | null
}

/** A note owned by the signed-in account (server-synced form of a local note). */
export interface AccountNote {
  slug: string
  editKey: string
  title: string
  createdAt: string
  expiresAt: string | null
}

export function signInWithGoogle(idToken: string): Promise<AuthResult> {
  return request('/api/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken }),
  })
}

function authed(token: string): RequestInit {
  return { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
}

export function getAccountNotes(token: string): Promise<AccountNote[]> {
  return request('/api/me/notes', authed(token))
}

export function putAccountNote(token: string, note: AccountNote): Promise<void> {
  return request('/api/me/notes', {
    method: 'PUT',
    ...authed(token),
    body: JSON.stringify(note),
  })
}

export function deleteAccountNote(token: string, slug: string): Promise<void> {
  return request(`/api/me/notes/${encodeURIComponent(slug)}`, {
    method: 'DELETE',
    ...authed(token),
  })
}

export function mergeAccountNotes(
  token: string,
  notes: AccountNote[]
): Promise<AccountNote[]> {
  return request('/api/me/notes/merge', {
    method: 'POST',
    ...authed(token),
    body: JSON.stringify(notes),
  })
}
