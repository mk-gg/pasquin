import { useEffect, useRef } from "react"
import { toast } from "sonner"

import { signInWithGoogle } from "@/lib/api"
import { GOOGLE_CLIENT_ID, setAuth } from "@/lib/auth"
import { syncOnSignIn } from "@/lib/my-notes"

// Minimal Google Identity Services typings.
declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    google?: any
    __gisLoading?: Promise<void>
  }
}

const GSI_SRC = "https://accounts.google.com/gsi/client"

function loadGis(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve()
  if (window.__gisLoading) return window.__gisLoading
  window.__gisLoading = new Promise((resolve, reject) => {
    const script = document.createElement("script")
    script.src = GSI_SRC
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error("Failed to load Google Identity Services"))
    document.head.appendChild(script)
  })
  return window.__gisLoading
}

function GoogleGlyph() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1Z"
      />
      <path
        fill="#34A853"
        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z"
      />
      <path
        fill="#FBBC05"
        d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.18a11 11 0 0 0 0 9.88l3.66-2.84Z"
      />
      <path
        fill="#EA4335"
        d="M12 4.75c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 1.46 14.97.5 12 .5A11 11 0 0 0 2.18 7.06l3.66 2.84C6.71 6.68 9.14 4.75 12 4.75Z"
      />
    </svg>
  )
}

/**
 * Google sign-in control themed to the project. GIS only mints ID tokens
 * through its own rendered button, so that button is layered transparently
 * over a styled button to capture the click while keeping our look.
 */
export function GoogleSignInButton({ onSuccess }: { onSuccess?: () => void }) {
  const wrapRef = useRef<HTMLDivElement>(null)
  const overlayRef = useRef<HTMLDivElement>(null)
  const onSuccessRef = useRef(onSuccess)
  onSuccessRef.current = onSuccess

  useEffect(() => {
    let cancelled = false
    loadGis()
      .then(() => {
        if (cancelled || !window.google || !overlayRef.current) return
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: async (response: { credential: string }) => {
            try {
              const result = await signInWithGoogle(response.credential)
              setAuth(result.token, {
                email: result.email,
                name: result.name,
                picture: result.picture,
                premium: result.premium,
              })
              await syncOnSignIn()
              toast.success(`Signed in as ${result.email}`)
              onSuccessRef.current?.()
            } catch {
              toast.error("Sign-in failed. Please try again.")
            }
          },
        })
        const width = Math.min(Math.max(wrapRef.current?.clientWidth ?? 320, 200), 400)
        overlayRef.current.innerHTML = ""
        window.google.accounts.id.renderButton(overlayRef.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          text: "continue_with",
          shape: "pill",
          width,
        })
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div ref={wrapRef} className="relative h-11 w-full">
      {/* Themed button the user sees. */}
      <div
        aria-hidden="true"
        className="border-input bg-background text-foreground hover:bg-accent pointer-events-none absolute inset-0 flex items-center justify-center gap-2.5 rounded-full border text-sm font-medium shadow-xs"
      >
        <GoogleGlyph />
        Continue with Google
      </div>
      {/* Real GIS button: transparent, on top, catches the click. */}
      <div
        ref={overlayRef}
        className="absolute inset-0 z-10 overflow-hidden opacity-0"
      />
    </div>
  )
}

export default GoogleSignInButton
