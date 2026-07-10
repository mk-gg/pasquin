import { useEffect, useRef, useState } from "react"
import { EditorContent, useEditor } from "@tiptap/react"
import { StarterKit } from "@tiptap/starter-kit"
import { Image } from "@tiptap/extension-image"
import { TaskItem, TaskList } from "@tiptap/extension-list"
import { TextAlign } from "@tiptap/extension-text-align"
import { Typography } from "@tiptap/extension-typography"
import { Highlight } from "@tiptap/extension-highlight"
import { Color, TextStyle } from "@tiptap/extension-text-style"
import {
  FileQuestion,
  LoaderCircle,
  Lock,
  LogIn,
  SquarePen,
  Timer,
  X,
} from "lucide-react"
import { toast } from "sonner"

import { HorizontalRule } from "@/components/tiptap-node/horizontal-rule-node/horizontal-rule-node-extension"
import "@/components/tiptap-node/blockquote-node/blockquote-node.scss"
import "@/components/tiptap-node/code-block-node/code-block-node.scss"
import "@/components/tiptap-node/horizontal-rule-node/horizontal-rule-node.scss"
import "@/components/tiptap-node/list-node/list-node.scss"
import "@/components/tiptap-node/image-node/image-node.scss"
import "@/components/tiptap-node/heading-node/heading-node.scss"
import "@/components/tiptap-node/paragraph-node/paragraph-node.scss"
import "@/components/tiptap-templates/simple/simple-editor.scss"

import { ApiError, getNote, unlockNote, updateNote, type NoteResult } from "@/lib/api"
import { requestSignIn, useAuth } from "@/lib/auth"
import { adoptNote, dismissKeyBanner, getMyNote, type MyNote } from "@/lib/my-notes"
import { KeyField } from "@/components/KeyField"
import { NoteInfoPopover } from "@/components/NoteInfo"
import { NoteOwnerControls } from "@/components/NoteOwnerControls"
import { NotesSidebar } from "@/components/NotesSidebar"
import { NoteViewerActions } from "@/components/NoteViewerActions"
import { SimpleEditor } from "@/components/tiptap-templates/simple/simple-editor"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

/** Extracts the note slug from `/n?id={slug}` or a rewritten `/n/{slug}` path. */
function resolveSlug(): string | null {
  const id = new URLSearchParams(window.location.search).get("id")
  if (id) return id
  const match = window.location.pathname.match(/^\/n\/([A-Za-z0-9]+)\/?$/)
  return match ? match[1] : null
}

function noteUrl(slug: string): string {
  return `${window.location.origin}/n/${slug}`
}

/** Formats time remaining as the largest whole unit: "31d", "7h", "49m", "<1m". */
function formatRemaining(expiresAt: string, now: Date): string {
  const remainingMs = new Date(expiresAt).getTime() - now.getTime()
  const minutes = remainingMs / 60_000
  if (minutes < 1) return "<1m"
  if (minutes < 60) return `${Math.floor(minutes)}m`
  const hours = minutes / 60
  if (hours < 24) return `${Math.floor(hours)}h`
  return `${Math.ceil(hours / 24)}d`
}

function ExpiryBadge({ expiresAt }: { expiresAt: string }) {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const interval = setInterval(() => setNow(new Date()), 30_000)
    return () => clearInterval(interval)
  }, [])

  return (
    <Badge variant="outline" className="text-muted-foreground shrink-0">
      <Timer />
      Expires {formatRemaining(expiresAt, now)}
    </Badge>
  )
}

function KeyBanner({
  editKey,
  onDismiss,
}: {
  editKey: string
  onDismiss: () => void
}) {
  return (
    <div className="bg-muted/50 text-muted-foreground flex items-center gap-2 border-b px-3 py-1.5 text-xs">
      {/* Message hides on narrow screens so the key field and copy button
          never get crammed; the key stays usable on mobile. */}
      <span className="hidden min-w-0 truncate sm:inline">
        To update or delete this note, you&apos;ll need this key. Keep it safe!
      </span>
      <KeyField
        value={editKey}
        className="h-7 min-w-0 flex-1 sm:max-w-72 sm:flex-none"
      />
      <span className="hidden flex-1 sm:block" />
      <button
        type="button"
        aria-label="Dismiss"
        className="hover:text-foreground text-muted-foreground shrink-0 p-1 transition-colors"
        onClick={onDismiss}
      >
        <X className="size-3.5" />
      </button>
    </div>
  )
}

