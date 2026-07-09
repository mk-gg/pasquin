import { useState } from "react"
import { Flag, Mail } from "lucide-react"
import { toast } from "sonner"

import { ApiError, submitContact } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { HoneypotField } from "@/components/HoneypotField"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const CONTACT_REASONS = [
  "General inquiry",
  "Technical support",
  "Feature request",
  "Partnership",
]

export function ContactPage() {
  const [reason, setReason] = useState(CONTACT_REASONS[0])
  const [submitting, setSubmitting] = useState(false)

  const handleContactSubmit = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault()
    const form = event.currentTarget
    const data = Object.fromEntries(new FormData(form))
    setSubmitting(true)
    try {
      await submitContact({
        name: String(data.name ?? ""),
        email: String(data.email ?? ""),
        reason,
        message: String(data.message ?? ""),
        website: String(data.website ?? ""),
      })
      toast.success("Message sent. We'll get back to you soon!")
      form.reset()
      setReason(CONTACT_REASONS[0])
    } catch (e) {
      toast.error(
        e instanceof ApiError ? e.message : "Something went wrong. Please try again."
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="grid gap-10 md:grid-cols-[1fr_15rem]">
      <section>
        <h1 className="text-4xl font-semibold tracking-tight">Get in touch</h1>
        <p className="text-muted-foreground mt-3 text-sm">
          Got a question, a bug, a feature request or just want to say hello?
          Drop us a message and we&apos;ll get back to you as soon as we can.
        </p>

        <form
          className="mt-8 flex flex-col gap-5"
          onSubmit={handleContactSubmit}
        >
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor="contact-name">Name</Label>
              <Input
                id="contact-name"
                name="name"
                placeholder="Your name"
                required
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="contact-email">Email</Label>
              <Input
                id="contact-email"
                name="email"
                type="email"
                placeholder="you@example.com"
                required
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="contact-reason">Reason for contact</Label>
            <Select value={reason} onValueChange={setReason}>
              <SelectTrigger id="contact-reason" className="w-full">
                <SelectValue placeholder="Reason for contact" />
              </SelectTrigger>
              <SelectContent>
                {CONTACT_REASONS.map((option) => (
                  <SelectItem key={option} value={option}>
                    {option}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="contact-message">How can we help?</Label>
            <Textarea
              id="contact-message"
              name="message"
              placeholder="Tell us a bit more..."
              className="min-h-32"
              required
            />
          </div>

          <HoneypotField />

          <Button
            type="submit"
            disabled={submitting}
            className="sm:self-start sm:px-8"
          >
            {submitting ? "Sending..." : "Send Message"}
          </Button>
        </form>
      </section>

      <aside className="flex flex-col gap-3">
        <h2 className="text-sm font-medium">Other links</h2>
        <Button variant="outline" className="justify-start" asChild>
          <a href="mailto:hello@placeholder.com">
            <Mail />
            hello@placeholder.com
          </a>
        </Button>
        <Button variant="outline" className="justify-start" asChild>
          <a href="/abuse">
            <Flag />
            Report Abuse
          </a>
        </Button>
      </aside>
    </div>
  )
}

export default ContactPage
