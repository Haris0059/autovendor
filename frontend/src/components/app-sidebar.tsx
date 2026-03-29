"use client"

import * as React from "react"

import { NavMain } from "@/components/nav-main"
import { NavSection } from "@/components/nav-section"
import { NavSecondary } from "@/components/nav-secondary"
import { NavUser } from "@/components/nav-user"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar"
import {
  LayoutDashboardIcon,
  PackageIcon,
  UsersIcon,
  ShoppingCartIcon,
  RefreshCwIcon,
  HistoryIcon,
  TagsIcon,
  MegaphoneIcon,
  Settings2Icon,
  CircleHelpIcon,
  SearchIcon,
  StoreIcon,
  PlusCircleIcon,
  MapIcon,
} from "lucide-react"

const data = {
  user: {
    name: "User",
    email: "user@example.com",
    avatar: "/avatars/default.jpg",
  },
  navMain: [
    {
      title: "Dashboard",
      url: "/dashboard",
      icon: <LayoutDashboardIcon />,
    },
  ],
  navOlx: [
    {
      title: "OLX Profili",
      url: "/dashboard/accounts",
      icon: <UsersIcon />,
    },
    {
      title: "Artikli",
      url: "/listings",
      icon: <PackageIcon />,
    },
    {
      title: "Dodavanje Artikala",
      url: "/listings/new",
      icon: <PlusCircleIcon />,
    },
    {
      title: "Kategorije",
      url: "/sync/mappings",
      icon: <TagsIcon />,
    },
    {
      title: "Sponzorisano",
      url: "/sponsored",
      icon: <MegaphoneIcon />,
    },
  ],
  navWoo: [
    {
      title: "WooCommerce",
      url: "/woocommerce",
      icon: <ShoppingCartIcon />,
    },
    {
      title: "Mapiranje Artikala",
      url: "/sync/mappings",
      icon: <MapIcon />,
    },
    {
      title: "Sync",
      url: "/sync",
      icon: <RefreshCwIcon />,
    },
    {
      title: "Logovi",
      url: "/sync/history",
      icon: <HistoryIcon />,
    },
  ],
  navSecondary: [
    {
      title: "Postavke",
      url: "/settings",
      icon: <Settings2Icon />,
    },
    {
      title: "Pomoć",
      url: "#",
      icon: <CircleHelpIcon />,
    },
    {
      title: "Pretraga",
      url: "#",
      icon: <SearchIcon />,
    },
  ],
}

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="offcanvas" {...props}>
      <SidebarHeader>
        <SidebarMenu>
          <SidebarMenuItem>
            <SidebarMenuButton
              className="data-[slot=sidebar-menu-button]:p-1.5!"
              render={<a href="/dashboard" />}
            >
              <StoreIcon className="size-5!" />
              <span className="text-base font-semibold">AutoVendor</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
        <NavSection label="OLX" items={data.navOlx} />
        <NavSection label="WooCommerce" items={data.navWoo} />
        <NavSecondary items={data.navSecondary} className="mt-auto" />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={data.user} />
      </SidebarFooter>
    </Sidebar>
  )
}
