import { useState } from "react"
import { Info } from "lucide-react"
import { toast } from "sonner"

import { ApiError, submitReport } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { HoneypotField } from "@/components/HoneypotField"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const REPORT_TYPES = [
  "Copyright Violation",
  "Inappropriate Content",
  "Spam",
  "Other Issue",
]

export function ReportAbusePage() {
  const [reportType, setReportType] = useState(REPORT_TYPES[0])
  const [submitting, setSubmitting] = useState(false)

  const handleReportSubmit = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault()
    const form = event.currentTarget
    const data = Object.fromEntries(new FormData(form))
    setSubmitting(true)
    try {
      await submitReport({
        type: reportType,
        email: String(data.email ?? ""),
        links: String(data.links ?? ""),
        website: String(data.website ?? ""),
      })
      toast.success("Report submitted. Thank you!")
      form.reset()
      setReportType(REPORT_TYPES[0])
    } catch (e) {
      toast.error(
        e instanceof ApiError ? e.message : "Something went wrong. Please try again."
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section>
      <h1 className="text-4xl font-semibold tracking-tight">Report abuse</h1>
      <p className="text-muted-foreground mt-3 max-w-prose text-sm">
        If you encounter a note that you believe violates our Terms of Service,
        you can use the form below to submit the note for review. We will
        evaluate the report and take appropriate action, including removal of
        the content if necessary.
      </p>

      <Alert className="mt-5 border-blue-500/50 bg-blue-500/10 text-blue-700 dark:text-blue-400 [&>svg]:text-current *:data-[slot=alert-description]:text-blue-700/90 dark:*:data-[slot=alert-description]:text-blue-400/90">
        <Info />
        <AlertTitle>Heads up</AlertTitle>
        <AlertDescription>
          <p>
            This is not a contact form for the person or organization that
            created the note.
          </p>
        </AlertDescription>
      </Alert>

      <form
        className="mt-6 flex max-w-xl flex-col gap-5"
        onSubmit={handleReportSubmit}
      >
        <div className="flex flex-col gap-2">
          <Label htmlFor="report-type">Type</Label>
          <Select value={reportType} onValueChange={setReportType}>
            <SelectTrigger id="report-type" className="w-full">
              <SelectValue placeholder="Type" />
            </SelectTrigger>
            <SelectContent>
              {REPORT_TYPES.map((option) => (
                <SelectItem key={option} value={option}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="report-links">Link to Note(s)</Label>
          <Textarea
            id="report-links"
            name="links"
            placeholder="https://..."
            className="min-h-24"
            required
          />
        </div>

        <div className="flex flex-col gap-2">
          <Label htmlFor="report-email">Your email</Label>
          <Input
            id="report-email"
            name="email"
            type="email"
            placeholder="you@example.com"
            required
          />
        </div>

        <HoneypotField />

        <Button
          type="submit"
          disabled={submitting}
          className="sm:self-start sm:px-8"
        >
          {submitting ? "Reporting..." : "Report"}
        </Button>
      </form>
    </section>
  )
}

export default ReportAbusePage
