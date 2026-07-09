import { useState } from "react"
import { type Editor, useEditorState } from "@tiptap/react"

// --- Hooks ---
import { useTiptapEditor } from "@/hooks/use-tiptap-editor"

// --- Icons ---
import { BanIcon } from "@/components/tiptap-icons/ban-icon"
import { TextColorIcon } from "@/components/tiptap-icons/text-color-icon"

// --- UI Primitives ---
import { Button } from "@/components/tiptap-ui-primitive/button"
import type { ButtonProps } from "@/components/tiptap-ui-primitive/button"
import { ButtonGroup } from "@/components/tiptap-ui-primitive/button-group"
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
} from "@/components/tiptap-ui-primitive/popover"
import { Separator } from "@/components/tiptap-ui-primitive/separator"
import {
  Card,
  CardBody,
  CardItemGroup,
} from "@/components/tiptap-ui-primitive/card"

// Reuses the highlight swatch styles (.tiptap-button-highlight)
import "@/components/tiptap-ui/color-highlight-button/color-highlight-button.scss"

export interface TextColor {
  label: string
  value: string
}

export const DEFAULT_TEXT_COLORS: TextColor[] = [
  { label: "Gray", value: "var(--tt-color-text-gray)" },
  { label: "Brown", value: "var(--tt-color-text-brown)" },
  { label: "Orange", value: "var(--tt-color-text-orange)" },
  { label: "Yellow", value: "var(--tt-color-text-yellow)" },
  { label: "Green", value: "var(--tt-color-text-green)" },
  { label: "Blue", value: "var(--tt-color-text-blue)" },
  { label: "Purple", value: "var(--tt-color-text-purple)" },
  { label: "Red", value: "var(--tt-color-text-red)" },
]

export interface ColorTextPopoverProps extends Omit<ButtonProps, "type"> {
  editor?: Editor | null
  colors?: TextColor[]
}

export function ColorTextPopover({
  editor: providedEditor,
  colors = DEFAULT_TEXT_COLORS,
  ...props
}: ColorTextPopoverProps) {
  const { editor } = useTiptapEditor(providedEditor)
  const [isOpen, setIsOpen] = useState(false)

  const activeColor = useEditorState({
    editor,
    selector: (context) =>
      (context.editor?.getAttributes("textStyle").color as
        | string
        | undefined) ?? undefined,
  })

  if (!editor) return null

  const applyColor = (color: string) => {
    editor.chain().focus().setColor(color).run()
    setIsOpen(false)
  }

  const removeColor = () => {
    editor.chain().focus().unsetColor().run()
    setIsOpen(false)
  }

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          data-appearance="default"
          data-active-state={activeColor ? "on" : "off"}
          role="button"
          tabIndex={-1}
          aria-label="Text color"
          aria-pressed={Boolean(activeColor)}
          tooltip="Text color"
          disabled={!editor.isEditable}
          style={
            activeColor
              ? ({
                  color: activeColor,
                  backgroundColor: `color-mix(in srgb, ${activeColor} 20%, transparent)`,
                } as React.CSSProperties)
              : undefined
          }
          {...props}
        >
          <TextColorIcon className="tiptap-button-icon" />
        </Button>
      </PopoverTrigger>
      <PopoverContent
        aria-label="Text colors"
        onCloseAutoFocus={(event) => event.preventDefault()}
      >
        <Card>
          <CardBody>
            <CardItemGroup orientation="horizontal">
              <ButtonGroup>
                {colors.map((color) => (
                  <Button
                    key={color.value}
                    type="button"
                    variant="ghost"
                    onClick={() => applyColor(color.value)}
                    aria-label={`${color.label} text color`}
                    aria-pressed={activeColor === color.value}
                    data-active-state={
                      activeColor === color.value ? "on" : "off"
                    }
                    tooltip={color.label}
                  >
                    <span
                      className="tiptap-button-highlight"
                      style={
                        {
                          "--highlight-color": color.value,
                        } as React.CSSProperties
                      }
                    />
                  </Button>
                ))}
              </ButtonGroup>
              <Separator />
              <ButtonGroup>
                <Button
                  onClick={removeColor}
                  aria-label="Remove text color"
                  tooltip="Remove text color"
                  type="button"
                  variant="ghost"
                >
                  <BanIcon className="tiptap-button-icon" />
                </Button>
              </ButtonGroup>
            </CardItemGroup>
          </CardBody>
        </Card>
      </PopoverContent>
    </Popover>
  )
}

export default ColorTextPopover
