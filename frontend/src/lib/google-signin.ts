/**
 * Shared Google sign-in trigger. Lets any custom button (header, sidebar,
 * note-not-found) start the Google flow via `google.accounts.id.prompt()`,
 * instead of relying on Google's own rendered button.
 */

import { toast } from 'sonner'

import { signInWithGoogle } from '@/lib/api'
import { GOOGLE_CLIENT_ID, setAuth } from '@/lib/auth'
import { syncOnSignIn } from '@/lib/my-notes'

declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    google?: any
    __gisLoading?: Promise<void>
  }
}

const GSI_SRC = 'https://accounts.google.com/gsi/client'

function loadGis(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (window.__gisLoading) return window.__gisLoading
  window.__gisLoading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = GSI_SRC
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load Google Identity Services'))
    document.head.appendChild(script)
  })
  return window.__gisLoading
}

let initialized = false

async function ensureInitialized(): Promise<void> {
  await loadGis()
  if (initialized || !window.google) return
  window.google.accounts.id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: async (response: { credential: string }) => {
      try {
        const result = await signInWithGoogle(response.credential)
        setAuth(result.token, { email: result.email, name: result.name })
        await syncOnSignIn()
        toast.success(`Signed in as ${result.email}`)
      } catch {
        toast.error('Sign-in failed. Please try again.')
      }
    },
  })
  initialized = true
}

/** Opens the Google sign-in prompt. Safe to call from any component. */
export async function promptSignIn(): Promise<void> {
  try {
    await ensureInitialized()
    window.google.accounts.id.prompt()
  } catch {
    toast.error('Could not start sign-in. Please try again.')
  }
}
