"use client"

import { useEffect, useRef, useState } from "react"
import { EditorContent, EditorContext, useEditor } from "@tiptap/react"

// --- Tiptap Core Extensions ---
import { StarterKit } from "@tiptap/starter-kit"
import { Image } from "@tiptap/extension-image"
import { TaskItem, TaskList } from "@tiptap/extension-list"
import { TextAlign } from "@tiptap/extension-text-align"
import { Typography } from "@tiptap/extension-typography"
import { Highlight } from "@tiptap/extension-highlight"
import { Color, TextStyle } from "@tiptap/extension-text-style"
import { Placeholder, Selection } from "@tiptap/extensions"

// --- UI Primitives ---
import { Button } from "@/components/tiptap-ui-primitive/button"
import { Spacer } from "@/components/tiptap-ui-primitive/spacer"
import {
  Toolbar,
  ToolbarGroup,
  ToolbarSeparator,
} from "@/components/tiptap-ui-primitive/toolbar"

// --- Tiptap Node ---
import { HorizontalRule } from "@/components/tiptap-node/horizontal-rule-node/horizontal-rule-node-extension"
import "@/components/tiptap-node/blockquote-node/blockquote-node.scss"
import "@/components/tiptap-node/code-block-node/code-block-node.scss"
import "@/components/tiptap-node/horizontal-rule-node/horizontal-rule-node.scss"
import "@/components/tiptap-node/list-node/list-node.scss"
import "@/components/tiptap-node/image-node/image-node.scss"
import "@/components/tiptap-node/heading-node/heading-node.scss"
import "@/components/tiptap-node/paragraph-node/paragraph-node.scss"

// --- Tiptap UI ---
import { HeadingDropdownMenu } from "@/components/tiptap-ui/heading-dropdown-menu"
import { ListDropdownMenu } from "@/components/tiptap-ui/list-dropdown-menu"
import { BlockquoteButton } from "@/components/tiptap-ui/blockquote-button"
import { CodeBlockButton } from "@/components/tiptap-ui/code-block-button"
import {
  ColorHighlightPopover,
  ColorHighlightPopoverContent,
  ColorHighlightPopoverButton,
} from "@/components/tiptap-ui/color-highlight-popover"
import {
  LinkPopover,
  LinkContent,
  LinkButton,
} from "@/components/tiptap-ui/link-popover"
import { ColorTextPopover } from "@/components/tiptap-ui/color-text-popover"
import { MarkButton } from "@/components/tiptap-ui/mark-button"
import { SaveButton } from "@/components/tiptap-ui/save-button"
import { UndoRedoButton } from "@/components/tiptap-ui/undo-redo-button"

// --- Icons ---
import { ArrowLeftIcon } from "@/components/tiptap-icons/arrow-left-icon"
import { HighlighterIcon } from "@/components/tiptap-icons/highlighter-icon"
import { LinkIcon } from "@/components/tiptap-icons/link-icon"

// --- Hooks ---
import { useIsBreakpoint } from "@/hooks/use-is-breakpoint"

// --- Components ---

// --- Lib ---
import { DRAFT_STORAGE_KEY } from "@/lib/my-notes"

// --- Styles ---
import "@/components/tiptap-templates/simple/simple-editor.scss"


const MainToolbarContent = ({
  onHighlighterClick,
  onLinkClick,
  isMobile,
  actionButton,
  leadingButton,
}: {
  onHighlighterClick: () => void
  onLinkClick: () => void
  isMobile: boolean
  actionButton?: React.ReactNode
  leadingButton?: React.ReactNode
}) => {
  return (
    <>
      {leadingButton && (
        <ToolbarGroup className="toolbar-leading-group">
          {leadingButton}
        </ToolbarGroup>
      )}

      <div className="tiptap-toolbar-scroll">
        <Spacer />

        <ToolbarGroup>
          <UndoRedoButton action="undo" />
          <UndoRedoButton action="redo" />
        </ToolbarGroup>

        <ToolbarSeparator />

        <ToolbarGroup>
          <HeadingDropdownMenu modal={false} levels={[1, 2, 3, 4]} />
          <MarkButton type="bold" />
          <MarkButton type="italic" />

          <MarkButton type="underline" />

          <ColorTextPopover />
          {!isMobile ? (
            <ColorHighlightPopover />
          ) : (
            <ColorHighlightPopoverButton onClick={onHighlighterClick} />
          )}
          {!isMobile ? <LinkPopover /> : <LinkButton onClick={onLinkClick} />}
          <MarkButton type="code" />
        </ToolbarGroup>

        <ToolbarSeparator />

        <ToolbarGroup>
          <ListDropdownMenu
            modal={false}
            types={["bulletList", "orderedList", "taskList"]}
          />
          <BlockquoteButton />
          <CodeBlockButton />
        </ToolbarGroup>

        <Spacer />
      </div>

      <ToolbarGroup className="toolbar-save-group">
        {actionButton ?? <SaveButton />}
      </ToolbarGroup>
    </>
  )
}

