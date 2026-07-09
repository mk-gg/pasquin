import { useEffect, useState } from "react"
import QRCode from "qrcode"
import { Copy, EllipsisVertical, Mail, TriangleAlert } from "lucide-react"
import { toast } from "sonner"

import { ApiError, deleteNote, updateNote, type NoteResult } from "@/lib/api"
import { removeMyNote, type MyNote } from "@/lib/my-notes"
import { EXPIRY_OPTIONS } from "@/lib/expiry"
import { NoteInfoContent } from "@/components/NoteInfo"
import { Button as TiptapButton } from "@/components/tiptap-ui-primitive/button"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Switch } from "@/components/ui/switch"

function noteUrl(slug: string): string {
  return `${window.location.origin}/n/${slug}`
}

const MIN_PASSWORD_LENGTH = 4

function ShareNoteDialog({
  open,
  onOpenChange,
  note,
  mine,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  note: NoteResult
  mine: MyNote
}) {
  const [allowEditing, setAllowEditing] = useState(false)
  const [qr, setQr] = useState<string | null>(null)

  const link = allowEditing
    ? `${noteUrl(mine.slug)}#${mine.editKey}`
    : noteUrl(mine.slug)

  useEffect(() => {
    if (!open) return
    QRCode.toDataURL(link, { margin: 1, width: 176 })
      .then(setQr)
      .catch(() => setQr(null))
  }, [link, open])

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(link)
      toast.success("Link copied to clipboard")
    } catch {
      // the input stays selectable as a fallback
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Share Note</DialogTitle>
          <DialogDescription className="sr-only">
            Share a link to this note
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <Label htmlFor="share-allow-editing" className="cursor-pointer">
              Allow Editing
            </Label>
            <Switch
              id="share-allow-editing"
              checked={allowEditing}
              onCheckedChange={setAllowEditing}
            />
          </div>

          <Input
            readOnly
            value={link}
            aria-label="Shareable link"
            onFocus={(event) => event.currentTarget.select()}
          />

          <div className="grid grid-cols-2 gap-2">
            <Button type="button" variant="outline" onClick={handleCopy}>
              <Copy />
              Copy
            </Button>
            <Button type="button" variant="outline" asChild>
              <a
                href={`mailto:?subject=${encodeURIComponent(note.title)}&body=${encodeURIComponent(link)}`}
              >
                <Mail />
                Email
              </a>
            </Button>
          </div>

          {allowEditing ? (
            <p className="flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400">
              <TriangleAlert className="size-3.5 shrink-0" />
              Anyone you share this link with can view and edit this note.
            </p>
          ) : (
            <p className="text-muted-foreground text-xs">
              Anyone you share this link with can view this note.
            </p>
          )}

          <Separator />

          <div className="flex flex-col items-center gap-2">
            {qr && (
              <img
                src={qr}
                alt="QR code for the shareable link"
                className="rounded-md border"
                width={176}
                height={176}
              />
            )}
            <p className="text-muted-foreground text-xs">
              Scan to open on another device
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

function AutoExpireDialog({
  open,
  onOpenChange,
  note,
  mine,
  onNoteChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  note: NoteResult
  mine: MyNote
  onNoteChange: (note: NoteResult) => void
}) {
  const hasExpiry = note.expiresAt != null
  const [view, setView] = useState<"manage" | "set">("set")
  const [expiresIn, setExpiresIn] = useState("1 Day")
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (open) setView(hasExpiry ? "manage" : "set")
  }, [open, hasExpiry])

  const applyExpiry = async () => {
    setBusy(true)
    try {
      const updated = await updateNote(mine.slug, mine.editKey, {
        autoExpire: expiresIn,
      })
      onNoteChange(updated)
      toast.success("Expiry updated")
      onOpenChange(false)
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to set expiry")
    } finally {
      setBusy(false)
    }
  }

  const handleRemove = async () => {
    setBusy(true)
    try {
      const updated = await updateNote(mine.slug, mine.editKey, {
        removeExpiry: true,
      })
      onNoteChange(updated)
      toast.success("Expiry removed")
      onOpenChange(false)
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to remove expiry")
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Auto Expire</DialogTitle>
          <DialogDescription>
            {view === "manage"
              ? "Auto expire is currently enabled for this note."
              : "Set an expiry for this note. After this time, the note will be deleted."}
          </DialogDescription>
        </DialogHeader>

        {view === "manage" ? (
          <div className="grid grid-cols-2 gap-2">
            <Button variant="outline" onClick={() => setView("set")}>
              Change Expiry
            </Button>
            <Button
              variant="destructive"
              disabled={busy}
              onClick={handleRemove}
            >
              {busy ? "Removing..." : "Remove Expiry"}
            </Button>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
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
            <Button disabled={busy} onClick={applyExpiry}>
              {busy ? "Saving..." : "Set Expiry"}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

function InfoDialog({
  open,
  onOpenChange,
  note,
  mine,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  note: NoteResult
  mine: MyNote
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Info</DialogTitle>
          <DialogDescription className="sr-only">
            Details about this note
          </DialogDescription>
        </DialogHeader>
        <NoteInfoContent note={note} mine={mine} />
      </DialogContent>
    </Dialog>
  )
}

function PasswordDialog({
  open,
  onOpenChange,
  note,
  mine,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  note: NoteResult
  mine: MyNote
}) {
  const [password, setPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [busy, setBusy] = useState(false)

  const mismatch =
    confirmPassword.length > 0 && password !== confirmPassword
  const disabled =
    busy || password.length < MIN_PASSWORD_LENGTH || password !== confirmPassword

  const handleOpenChange = (next: boolean) => {
    onOpenChange(next)
    if (!next) {
      setPassword("")
      setConfirmPassword("")
    }
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setBusy(true)
    try {
      await updateNote(mine.slug, mine.editKey, { password })
      toast.success(
        note.passwordProtected ? "Password updated" : "Note is now password protected"
      )
      handleOpenChange(false)
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to set password")
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Password protect</DialogTitle>
          <DialogDescription>
            {note.passwordProtected
              ? "This note is already password protected. Setting a new password replaces the old one."
              : "Readers will need this password to view the note."}
          </DialogDescription>
        </DialogHeader>
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <Input
            type="password"
            placeholder="Password"
            autoComplete="new-password"
            autoFocus
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          <Input
            type="password"
            placeholder="Confirm password"
            autoComplete="new-password"
            aria-invalid={mismatch || undefined}
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
          />
          {mismatch && (
            <p className="text-destructive text-xs">Passwords do not match</p>
          )}
          {password.length > 0 && password.length < MIN_PASSWORD_LENGTH && (
            <p className="text-destructive text-xs">
              Password must be at least {MIN_PASSWORD_LENGTH} characters
            </p>
          )}
          <DialogFooter>
            <Button type="submit" disabled={disabled}>
              {busy ? "Saving..." : "Set password"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function DeleteDialog({
  open,
  onOpenChange,
  note,
  mine,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  note: NoteResult
  mine: MyNote
}) {
  const [busy, setBusy] = useState(false)

  const handleDelete = async () => {
    setBusy(true)
    try {
      await deleteNote(mine.slug, mine.editKey)
      removeMyNote(mine.slug)
      toast.success("Note deleted")
      window.location.assign("/n")
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to delete note")
      setBusy(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Delete note?</DialogTitle>
          <DialogDescription>
            &ldquo;{note.title}&rdquo; will be permanently deleted. This cannot
            be undone.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button variant="destructive" disabled={busy} onClick={handleDelete}>
            {busy ? "Deleting..." : "Delete"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** Share button + Actions dropdown for the note editor's toolbar action area. */
export function NoteOwnerControls({
  note,
  mine,
  onNoteChange,
}: {
  note: NoteResult
  mine: MyNote
  onNoteChange: (note: NoteResult) => void
}) {
  const [shareOpen, setShareOpen] = useState(false)
  const [infoOpen, setInfoOpen] = useState(false)
  const [passwordOpen, setPasswordOpen] = useState(false)
  const [autoExpireOpen, setAutoExpireOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(noteUrl(mine.slug))
      toast.success("Link copied to clipboard")
    } catch {
      toast.info(noteUrl(mine.slug))
    }
  }

  return (
    <>
      <TiptapButton
        type="button"
        variant="ghost"
        className="tiptap-save-button"
        data-appearance="default"
        role="button"
        tabIndex={-1}
        aria-label="Share note"
        tooltip="Share"
        onClick={() => setShareOpen(true)}
      >
        <span className="tiptap-button-text">Share</span>
      </TiptapButton>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <TiptapButton
            type="button"
            variant="ghost"
            data-appearance="default"
            role="button"
            tabIndex={-1}
            aria-label="Note actions"
            tooltip="Actions"
          >
            <EllipsisVertical className="tiptap-button-icon" />
          </TiptapButton>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => setShareOpen(true)}>
            Share
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={handleCopyLink}>
            Copy Link
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => setInfoOpen(true)}>
            Info
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => setPasswordOpen(true)}>
            Password protect
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => setAutoExpireOpen(true)}>
            Auto Expire
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem
            variant="destructive"
            onSelect={() => setDeleteOpen(true)}
          >
            Delete Note
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <ShareNoteDialog
        open={shareOpen}
        onOpenChange={setShareOpen}
        note={note}
        mine={mine}
      />
      <InfoDialog
        open={infoOpen}
        onOpenChange={setInfoOpen}
        note={note}
        mine={mine}
      />
      <PasswordDialog
        open={passwordOpen}
        onOpenChange={setPasswordOpen}
        note={note}
        mine={mine}
      />
      <AutoExpireDialog
        open={autoExpireOpen}
        onOpenChange={setAutoExpireOpen}
        note={note}
        mine={mine}
        onNoteChange={onNoteChange}
      />
      <DeleteDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        note={note}
        mine={mine}
      />
    </>
  )
}

export default NoteOwnerControls
