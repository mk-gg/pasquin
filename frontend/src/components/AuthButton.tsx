import { useEffect, useRef } from "react"
import { LogOut } from "lucide-react"
import { toast } from "sonner"

import { signInWithGoogle } from "@/lib/api"
import { clearAuth, GOOGLE_CLIENT_ID, setAuth, useAuth } from "@/lib/auth"
import { syncOnSignIn } from "@/lib/my-notes"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

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

export function AuthButton() {
  const { user, signedIn } = useAuth()
  const buttonRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (signedIn) return
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
            } catch {
              toast.error("Sign-in failed. Please try again.")
            }
          },
        })
        buttonRef.current.innerHTML = ""
        window.google.accounts.id.renderButton(buttonRef.current, {
          type: "standard",
          theme: "outline",
          size: "medium",
          text: "signin",
          shape: "pill",
        })
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [signedIn])

  const handleSignOut = () => {
    try {
      window.google?.accounts?.id?.disableAutoSelect()
    } catch {
      // ignore
    }
    clearAuth()
    toast.success("Signed out")
  }

  if (signedIn && user) {
    const initial = (user.name || user.email).charAt(0).toUpperCase()
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="size-8 rounded-full"
            aria-label="Account menu"
          >
            <span className="bg-primary text-primary-foreground flex size-7 items-center justify-center rounded-full text-xs font-medium">
              {initial}
            </span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuLabel className="font-normal">
            {user.name && <div className="text-sm font-medium">{user.name}</div>}
            <div className="text-muted-foreground max-w-52 truncate text-xs">
              {user.email}
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem onSelect={handleSignOut}>
            <LogOut />
            Sign out
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    )
  }

  // Google renders its own button into this element.
  return <div ref={buttonRef} className="flex items-center" />
}

export default AuthButton
