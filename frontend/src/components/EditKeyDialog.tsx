import { useState } from "react"
import { Info, KeyRound } from "lucide-react"

import { ApiError } from "@/lib/api"
import { adoptNote } from "@/lib/my-notes"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

/** Lets a reader enter their edit key to unlock editing on this device. */
export function EditKeyDialog({
  slug,
  open,
  onOpenChange,
}: {
  slug: string
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [editKey, setEditKey] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleOpenChange = (nextOpen: boolean) => {
    onOpenChange(nextOpen)
    if (!nextOpen) {
      setEditKey("")
      setError(null)
    }
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const key = editKey.trim()
    if (!key) return
    setSubmitting(true)
    setError(null)
    try {
      await adoptNote(slug, key)
      window.location.assign(`/n?id=${slug}`)
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 403
          ? "That edit key doesn't match this note."
          : "Something went wrong. Please try again."
      )
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Edit Note</DialogTitle>
          <DialogDescription>
            Please enter the Edit Key to access this note. You can find it under
            Info (the info icon) on the device you originally created the note
            on.
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Alert className="border-blue-500/50 bg-blue-500/10 text-blue-700 dark:text-blue-400 [&>svg]:text-current *:data-[slot=alert-description]:text-blue-700/90 dark:*:data-[slot=alert-description]:text-blue-400/90">
            <Info />
            <AlertDescription>
              <p>
                Alternatively, you can edit your notes from any device with a
                free account.
              </p>
            </AlertDescription>
          </Alert>

          <div className="flex flex-col gap-2">
            <Label htmlFor="edit-key">Edit Key</Label>
            <div className="relative">
              <KeyRound className="text-muted-foreground absolute top-1/2 left-2.5 size-4 -translate-y-1/2" />
              <Input
                id="edit-key"
                className="pl-8 font-mono"
                placeholder="Your edit key"
                autoComplete="off"
                autoFocus
                value={editKey}
                onChange={(event) => setEditKey(event.target.value)}
              />
            </div>
            {error && <p className="text-destructive text-xs">{error}</p>}
          </div>

          <Button type="submit" disabled={submitting || editKey.trim() === ""}>
            {submitting ? "Verifying..." : "Edit Note"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default EditKeyDialog
