"use client";

import { SearchIcon } from "lucide-react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface PageToolbarProps {
  search?: {
    value: string;
    onChange: (value: string) => void;
    placeholder?: string;
  };
  leading?: React.ReactNode;
  trailing?: React.ReactNode;
  className?: string;
}

export function PageToolbar({
  search,
  leading,
  trailing,
  className,
}: PageToolbarProps) {
  return (
    <div className={cn("flex flex-wrap items-center gap-3", className)}>
      {leading}
      {search ? (
        <div className="relative">
          <SearchIcon className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder={search.placeholder ?? "Pretraži..."}
            value={search.value}
            onChange={(e) => search.onChange(e.target.value)}
            className="w-52 pl-8"
          />
        </div>
      ) : null}
      {trailing ? (
        <div className="ml-auto flex items-center gap-2">{trailing}</div>
      ) : null}
    </div>
  );
}
