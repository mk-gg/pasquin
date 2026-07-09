import { Info, TriangleAlert } from "lucide-react"

import { type NoteResult } from "@/lib/api"
import { type MyNote } from "@/lib/my-notes"
import { KeyField } from "@/components/KeyField"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button as TiptapButton } from "@/components/tiptap-ui-primitive/button"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"

/** Created/updated timestamps, edit key, and the keep-it-safe warning. */
export function NoteInfoContent({
  note,
  mine,
}: {
  note: NoteResult
  mine: MyNote
}) {
  return (
    <div className="flex flex-col gap-4">
      <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1.5 text-sm">
        <dt className="text-muted-foreground">Created</dt>
        <dd>{new Date(note.createdAt).toLocaleString()}</dd>
        <dt className="text-muted-foreground">Updated</dt>
        <dd>{new Date(note.updatedAt).toLocaleString()}</dd>
      </dl>

      <p className="text-muted-foreground text-sm">
        To update or delete this note, you&apos;ll need this key.
      </p>

      <KeyField value={mine.editKey} />

      <Alert className="bg-amber-50 dark:bg-amber-950">
        <TriangleAlert />
        <AlertDescription>
          <p>
            If you clear your browser data, this key will be lost. So please
            keep it safe!
          </p>
        </AlertDescription>
      </Alert>
    </div>
  )
}

/** Info icon button in the note header that opens the details in a popover. */
export function NoteInfoPopover({
  note,
  mine,
}: {
  note: NoteResult
  mine: MyNote
}) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <TiptapButton
          type="button"
          variant="ghost"
          data-appearance="default"
          role="button"
          tabIndex={-1}
          aria-label="Note info"
          tooltip="Info"
        >
          <Info className="tiptap-button-icon" />
        </TiptapButton>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-96 max-w-[calc(100vw-2rem)]">
        <NoteInfoContent note={note} mine={mine} />
      </PopoverContent>
    </Popover>
  )
}
