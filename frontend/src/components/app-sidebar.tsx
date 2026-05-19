"use client"

import * as React from "react"
import Link from "next/link"

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
  CircleHelpIcon,
  StoreIcon,
  PlusCircleIcon,
  SettingsIcon,
  BarChart3Icon,
} from "lucide-react"

const data = {
  navMain: [
    {
      title: "Dashboard",
      url: "/dashboard",
      icon: <LayoutDashboardIcon />,
    },
    {
      title: "Analitika",
      url: "/analytics",
      icon: <BarChart3Icon />,
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
      title: "Novi Artikal",
      url: "/listings/new",
      icon: <PlusCircleIcon />,
    },
    {
      title: "Sponzorisano",
      url: "/sponsored",
      icon: <MegaphoneIcon />,
    },
  ],
  navSync: [
    {
      title: "WooCommerce",
      url: "/woocommerce",
      icon: <ShoppingCartIcon />,
    },
    {
      title: "Sinhronizacija",
      url: "/sync",
      icon: <RefreshCwIcon />,
    },
    {
      title: "Mapiranje kategorija",
      url: "/sync/mappings",
      icon: <TagsIcon />,
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
      icon: <SettingsIcon />,
    },
    {
      title: "Pomoć i podrška",
      url: "#",
      icon: <CircleHelpIcon />,
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
              render={<Link href="/dashboard" />}
            >
              <div className="flex aspect-square size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <StoreIcon className="size-5!" />
              </div>
              <span className="text-base font-bold">AutoVendor</span>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
        <NavSection label="OLX Prodaja" items={data.navOlx} />
        <NavSection label="Integracija" items={data.navSync} />
        <NavSecondary items={data.navSecondary} className="mt-auto" />
      </SidebarContent>
      <SidebarFooter>
        <NavUser />
      </SidebarFooter>
    </Sidebar>
  )
}
