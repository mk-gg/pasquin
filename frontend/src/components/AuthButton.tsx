import { useEffect, useState } from "react"
import { LogIn, LogOut } from "lucide-react"
import { toast } from "sonner"

import { onSignInRequest, signOut, useAuth } from "@/lib/auth"
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
