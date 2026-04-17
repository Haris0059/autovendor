"use client";

import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  EllipsisVerticalIcon,
  BellIcon,
  LogOutIcon,
  SettingsIcon,
  UserIcon,
} from "lucide-react";

import {
  Avatar,
  AvatarFallback,
  AvatarImage,
} from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { useAuth, useLogout } from "@/hooks/use-auth";
import { toastMessages } from "@/lib/toast-messages";

function initials(name?: string, email?: string): string {
  if (name) {
    return name
      .split(" ")
      .map((p) => p[0])
      .slice(0, 2)
      .join("")
      .toUpperCase();
  }
  return (email ?? "?").slice(0, 2).toUpperCase();
}

export function NavUser() {
  const { isMobile } = useSidebar();
  const router = useRouter();
  const { data: user } = useAuth();
  const logout = useLogout();

  const displayName = user?.name ?? user?.email ?? "Korisnik";
  const displayEmail = user?.email ?? "";

  const handleLogout = () => {
    logout.mutate(undefined, {
      onSuccess: () => {
        toast.success(toastMessages.logoutSuccess);
        router.replace("/login");
      },
    });
  };

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger
            nativeButton
            render={
              <SidebarMenuButton size="lg" className="aria-expanded:bg-muted" />
            }
          >
            <Avatar className="size-8 rounded-lg grayscale">
              <AvatarImage src={undefined} alt={displayName} />
              <AvatarFallback className="rounded-lg">
                {initials(user?.name, user?.email)}
              </AvatarFallback>
            </Avatar>
            <div className="grid flex-1 text-left text-sm leading-tight">
              <span className="truncate font-medium">{displayName}</span>
              <span className="truncate text-xs text-foreground/70">
                {displayEmail}
              </span>
            </div>
            <EllipsisVerticalIcon className="ml-auto size-4" />
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="min-w-56"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={4}
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                  <Avatar className="size-8">
                    <AvatarFallback className="rounded-lg">
                      {initials(user?.name, user?.email)}
                    </AvatarFallback>
                  </Avatar>
                  <div className="grid flex-1 text-left text-sm leading-tight">
                    <span className="truncate font-medium">{displayName}</span>
                    <span className="truncate text-xs text-muted-foreground">
                      {displayEmail}
                    </span>
                  </div>
                </div>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem onClick={() => router.push("/settings")}>
                <UserIcon />
                Profil
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => router.push("/settings")}>
                <SettingsIcon />
                Postavke
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => router.push("/settings")}>
                <BellIcon />
                Notifikacije
              </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} disabled={logout.isPending}>
              <LogOutIcon />
              Odjavi se
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
