import { useEffect, useState } from "react"
import { LogIn, LogOut, Sparkles } from "lucide-react"
import { toast } from "sonner"

import { getMe } from "@/lib/api"
import {
  getToken,
  onSignInRequest,
  signOut,
  updateStoredUser,
  useAuth,
} from "@/lib/auth"
import { SignInDialog } from "@/components/SignInDialog"
import { UserAvatar } from "@/components/UserAvatar"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

/**
 * Header auth control: a project-styled "Sign In" button (opens the shared
 * sign-in dialog) when signed out, an account menu when signed in. Also hosts
 * the dialog itself, so any island can open it via requestSignIn().
 *
 * The visible trigger is desktop-only; on mobile the same actions live in the
 * hamburger menu (which calls requestSignIn() / signOut()). The dialog stays
 * mounted at all breakpoints so those requests still open it.
 */
export function AuthButton() {
  const { user, signedIn } = useAuth()
  const [signInOpen, setSignInOpen] = useState(false)

  useEffect(() => onSignInRequest(() => setSignInOpen(true)), [])

  // Returning from a Polar checkout: the webhook flips premium server-side,
  // so refresh the stored profile and let the user know.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get("checkout") !== "success") return
    params.delete("checkout")
    const query = params.toString()
    window.history.replaceState(
      null,
      "",
      window.location.pathname + (query ? `?${query}` : "")
    )
    const token = getToken()
    if (!token) return
    getMe(token)
      .then((me) => {
        updateStoredUser({ premium: me.premium })
        if (me.premium) {
          toast.success("Premium unlocked — you can now add images to notes!")
        }
      })
      .catch(() => {
        // The webhook may still be in flight; premium will show on next sign-in.
      })
  }, [])

  // Close the dialog automatically once sign-in completes.
  useEffect(() => {
    if (signedIn) setSignInOpen(false)
  }, [signedIn])

  const handleSignOut = () => {
    signOut()
    toast.success("Signed out")
  }

  return (
    <>
      <div className="hidden sm:flex sm:items-center">
        {signedIn && user ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="size-8 rounded-full"
                aria-label="Account menu"
              >
                <UserAvatar user={user} />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel className="font-normal">
                {user.name && (
                  <div className="text-sm font-medium">{user.name}</div>
                )}
                <div className="text-muted-foreground max-w-52 truncate text-xs">
                  {user.email}
                </div>
                {user.premium && (
                  <div className="mt-1 flex items-center gap-1 text-xs font-medium text-amber-600 dark:text-amber-400">
                    <Sparkles className="size-3" />
                    Premium
                  </div>
                )}
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem onSelect={handleSignOut}>
                <LogOut />
                Sign out
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <Button
            variant="ghost"
            size="sm"
            aria-label="Sign in"
            onClick={() => setSignInOpen(true)}
          >
            <LogIn />
            Sign In
          </Button>
        )}
      </div>

      <SignInDialog open={signInOpen} onOpenChange={setSignInOpen} />
    </>
  )
}

export default AuthButton
