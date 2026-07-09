import { useState } from "react"
import { EllipsisVertical, Flag } from "lucide-react"

import { EditKeyDialog } from "@/components/EditKeyDialog"
import { ReportNoteDialog } from "@/components/ReportNoteDialog"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

/** Report flag + actions dropdown shown to readers of a note (viewer/locked). */
export function NoteViewerActions({ slug }: { slug: string }) {
  const [reportOpen, setReportOpen] = useState(false)
  const [editOpen, setEditOpen] = useState(false)

  return (
    <div className="flex items-center gap-1">
      <TooltipProvider delayDuration={200}>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Report note"
              onClick={() => setReportOpen(true)}
            >
              <Flag />
            </Button>
          </TooltipTrigger>
          <TooltipContent>Report</TooltipContent>
        </Tooltip>

        <DropdownMenu>
          <Tooltip>
            <TooltipTrigger asChild>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Note actions">
                  <EllipsisVertical />
                </Button>
              </DropdownMenuTrigger>
            </TooltipTrigger>
            <TooltipContent>Actions</TooltipContent>
          </Tooltip>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onSelect={() => setEditOpen(true)}>
              Edit Note
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={() => setReportOpen(true)}>
              Report Note
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </TooltipProvider>

      <ReportNoteDialog
        slug={slug}
        open={reportOpen}
        onOpenChange={setReportOpen}
      />
      <EditKeyDialog slug={slug} open={editOpen} onOpenChange={setEditOpen} />
    </div>
  )
}

export default NoteViewerActions
