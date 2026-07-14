"use client";

import { UserIcon } from "lucide-react";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useActiveAccount, useActiveAccountSync } from "@/hooks/use-active-account";
import { useOlxAccounts } from "@/hooks/use-olx-accounts";
import { cn } from "@/lib/utils";

interface AccountSelectProps {
  className?: string;
  placeholder?: string;
}

export function AccountSelect({
  className,
  placeholder = "Odaberite profil",
}: AccountSelectProps) {
  const { data: accounts = [] } = useOlxAccounts();
  const { account, setAccount } = useActiveAccount();
  useActiveAccountSync();

  const value = account ? String(account.id) : "";

  return (
    <Select
      items={accounts.map((a) => ({ value: String(a.id), label: a.username }))}
      value={value}
      onValueChange={(v) => {
        const next = accounts.find((a) => String(a.id) === v) ?? null;
        setAccount(next);
      }}
    >
      <SelectTrigger className={cn("min-w-48", className)}>
        <UserIcon className="size-4 text-muted-foreground" />
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {accounts.length === 0 ? (
          <div className="px-2 py-1.5 text-sm text-muted-foreground">
            Nema profila
          </div>
        ) : (
          accounts.map((a) => (
            <SelectItem key={a.id} value={String(a.id)}>
              {a.username}
            </SelectItem>
          ))
        )}
      </SelectContent>
    </Select>
  );
}
