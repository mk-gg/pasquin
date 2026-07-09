import { type Editor } from "@tiptap/react"

// --- Hooks ---
import { useTiptapEditor } from "@/hooks/use-tiptap-editor"

// --- UI Primitives ---
import { Button } from "@/components/tiptap-ui-primitive/button"
import type { ButtonProps } from "@/components/tiptap-ui-primitive/button"

// --- Components ---
import { SaveNoteDialog } from "@/components/tiptap-ui/save-button/save-dialog"

// --- Styles ---
import "@/components/tiptap-ui/save-button/save-button.scss"

export interface SaveButtonProps extends Omit<ButtonProps, "type"> {
  editor?: Editor | null
}

export function SaveButton({
  editor: providedEditor,
  ...props
}: SaveButtonProps) {
  const { editor } = useTiptapEditor(providedEditor)

  if (!editor) return null

  return (
    <SaveNoteDialog editor={editor}>
      <Button
        type="button"
        variant="ghost"
        className="tiptap-save-button"
        data-appearance="default"
        role="button"
        tabIndex={-1}
        aria-label="Save note"
        tooltip="Save"
        disabled={!editor.isEditable}
        {...props}
      >
        <span className="tiptap-button-text">Save</span>
      </Button>
    </SaveNoteDialog>
  )
}

export default SaveButton
