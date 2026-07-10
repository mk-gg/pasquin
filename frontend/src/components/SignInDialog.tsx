import { SITE } from "@/consts"
import { GoogleSignInButton } from "@/components/GoogleSignInButton"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

/** Shared sign-in dialog; every "Sign In" trigger in the app opens this. */
export function SignInDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Sign in to {SITE.title}</DialogTitle>
          <DialogDescription>
            Keep your notes safe and edit them from any device with a free
            account.
          </DialogDescription>
        </DialogHeader>

        <div className="py-2">
          <GoogleSignInButton onSuccess={() => onOpenChange(false)} />
        </div>

        <p className="text-muted-foreground text-center text-xs">
          We only use your Google account to identify you. Your notes stay
          yours.
        </p>
      </DialogContent>
    </Dialog>
  )
}

export default SignInDialog
