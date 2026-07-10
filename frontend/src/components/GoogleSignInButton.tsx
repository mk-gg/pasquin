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

/**
 * The official Google Identity Services button. GIS only issues ID tokens via
 * its own rendered button (or One Tap), so custom-styled triggers open a
 * dialog containing this instead of launching the popup directly.
 */
export function GoogleSignInButton({ onSuccess }: { onSuccess?: () => void }) {
  const buttonRef = useRef<HTMLDivElement>(null)
  const onSuccessRef = useRef(onSuccess)
  onSuccessRef.current = onSuccess

  useEffect(() => {
    let cancelled = false
    loadGis()
      .then(() => {
        if (cancelled || !window.google || !buttonRef.current) return
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: async (response: { credential: string }) => {
            try {
              const result = await signInWithGoogle(response.credential)
              setAuth(result.token, { email: result.email, name: result.name })
              await syncOnSignIn()
              toast.success(`Signed in as ${result.email}`)
              onSuccessRef.current?.()
            } catch {
              toast.error("Sign-in failed. Please try again.")
            }
          },
        })
        buttonRef.current.innerHTML = ""
        window.google.accounts.id.renderButton(buttonRef.current, {
          type: "standard",
          theme: "outline",
          size: "large",
          text: "signin_with",
          shape: "pill",
        })
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div ref={buttonRef} className="flex min-h-11 items-center justify-center" />
  )
}

export default GoogleSignInButton
