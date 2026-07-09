import { Toaster as Sonner, type ToasterProps } from "sonner"

/**
 * App-wide toast outlet, themed with the site tokens. Top-right on desktop;
 * sonner automatically renders toasts full-width at the top on small screens.
 */
const Toaster = ({ ...props }: ToasterProps) => {
  return (
    <Sonner
      position="top-right"
      className="toaster group"
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
        } as React.CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