/** Owner view: live editor with debounced server autosave. */
function NoteEditor({
  note,
  mine,
  onNoteChange,
}: {
  note: NoteResult
  mine: MyNote
  onNoteChange: (note: NoteResult) => void
}) {
  const [bannerVisible, setBannerVisible] = useState(!mine.keyBannerDismissed)

  const handleDocChange = (doc: unknown) => {
    updateNote(mine.slug, mine.editKey, { content: doc }).catch((e) => {
      toast.error(
        e instanceof ApiError ? e.message : "Failed to save changes",
        { id: "autosave-error" }
      )
    })
  }

  const handleDismiss = () => {
    dismissKeyBanner(mine.slug)
    setBannerVisible(false)
  }

  return (
    <SimpleEditor
      initialContent={note.content}
      onDocChange={handleDocChange}
      actionButton={
        <NoteOwnerControls note={note} mine={mine} onNoteChange={onNoteChange} />
      }
      leadingButton={<NotesSidebar currentSlug={mine.slug} />}
      banner={
        bannerVisible ? (
          <KeyBanner editKey={mine.editKey} onDismiss={handleDismiss} />
        ) : null
      }
    />
  )
}

function NoteContent({ content }: { content: unknown }) {
  const editor = useEditor({
    editable: false,
    immediatelyRender: false,
    content: content as Parameters<typeof useEditor>[0]["content"],
    editorProps: {
      attributes: {
        class: "simple-editor",
        "aria-label": "Note content",
      },
    },
    extensions: [
      StarterKit.configure({ horizontalRule: false, link: { openOnClick: true } }),
      HorizontalRule,
      TextAlign.configure({ types: ["heading", "paragraph"] }),
      TaskList,
      TaskItem.configure({ nested: true }),
      Highlight.configure({ multicolor: true }),
      TextStyle,
      Color,
      Image,
      Typography,
    ],
  })

  return (
    <div className="simple-editor-wrapper">
      <EditorContent
        editor={editor}
        role="presentation"
        className="simple-editor-content"
      />
    </div>
  )
}

function CenteredMessage({ children }: { children: React.ReactNode }) {
  return (
    <div className="text-muted-foreground flex h-full flex-col items-center justify-center gap-3 p-8 text-center text-sm">
      {children}
    </div>
  )
}

function PasswordGate({
  onUnlock,
  unlocking,
  error,
}: {
  onUnlock: (password: string) => void
  unlocking: boolean
  error: string | null
}) {
  const [password, setPassword] = useState("")

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (password.length > 0) onUnlock(password)
  }

  return (
    <div className="flex h-full items-center justify-center p-8">
      <form
        className="flex w-full max-w-sm flex-col gap-4"
        onSubmit={handleSubmit}
      >
        <div className="flex flex-col items-center gap-2 text-center">
          <Lock className="text-muted-foreground size-8" />
          <h2 className="text-lg font-medium">This note is password protected</h2>
          <p className="text-muted-foreground text-sm">
            Enter the password to view its contents.
          </p>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="note-password" className="sr-only">
            Password
          </Label>
          <Input
            id="note-password"
            type="password"
            placeholder="Password"
            autoComplete="current-password"
            autoFocus
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {error && <p className="text-destructive text-xs">{error}</p>}
        </div>
        <Button type="submit" disabled={unlocking || password.length === 0}>
          {unlocking ? "Unlocking..." : "Unlock"}
        </Button>
      </form>
    </div>
  )
}

