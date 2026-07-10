import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { NAV_LINKS } from '@/consts'
import { requestSignIn, signOut, useAuth } from '@/lib/auth'
import { LogIn, LogOut, Menu } from 'lucide-react'

const MobileMenu = () => {
  const [isOpen, setIsOpen] = useState(false)
  const { user, signedIn } = useAuth()

  useEffect(() => {
    const handleViewTransitionStart = () => {
      setIsOpen(false)
    }

    document.addEventListener('astro:before-swap', handleViewTransitionStart)

    return () => {
      document.removeEventListener(
        'astro:before-swap',
        handleViewTransitionStart,
      )
    }
  }, [])

  return (
    <DropdownMenu open={isOpen} onOpenChange={(val) => setIsOpen(val)}>
      <DropdownMenuTrigger
        asChild
        onClick={() => {
          setIsOpen((val) => !val)
        }}
      >
        <Button
          variant="ghost"
          size="icon"
          className="size-8 sm:hidden"
          title="Menu"
        >
          <Menu className="size-5" />
          <span className="sr-only">Toggle menu</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="bg-background">
        {NAV_LINKS.map((item) => (
          <DropdownMenuItem key={item.href} asChild>
            <a
              href={item.href}
              className="w-full text-lg font-medium capitalize"
              onClick={() => setIsOpen(false)}
            >
              {item.label}
            </a>
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        {signedIn && user ? (
          <>
            <DropdownMenuLabel className="font-normal">
              {user.name && (
                <div className="text-sm font-medium">{user.name}</div>
              )}
              <div className="text-muted-foreground max-w-48 truncate text-xs">
                {user.email}
              </div>
            </DropdownMenuLabel>
            <DropdownMenuItem
              onSelect={() => {
                setIsOpen(false)
                signOut()
                toast.success('Signed out')
              }}
            >
              <LogOut />
              Sign out
            </DropdownMenuItem>
          </>
        ) : (
          <DropdownMenuItem
            onSelect={() => {
              setIsOpen(false)
              requestSignIn()
            }}
          >
            <LogIn />
            Sign in
          </DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export default MobileMenu
