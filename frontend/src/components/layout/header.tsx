"use client";

import { AccountSwitcher } from "./account-switcher";
import { MobileNav } from "./mobile-nav";

export function Header() {
  return (
    <header className="flex h-16 items-center justify-between border-b px-6">
      <MobileNav />
      <div className="flex items-center gap-4 ml-auto">
        <AccountSwitcher />
      </div>
    </header>
  );
}
