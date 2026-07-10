import { useEffect, useState } from "react"
import {
  CircleHelp,
  MoreHorizontal,
  PanelLeft,
  SquarePenIcon,
  Search,
  LogIn,
} from "lucide-react"
import { toast } from "sonner"

import { ApiError, deleteNote, updateNote } from "@/lib/api"
import { requestSignIn, useAuth } from "@/lib/auth"
import {
  listMyNotes,
  removeMyNote,
  renameMyNote,
  syncOnSignIn,
  type MyNote,
} from "@/lib/my-notes"
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
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

function noteUrl(slug: string): string {
  return `${window.location.origin}/n/${slug}`
}

function relativeTime(iso: string): string {
  const minutes = (Date.now() - new Date(iso).getTime()) / 60_000
  if (minutes < 1) return "just now"
  if (minutes < 60) return `${Math.floor(minutes)}m ago`
  const hours = minutes / 60
  if (hours < 24) return `${Math.floor(hours)}h ago`
  const days = hours / 24
  if (days < 30) return `${Math.floor(days)}d ago`
  return new Date(iso).toLocaleDateString()
}

export function NotesSidebar({ currentSlug }: { currentSlug?: string }) {
  const { signedIn } = useAuth()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const [notes, setNotes] = useState<MyNote[]>([])
  const [renaming, setRenaming] = useState<MyNote | null>(null)
  const [renameValue, setRenameValue] = useState("")
  const [deleting, setDeleting] = useState<MyNote | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open) return
    setNotes(listMyNotes())
    // Signed-in users also pull notes created on other devices; show the
    // local list immediately and refresh once the account merge lands.
    if (signedIn) {
      syncOnSignIn().then(() => setNotes(listMyNotes()))
    }
  }, [open, signedIn])

  const filtered = notes.filter((note) =>
    note.title.toLowerCase().includes(query.trim().toLowerCase())
  )

  const handleCopyLink = async (note: MyNote) => {
    try {
      await navigator.clipboard.writeText(noteUrl(note.slug))
      toast.success("Link copied to clipboard")
    } catch {
      toast.info(noteUrl(note.slug))
    }
  }

  const handleRename = async () => {
    if (!renaming) return
    const title = renameValue.trim()
    if (!title) return
    setBusy(true)
    try {
      await updateNote(renaming.slug, renaming.editKey, { title })
      renameMyNote(renaming.slug, title)
      setNotes(listMyNotes())
      setRenaming(null)
      toast.success("Note renamed")
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to rename note")
    } finally {
      setBusy(false)
    }
  }

  const handleDelete = async () => {
    if (!deleting) return
    setBusy(true)
    try {
      await deleteNote(deleting.slug, deleting.editKey)
      removeMyNote(deleting.slug)
      setNotes(listMyNotes())
      toast.success("Note deleted")
      const wasCurrent = deleting.slug === currentSlug
      setDeleting(null)
      if (wasCurrent) window.location.assign("/n")
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to delete note")
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <Sheet open={open} onOpenChange={setOpen}>
        <SheetTrigger asChild>
          <TiptapButton
            type="button"
            variant="ghost"
            data-appearance="default"
            role="button"
            tabIndex={-1}
            aria-label="My notes"
            tooltip="My notes"
          >
            <PanelLeft className="tiptap-button-icon" />
          </TiptapButton>
        </SheetTrigger>
        <SheetContent side="left" className="w-80 gap-0 p-0">
          <SheetHeader className="pb-2">
            <div className="flex items-center justify-between gap-2 pr-7">
              <SheetTitle>My Notes</SheetTitle>
              <Button
                variant="ghost"
                size="icon"
                className="size-7"
                aria-label="New note"
                title="New note"
                asChild
              >
                <a href="/n">
                  <SquarePenIcon />
                </a>
              </Button>
            </div>
            <SheetDescription className="sr-only">
              Notes created in this browser
            </SheetDescription>
            <div className="relative mt-1">
              <Search className="text-muted-foreground absolute top-1/2 left-2.5 size-4 -translate-y-1/2" />
              <Input
                placeholder="Search notes..."
                className="pl-8"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </div>
          </SheetHeader>

          <nav className="flex-1 overflow-y-auto px-2 pb-4">
            {filtered.length === 0 && (
              <p className="text-muted-foreground px-2 py-6 text-center text-sm">
                {notes.length === 0
                  ? "No notes yet. Save a note and it will show up here."
                  : "No notes match your search."}
              </p>
            )}
            {filtered.map((note) => (
              <div
                key={note.slug}
                className="group hover:bg-accent has-[a:focus-visible]:bg-accent relative flex items-center rounded-md"
                data-active={note.slug === currentSlug || undefined}
              >
                <a
                  href={noteUrl(note.slug)}
                  className="min-w-0 flex-1 px-2 py-2 outline-none"
                >
                  <p className="truncate text-sm font-medium">
                    {note.title}
                    {note.slug === currentSlug && (
                      <span className="text-muted-foreground ml-1.5 text-xs">
                        (current)
                      </span>
                    )}
                  </p>
                  <p className="text-muted-foreground truncate text-xs">
                    {relativeTime(note.createdAt)}
                  </p>
                </a>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      aria-label={`Actions for ${note.title}`}
                      className="mr-1 size-7 opacity-0 group-hover:opacity-100 focus-visible:opacity-100 data-[state=open]:opacity-100"
                    >
                      <MoreHorizontal />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="start" side="right">
                    <DropdownMenuItem
                      onSelect={() => {
                        setRenameValue(note.title)
                        setRenaming(note)
                      }}
                    >
                      Rename
                    </DropdownMenuItem>
                    <DropdownMenuItem onSelect={() => handleCopyLink(note)}>
                      Copy Link
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      variant="destructive"
                      onSelect={() => setDeleting(note)}
                    >
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            ))}
          </nav>

          <div className="mt-auto">
            <Separator />
            <div className="flex items-center justify-between p-2">
              {signedIn ? (
                <span />
              ) : (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setOpen(false)
                    requestSignIn()
                  }}
                >
                  <LogIn />
                  Sign In
                </Button>
              )}

              <TooltipProvider delayDuration={200}>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-8"
                      aria-label="Help & Support"
                      asChild
                    >
                      <a href="/contact">
                        <CircleHelp />
                      </a>
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>Help &amp; Support</TooltipContent>
                </Tooltip>
              </TooltipProvider>
            </div>
          </div>
        </SheetContent>
      </Sheet>

      <Dialog
        open={renaming !== null}
        onOpenChange={(next) => !next && setRenaming(null)}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Rename note</DialogTitle>
            <DialogDescription className="sr-only">
              Choose a new title for this note
            </DialogDescription>
          </DialogHeader>
          <form
            className="flex flex-col gap-4"
            onSubmit={(event) => {
              event.preventDefault()
              handleRename()
            }}
          >
            <Input
              autoFocus
              maxLength={200}
              value={renameValue}
              onChange={(event) => setRenameValue(event.target.value)}
            />
            <DialogFooter>
              <Button
                type="submit"
                disabled={busy || renameValue.trim().length === 0}
              >
                {busy ? "Renaming..." : "Rename"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog
        open={deleting !== null}
        onOpenChange={(next) => !next && setDeleting(null)}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Delete note?</DialogTitle>
            <DialogDescription>
              &ldquo;{deleting?.title}&rdquo; will be permanently deleted. This
              cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleting(null)}>
              Cancel
            </Button>
            <Button variant="destructive" disabled={busy} onClick={handleDelete}>
              {busy ? "Deleting..." : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}

export default NotesSidebar
