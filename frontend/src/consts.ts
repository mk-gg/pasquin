import type { IconMap, SocialLink, Site } from '@/types'

export const SITE: Site = {
  title: 'Pasquin',
  description:
    'Anonymous notes, shared instantly.',
  href: 'https://pasquin.mkgg.dev',
  author: 'mk-gg',
  locale: 'en-US',
  featuredPostCount: 2,
  postsPerPage: 3,
}

export const NAV_LINKS: SocialLink[] = [
  {
    href: '/blog',
    label: 'blog',
  },
  {
    href: '/contact',
    label: 'contact',
  },
]

export const SOCIAL_LINKS: SocialLink[] = [
  {
    href: 'https://github.com/mk-gg',
    label: 'GitHub',
  },
  {
    href: 'mailto:hello@mkgg.dev',
    label: 'Email',
  },
  {
    href: '/rss.xml',
    label: 'RSS',
  },
]

export const ICON_MAP: IconMap = {
  Website: 'lucide:globe',
  GitHub: 'lucide:github',
  LinkedIn: 'lucide:linkedin',
  Twitter: 'lucide:twitter',
  Email: 'lucide:mail',
  RSS: 'lucide:rss',
}
