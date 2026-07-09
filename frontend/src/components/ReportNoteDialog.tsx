import { useState } from "react"
import { Info } from "lucide-react"
import { toast } from "sonner"

import { SITE } from "@/consts"
import { ApiError, submitReport } from "@/lib/api"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Textarea } from "@/components/ui/textarea"

const REPORT_TYPES = [
  "Copyright Violation",
  "Inappropriate Content",
  "Spam",
  "Other Issue",
]

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function ReportNoteDialog({
  slug,
  open,
  onOpenChange,
}: {
  slug: string
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [type, setType] = useState<string | null>(null)
  const [details, setDetails] = useState("")
  const [email, setEmail] = useState("")
  const [submitting, setSubmitting] = useState(false)

  const appName = SITE.title
  const submitDisabled =
    submitting ||
    type === null ||
    details.trim().length === 0 ||
    !EMAIL_PATTERN.test(email)

  const resetForm = () => {
    setType(null)
    setDetails("")
    setEmail("")
  }

  const handleOpenChange = (nextOpen: boolean) => {
    onOpenChange(nextOpen)
    if (!nextOpen) resetForm()
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (type === null) return
    setSubmitting(true)
    try {
      await submitReport({ type, email, details, noteSlug: slug })
      toast.success("Report submitted. Thank you!")
      resetForm()
    } catch (e) {
      toast.error(
        e instanceof ApiError ? e.message : "Something went wrong. Please try again."
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Report Note</DialogTitle>
          <DialogDescription>
            This note is hosted by <span className="capitalize">{appName}</span>
            . If you believe this note violates{" "}
            <span className="capitalize">{appName}</span>&apos;s terms of
            service, please report it.
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Alert className="border-blue-500/50 bg-blue-500/10 text-blue-700 dark:text-blue-400 [&>svg]:text-current *:data-[slot=alert-description]:text-blue-700/90 dark:*:data-[slot=alert-description]:text-blue-400/90">
            <Info />
            <AlertDescription>
              <p>
                This is not a contact form for the person or organization that
                created this note.
              </p>
            </AlertDescription>
          </Alert>

          <RadioGroup
            // Empty string (matches no option) keeps Radix in controlled
            // mode so resetting the form visually clears the selection
            value={type ?? ""}
            onValueChange={setType}
            aria-label="Report type"
          >
            {REPORT_TYPES.map((option) => (
              <div key={option} className="flex items-center gap-2">
                <RadioGroupItem value={option} id={`report-${option}`} />
                <Label htmlFor={`report-${option}`} className="cursor-pointer">
                  {option}
                </Label>
              </div>
            ))}
          </RadioGroup>

          {type !== null && (
            <>
              <div className="flex flex-col gap-2">
                <Label htmlFor="report-details">Details</Label>
                <Textarea
                  id="report-details"
                  placeholder="Please describe how this note violates our terms of service"
                  className="min-h-24"
                  value={details}
                  onChange={(event) => setDetails(event.target.value)}
                />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="report-email">Email</Label>
                <Input
                  id="report-email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </div>
            </>
          )}

          <Button type="submit" disabled={submitDisabled}>
            {submitting ? "Reporting..." : "Report Note"}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export default ReportNoteDialog
