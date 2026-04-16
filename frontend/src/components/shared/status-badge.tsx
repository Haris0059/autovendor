"use client";

import { useEffect, useState } from "react";

import { Badge } from "@/components/ui/badge";

export type OlxStatus =
  | "active"
  | "draft"
  | "expired"
  | "hidden"
  | "finished"
  | string;

const OLX_STATUS_LABEL: Record<string, string> = {
  active: "Aktivan",
  draft: "Draft",
  expired: "Istekao",
  hidden: "Sakriven",
  finished: "Završen",
};

const OLX_STATUS_VARIANT: Record<
  string,
  "default" | "secondary" | "destructive" | "outline"
> = {
  active: "default",
  draft: "secondary",
  expired: "destructive",
  hidden: "destructive",
  finished: "outline",
};

export function OlxStatusBadge({ status }: { status: string }) {
  return (
    <Badge variant={OLX_STATUS_VARIANT[status] ?? "secondary"}>
      {OLX_STATUS_LABEL[status] ?? status}
    </Badge>
  );
}

export function StatusBadge({ status }: { status: "active" | "inactive" | "pending" | string }) {
  const variants: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
    active: "default",
    inactive: "destructive",
    pending: "outline",
  };
  const labels: Record<string, string> = {
    active: "Aktivan",
    inactive: "Neaktivan",
    pending: "Na čekanju",
  };

  return (
    <Badge variant={variants[status] ?? "outline"}>
      {labels[status] ?? status}
    </Badge>
  );
}

type SyncStatus = "success" | "failed" | "skipped" | "pending";

const SYNC_STATUS_LABEL: Record<SyncStatus, string> = {
  success: "Uspješno",
  failed: "Neuspješno",
  skipped: "Preskočeno",
  pending: "U toku",
};

const SYNC_STATUS_VARIANT: Record<
  SyncStatus,
  "default" | "secondary" | "destructive" | "outline"
> = {
  success: "default",
  failed: "destructive",
  skipped: "outline",
  pending: "secondary",
};

export function SyncStatusBadge({ status }: { status: SyncStatus }) {
  return (
    <Badge variant={SYNC_STATUS_VARIANT[status]}>
      {SYNC_STATUS_LABEL[status]}
    </Badge>
  );
}

export function TokenStatusBadge({ expiresAt }: { expiresAt: string | null }) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return <Badge variant="secondary">—</Badge>;

  // eslint-disable-next-line react-hooks/purity -- token validity is a time-based snapshot, gated behind `mounted` so SSR is stable
  const expired = !expiresAt || new Date(expiresAt).getTime() < Date.now();
  return (
    <Badge variant={expired ? "destructive" : "default"}>
      {expired ? "Token istekao" : "Token važeći"}
    </Badge>
  );
}
