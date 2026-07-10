/**
 * Client-side auth state. The session token + user profile live in localStorage
 * so every React island (which mount independently) can read them, and a custom
 * event notifies islands when auth changes.
 */

import { useEffect, useState } from 'react'

const TOKEN_KEY = 'auth-token'
const USER_KEY = 'auth-user'
const AUTH_EVENT = 'pasquin-auth-changed'

/** Public Google OAuth client id (safe to expose; set at build via env). */
export const GOOGLE_CLIENT_ID: string =
  import.meta.env.PUBLIC_GOOGLE_CLIENT_ID ??
  '371854982599-q0uu06bo4mvcq0cba2r5b42eso80jg9j.apps.googleusercontent.com'

export interface AuthUser {
  email: string
  name: string | null
}

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function getUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

export function isSignedIn(): boolean {
  return getToken() !== null
}

export function setAuth(token: string, user: AuthUser): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new Event(AUTH_EVENT))
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  window.dispatchEvent(new Event(AUTH_EVENT))
}

/**
 * Sign out: stop Google from auto-selecting the account on the next visit,
 * then clear local session state. Shared by the header menu and mobile menu.
 */
export function signOut(): void {
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ;(window as any).google?.accounts?.id?.disableAutoSelect()
  } catch {
    // ignore
  }
  clearAuth()
}

/** Subscribe to sign-in / sign-out; returns an unsubscribe function. */
export function onAuthChange(callback: () => void): () => void {
  window.addEventListener(AUTH_EVENT, callback)
  window.addEventListener('storage', callback) // sync across tabs
  return () => {
    window.removeEventListener(AUTH_EVENT, callback)
    window.removeEventListener('storage', callback)
  }
}

/** React hook exposing the current user; re-renders on sign-in / sign-out. */
export function useAuth(): { user: AuthUser | null; signedIn: boolean } {
  const [user, setUser] = useState<AuthUser | null>(null)
  useEffect(() => {
    setUser(getUser())
    return onAuthChange(() => setUser(getUser()))
  }, [])
  return { user, signedIn: user !== null }
}

// The sign-in dialog is hosted once (in the header island); any other island
// can open it by dispatching this event.
const SIGNIN_REQUEST_EVENT = 'pasquin-signin-request'

/** Opens the sign-in dialog from anywhere in the app. */
export function requestSignIn(): void {
  window.dispatchEvent(new Event(SIGNIN_REQUEST_EVENT))
}

/** Subscribe to sign-in requests; returns an unsubscribe function. */
export function onSignInRequest(callback: () => void): () => void {
  window.addEventListener(SIGNIN_REQUEST_EVENT, callback)
  return () => window.removeEventListener(SIGNIN_REQUEST_EVENT, callback)
}
