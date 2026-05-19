"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Loader2Icon } from "lucide-react";
import { useAuth } from "@/hooks/use-auth";

import { AppSidebar } from "@/components/app-sidebar";
import { SiteHeader } from "@/components/site-header";
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";

function useAuthGuard(): "loading" | "authenticated" {
  const router = useRouter();
  const { isPending, isError } = useAuth();

  useEffect(() => {
    if (isError) {
      router.replace("/login");
    }
  }, [isError, router]);

  if (isPending || isError) return "loading";
  return "authenticated";
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const status = useAuthGuard();

  if (status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2Icon className="size-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <SidebarProvider
      style={
        {
          "--sidebar-width": "calc(var(--spacing) * 72)",
          "--header-height": "calc(var(--spacing) * 12)",
        } as React.CSSProperties
      }
    >
      <AppSidebar variant="inset" />
      <SidebarInset>
        <SiteHeader />
        <div className="flex flex-1 flex-col">
          <div className="@container/main flex flex-1 flex-col gap-2">
            {children}
          </div>
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
