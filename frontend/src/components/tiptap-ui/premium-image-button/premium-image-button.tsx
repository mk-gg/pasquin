import { useRef, useState } from "react"
import { ImagePlus, Sparkles } from "lucide-react"
import { toast } from "sonner"

import { ApiError, createCheckout, uploadImage } from "@/lib/api"
import { getToken, requestSignIn, useAuth } from "@/lib/auth"
import { Button } from "@/components/tiptap-ui-primitive/button"
import { Button as UiButton } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { useTiptapEditor } from "@/hooks/use-tiptap-editor"

const MAX_IMAGE_BYTES = 5 * 1024 * 1024

/**
 * Toolbar button that inserts an image at the cursor. Signed-out users are
 * asked to sign in; free users get the premium upsell; premium users get a
 * file picker and the image is uploaded, then embedded by URL.
 */
export function PremiumImageButton() {
  const { editor } = useTiptapEditor()
  const { user, signedIn } = useAuth()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [upsellOpen, setUpsellOpen] = useState(false)
  const [busy, setBusy] = useState(false)

  const handleClick = () => {
    if (!signedIn) {
      requestSignIn()
      return
    }
    if (!user?.premium) {
      setUpsellOpen(true)
      return
    }
    fileInputRef.current?.click()
  }

  const handleFile = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ""
    const token = getToken()
    if (!file || !editor || !token) return
    if (!file.type.startsWith("image/")) {
      toast.error("Please choose an image file")
      return
    }
    if (file.size > MAX_IMAGE_BYTES) {
      toast.error("Images can be up to 5 MB")
      return
    }
    setBusy(true)
    try {
      const { url } = await uploadImage(token, file)
      editor.chain().focus().setImage({ src: url }).run()
    } catch (e) {
      toast.error(
        e instanceof ApiError ? e.message : "Upload failed. Please try again."
      )
    } finally {
      setBusy(false)
    }
  }

  const handleUpgrade = async () => {
    const token = getToken()
    if (!token) return
    setBusy(true)
    try {
      const { url } = await createCheckout(token)
      // Polar's hosted checkout is an external page; a full navigation is correct.
      window.location.assign(url)
    } catch (e) {
      toast.error(
        e instanceof ApiError
          ? e.message
          : "Could not start checkout. Please try again."
      )
      setBusy(false)
    }
  }

  return (
    <>
      <Button
        type="button"
        data-style="ghost"
        role="button"
        tabIndex={-1}
        aria-label="Insert image"
        tooltip="Insert image (premium)"
        disabled={busy}
        onClick={handleClick}
      >
        <ImagePlus className="tiptap-button-icon" />
      </Button>

      <input
        ref={fileInputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp,image/gif"
        className="hidden"
        onChange={handleFile}
      />

      <Dialog open={upsellOpen} onOpenChange={setUpsellOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="size-4 text-amber-500" />
              Images are a premium feature
            </DialogTitle>
            <DialogDescription>
              Upgrade once and add images (up to 5 MB each) to your notes,
              stored securely and served fast from the CDN.
            </DialogDescription>
          </DialogHeader>
          <UiButton onClick={handleUpgrade} disabled={busy}>
            {busy ? "Redirecting..." : "Upgrade"}
          </UiButton>
        </DialogContent>
      </Dialog>
    </>
  )
}

export default PremiumImageButton