export function NoteViewer() {
  const { signedIn } = useAuth()
  const [note, setNote] = useState<NoteResult | null>(null)
  const [status, setStatus] = useState<"loading" | "notFound" | "locked" | "ready">(
    "loading"
  )
  const [unlocking, setUnlocking] = useState(false)
  const [unlockError, setUnlockError] = useState<string | null>(null)
  const [slug] = useState<string | null>(() => resolveSlug())
  const [mine, setMine] = useState<MyNote | null>(null)

  useEffect(() => {
    if (!slug) {
      return
    }
    const run = async () => {
      let owned = getMyNote(slug)

      // Edit links carry the key in the URL fragment (#key), which browsers
      // never send to the server. Validate and adopt it, then scrub it from
      // the address bar.
      const hashKey = window.location.hash.match(/^#([0-9a-f]{32})$/)?.[1]
      if (!owned && hashKey) {
        try {
          owned = await adoptNote(slug, hashKey)
        } catch {
          owned = null
        }
        history.replaceState(
          null,
          "",
          window.location.pathname + window.location.search
        )
      }
      setMine(owned)

      try {
        const fetched = await getNote(slug, owned?.editKey)
        setNote(fetched)
        document.title = fetched.title ?? "Protected Note"
        setStatus(
          fetched.passwordProtected && fetched.content == null ? "locked" : "ready"
        )
      } catch {
        setStatus("notFound")
      }
    }
    run()
  }, [slug])

  const handleUnlock = async (password: string) => {
    if (!slug) return
    setUnlocking(true)
    setUnlockError(null)
    try {
      const unlocked = await unlockNote(slug, password)
      setNote(unlocked)
      document.title = unlocked.title
      setStatus("ready")
    } catch (e) {
      setUnlockError(
        e instanceof ApiError && e.status === 401
          ? "Wrong password. Please try again."
          : "Something went wrong. Please try again."
      )
    } finally {
      setUnlocking(false)
    }
  }

  const isOwner = Boolean(mine && status === "ready" && note?.content != null)

  // /n without an id: workspace mode — a fresh scratchpad draft with the
  // sidebar; saving redirects to the created note.
  if (!slug) {
    return (
      <div className="h-[70dvh] overflow-hidden rounded-lg border">
        <SimpleEditor leadingButton={<NotesSidebar />} />
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      {note && slug && (
        <div className="flex min-h-9 items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-1.5">
            {isOwner && mine ? (
              <NoteInfoPopover note={note} mine={mine} />
            ) : (
              <h1 className="truncate text-sm font-medium">
                {note.title ?? "Protected Note"}
              </h1>
            )}
            {note.expiresAt && <ExpiryBadge expiresAt={note.expiresAt} />}
          </div>
          {!isOwner && <NoteViewerActions slug={slug} />}
        </div>
      )}
      <div className="h-[70dvh] overflow-hidden rounded-lg border">
        {status === "loading" && (
          <CenteredMessage>
            <LoaderCircle className="size-6 animate-spin" />
            Loading note...
          </CenteredMessage>
        )}
        {status === "notFound" && (
          <CenteredMessage>
            <FileQuestion className="size-8" />
            <span className="text-foreground text-lg font-medium">
              Note not found
            </span>
            This note may have expired or the link is incorrect.
            <div className="mt-2 flex items-center gap-2">
              <Button size="sm" asChild>
                <a href="/n">
                  <SquarePen />
                  New Note
                </a>
              </Button>
              {!signedIn && (
                <Button variant="outline" size="sm" onClick={requestSignIn}>
                  <LogIn />
                  Sign In
                </Button>
              )}
            </div>
          </CenteredMessage>
        )}
        {status === "locked" && (
          <PasswordGate
            onUnlock={handleUnlock}
            unlocking={unlocking}
            error={unlockError}
          />
        )}
        {status === "ready" && note?.content != null && mine && (
          <NoteEditor note={note} mine={mine} onNoteChange={setNote} />
        )}
        {status === "ready" && note?.content != null && !mine && (
          <NoteContent content={note.content} />
        )}
      </div>
    </div>
  )
}

export default NoteViewer
