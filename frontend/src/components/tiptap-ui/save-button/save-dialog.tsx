import { useState } from "react"
import { type Editor } from "@tiptap/react"
import { Info, TriangleAlert } from "lucide-react"
import { toast } from "sonner"

import { SITE } from "@/consts"
import { ApiError, createNote } from "@/lib/api"
import { addMyNote, DRAFT_STORAGE_KEY } from "@/lib/my-notes"
import { EXPIRY_OPTIONS } from "@/lib/expiry"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Switch } from "@/components/ui/switch"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

const MIN_PASSWORD_LENGTH = 4
const MAX_TITLE_LENGTH = 60

/** Derives a display title from the first non-empty line of the document. */
function deriveTitle(editor: Editor): string {
  const firstBlockText = editor.state.doc.firstChild?.textContent.trim() ?? ""
  const text = firstBlockText || editor.state.doc.textContent.trim()
  return text.slice(0, MAX_TITLE_LENGTH)
}

export function SaveNoteDialog({
  editor,
  children,
}: {
  editor: Editor
  children: React.ReactNode
}) {
  const [open, setOpen] = useState(false)
  const [autoExpire, setAutoExpire] = useState(false)
  const [expiresIn, setExpiresIn] = useState("1 Day")
  const [passwordProtect, setPasswordProtect] = useState(false)
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const passwordsMismatch =
    passwordProtect && confirmPassword.length > 0 && password !== confirmPassword
  const passwordTooShort =
    passwordProtect && password.length > 0 && password.length < MIN_PASSWORD_LENGTH
  const saveDisabled =
    saving ||
    (passwordProtect &&
      (password.length < MIN_PASSWORD_LENGTH || password !== confirmPassword))

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen)
    if (!nextOpen) {
      setPassword("")
      setConfirmPassword("")
      setError(null)
    }
  }

  const handleSave = async () => {
    setSaving(true)
    setError(null)
    try {
      const created = await createNote({
        content: editor.getJSON(),
        title: deriveTitle(editor) || null,
        autoExpire: autoExpire ? expiresIn : null,
        password: passwordProtect ? password : null,
      })
      // Remember ownership locally so /n/{slug} opens in edit mode,
      // and clear the scratchpad draft the note was created from
      addMyNote({
        slug: created.slug,
        editKey: created.editKey,
        title: created.title,
        createdAt: new Date().toISOString(),
        expiresAt: created.expiresAt,
      })
      // Clear the editor before removing the stored draft: the editor's
      // beforeunload flush re-saves the current doc during the redirect,
      // so an unclear editor would resurrect the draft we just removed.
      editor.commands.clearContent()
      try {
        localStorage.removeItem(DRAFT_STORAGE_KEY)
      } catch {
        // non-fatal
      }
      toast.success("Note saved!")
      window.location.assign(`/n?id=${created.slug}`)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Something went wrong. Please try again.")
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <>
          <DialogHeader>
              <DialogTitle>Save note</DialogTitle>
              <DialogDescription>
                When you save your note, it&apos;s securely stored with
                encryption, added to &ldquo;My Notes&rdquo;, and a shareable
                link is created &mdash; instantly!
              </DialogDescription>
            </DialogHeader>

            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <TooltipProvider delayDuration={200}>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Label
                        htmlFor="save-auto-expire"
                        className="cursor-pointer"
                      >
                        Auto expire
                        <Info className="text-muted-foreground size-3.5" />
                      </Label>
                    </TooltipTrigger>
                    <TooltipContent>
                      After this time, the note will be deleted
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
                <Switch
                  id="save-auto-expire"
                  checked={autoExpire}
                  onCheckedChange={setAutoExpire}
                />
              </div>

              {autoExpire && (
                <Select value={expiresIn} onValueChange={setExpiresIn}>
                  <SelectTrigger className="w-full" aria-label="Expires in">
                    <SelectValue placeholder="Expires in" />
                  </SelectTrigger>
                  <SelectContent>
                    {EXPIRY_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}

              <div className="flex items-center justify-between">
                <Label
                  htmlFor="save-password-protect"
                  className="cursor-pointer"
                >
                  Password protect
                </Label>
                <Switch
                  id="save-password-protect"
                  checked={passwordProtect}
                  onCheckedChange={setPasswordProtect}
                />
              </div>

              {passwordProtect && (
                <div className="flex flex-col gap-3">
                  <Alert className="bg-amber-50 dark:bg-amber-950">
                    <TriangleAlert />
                    <AlertTitle>Important!</AlertTitle>
                    <AlertDescription>
                      <p>
                        <span className="capitalize">{SITE.title}</span> will
                        not be able to recover this password, so please keep it
                        safe.
                      </p>
                    </AlertDescription>
                  </Alert>
                  <Input
                    type="password"
                    placeholder="Password"
                    autoComplete="new-password"
                    aria-invalid={passwordTooShort || undefined}
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                  <Input
                    type="password"
                    placeholder="Confirm password"
                    autoComplete="new-password"
                    aria-invalid={passwordsMismatch || undefined}
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                  />
                  {passwordTooShort && (
                    <p className="text-destructive text-xs">
                      Password must be at least {MIN_PASSWORD_LENGTH} characters
                    </p>
                  )}
                  {passwordsMismatch && (
                    <p className="text-destructive text-xs">
                      Passwords do not match
                    </p>
                  )}
                </div>
              )}

              {error && <p className="text-destructive text-sm">{error}</p>}

              <Button
                type="button"
                onClick={handleSave}
                disabled={saveDisabled}
                className="mt-1 w-full"
              >
                {saving ? "Saving..." : "Save Note"}
              </Button>
            </div>
          </>
      </DialogContent>
    </Dialog>
  )
}

export default SaveNoteDialog