const MobileToolbarContent = ({
  type,
  onBack,
}: {
  type: "highlighter" | "link"
  onBack: () => void
}) => (
  <>
    <ToolbarGroup>
      <Button variant="ghost" onClick={onBack}>
        <ArrowLeftIcon className="tiptap-button-icon" />
        {type === "highlighter" ? (
          <HighlighterIcon className="tiptap-button-icon" />
        ) : (
          <LinkIcon className="tiptap-button-icon" />
        )}
      </Button>
    </ToolbarGroup>

    <ToolbarSeparator />

    {type === "highlighter" ? (
      <ColorHighlightPopoverContent />
    ) : (
      <LinkContent />
    )}
  </>
)

function loadSavedContent() {
  try {
    const raw = localStorage.getItem(DRAFT_STORAGE_KEY)
    return raw ? JSON.parse(raw) : undefined
  } catch {
    return undefined
  }
}

export interface SimpleEditorProps {
  /** Initial document; when omitted the local scratchpad draft is loaded. */
  initialContent?: unknown
  /**
   * Debounced change callback. When provided it replaces the default
   * localStorage draft persistence (used for server autosave).
   */
  onDocChange?: (doc: unknown) => void
  /** Replaces the Save button in the toolbar's action area. */
  actionButton?: React.ReactNode
  /** Rendered at the far left of the toolbar (e.g. the notes sidebar trigger). */
  leadingButton?: React.ReactNode
  /** Rendered between the toolbar and the content (e.g. the edit-key banner). */
  banner?: React.ReactNode
}

export function SimpleEditor({
  initialContent,
  onDocChange,
  actionButton,
  leadingButton,
  banner,
}: SimpleEditorProps = {}) {
  const isMobile = useIsBreakpoint()
  const [mobileView, setMobileView] = useState<"main" | "highlighter" | "link">(
    "main"
  )
  const saveTimeout = useRef<ReturnType<typeof setTimeout> | null>(null)
  const onDocChangeRef = useRef(onDocChange)
  onDocChangeRef.current = onDocChange

  const editor = useEditor({
    immediatelyRender: false,
    autofocus: "end",
    content:
      initialContent !== undefined
        ? (initialContent as Parameters<typeof useEditor>[0]["content"])
        : loadSavedContent(),
    onUpdate: ({ editor }) => {
      if (saveTimeout.current) clearTimeout(saveTimeout.current)
      saveTimeout.current = setTimeout(() => {
        const doc = editor.getJSON()
        if (onDocChangeRef.current) {
          onDocChangeRef.current(doc)
          return
        }
        try {
          localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(doc))
        } catch (error) {
          console.error("Failed to save editor content:", error)
        }
      }, 400)
    },
    editorProps: {
      attributes: {
        autocomplete: "off",
        autocorrect: "off",
        autocapitalize: "off",
        "aria-label": "Main content area, start typing to enter text.",
        class: "simple-editor",
      },
    },
    extensions: [
      StarterKit.configure({
        horizontalRule: false,
        strike: false,
        link: {
          openOnClick: false,
          enableClickSelection: true,
        },
      }),
      HorizontalRule,
      // Keep rendering existing aligned content, but disable the
      // Ctrl+Shift+L/E/R/J shortcuts since the toolbar buttons are gone
      TextAlign.extend({
        addKeyboardShortcuts() {
          return {}
        },
      }).configure({ types: ["heading", "paragraph"] }),
      TaskList,
      TaskItem.configure({ nested: true }),
      Highlight.configure({ multicolor: true }),
      TextStyle,
      Color,
      Image,
      Typography,
      Selection,
      Placeholder.configure({
        // Only show the placeholder on the pristine single empty paragraph;
        // editor.isEmpty alone stays true for multiple blank lines
        placeholder: ({ editor }) =>
          editor.isEmpty && editor.state.doc.childCount <= 1
            ? "Start typing to get started..."
            : "",
      }),
    ],
  })

  useEffect(() => {
    if (!isMobile && mobileView !== "main") {
      setMobileView("main")
    }
  }, [isMobile, mobileView])

  // Flush any pending debounced save if the page is closed/refreshed
  useEffect(() => {
    if (!editor) return
    const flush = () => {
      const doc = editor.getJSON()
      if (onDocChangeRef.current) {
        onDocChangeRef.current(doc)
        return
      }
      try {
        localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(doc))
      } catch {
        // ignore quota/serialization errors on unload
      }
    }
    window.addEventListener("beforeunload", flush)
    return () => window.removeEventListener("beforeunload", flush)
  }, [editor])

  return (
    <div className="simple-editor-wrapper">
      <EditorContext.Provider value={{ editor }}>
        <Toolbar>
          {mobileView === "main" ? (
            <MainToolbarContent
              onHighlighterClick={() => setMobileView("highlighter")}
              onLinkClick={() => setMobileView("link")}
              isMobile={isMobile}
              actionButton={actionButton}
              leadingButton={leadingButton}
            />
          ) : (
            <MobileToolbarContent
              type={mobileView === "highlighter" ? "highlighter" : "link"}
              onBack={() => setMobileView("main")}
            />
          )}
        </Toolbar>

        {banner}

        <EditorContent
          editor={editor}
          role="presentation"
          className="simple-editor-content"
        />
      </EditorContext.Provider>
    </div>
  )
}
