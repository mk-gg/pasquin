import { Copy, KeyRound } from "lucide-react"
import { toast } from "sonner"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

/** Input-styled display of an edit key: [key icon | key | copy button]. */
export function KeyField({
  value,
  className,
}: {
  value: string
  className?: string
}) {
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      toast.success("Edit key copied to clipboard")
    } catch {
      // the key text below stays selectable as a fallback
    }
  }

  return (
    <div
      className={cn(
        "border-input dark:bg-input/30 flex h-8 min-w-0 items-center gap-1.5 rounded-md border bg-transparent pr-1 pl-2.5 shadow-xs",
        className
      )}
    >
      <KeyRound className="text-muted-foreground size-3.5 shrink-0" />
      <code className="min-w-0 flex-1 truncate font-mono text-xs select-all">
        {value}
      </code>
      <div className="bg-border h-4 w-px shrink-0" />
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label="Copy edit key"
        className="size-6 shrink-0"
        onClick={handleCopy}
      >
        <Copy className="size-3.5" />
      </Button>
    </div>
  )
}

export default KeyField
