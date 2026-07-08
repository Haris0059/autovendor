"use client";

import Link from "next/link";
import { PlusIcon, SearchIcon, UserIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { OlxAccount } from "@/types/olx";

interface ListingsToolbarProps {
  accounts: OlxAccount[];
  activeAccountId: number | null;
  onAccountChange: (id: number) => void;
  search: string;
  onSearchChange: (value: string) => void;
  perPage: string;
  onPerPageChange: (value: string) => void;
}

export function ListingsToolbar({
  accounts,
  activeAccountId,
  onAccountChange,
  search,
  onSearchChange,
  perPage,
  onPerPageChange,
}: ListingsToolbarProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Select
        items={accounts.map((a) => ({ value: String(a.id), label: a.username }))}
        value={activeAccountId ? String(activeAccountId) : ""}
        onValueChange={(v) => v && onAccountChange(Number(v))}
      >
        <SelectTrigger className="min-w-48">
          <UserIcon className="size-4 text-muted-foreground" />
          <SelectValue placeholder="Odaberite profil" />
        </SelectTrigger>
        <SelectContent>
          {accounts.length === 0 ? (
            <div className="px-2 py-1.5 text-sm text-muted-foreground">
              Nema profila
            </div>
          ) : (
            accounts.map((account) => (
              <SelectItem key={account.id} value={String(account.id)}>
                {account.username}
              </SelectItem>
            ))
          )}
        </SelectContent>
      </Select>

      <div className="relative">
        <SearchIcon className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Pretraži artikle..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className="w-52 pl-8"
        />
      </div>

      <div className="ml-auto flex items-center gap-2">
        <span className="text-sm text-muted-foreground">Po stranici:</span>
        <Select value={perPage} onValueChange={(v) => v && onPerPageChange(v)}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="10">10</SelectItem>
            <SelectItem value="25">25</SelectItem>
            <SelectItem value="50">50</SelectItem>
          </SelectContent>
        </Select>
        <Button render={<Link href="/listings/new" />}>
          <PlusIcon />
          Novi artikal
        </Button>
      </div>
    </div>
  );
}
