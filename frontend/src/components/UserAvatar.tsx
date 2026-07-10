import { useEffect, useState } from "react"

import type { AuthUser } from "@/lib/auth"
import { cn } from "@/lib/utils"

/**
 * Round avatar for the signed-in account: the Google profile picture when
 * available, otherwise the first letter of the name/email. Google's avatar
 * CDN rejects requests that carry a referrer, hence no-referrer; a failed
 * load falls back to the initial.
 */
export function UserAvatar({
  user,
  className,
}: {
  user: AuthUser
  className?: string
}) {
  const [failed, setFailed] = useState(false)

  // Retry the image if a new picture URL arrives (e.g. after re-sign-in).
  useEffect(() => {
    setFailed(false)
  }, [user.picture])

  if (user.picture && !failed) {
    return (
      <img
        src={user.picture}
        alt=""
        referrerPolicy="no-referrer"
        onError={() => setFailed(true)}
        className={cn("size-7 shrink-0 rounded-full", className)}
      />
    )
  }

  return (
    <span
      className={cn(
        "bg-primary text-primary-foreground flex size-7 shrink-0 items-center justify-center rounded-full text-xs font-medium",
        className
      )}
    >
      {(user.name || user.email).charAt(0).toUpperCase()}
    </span>
  )
}

export default UserAvatar
