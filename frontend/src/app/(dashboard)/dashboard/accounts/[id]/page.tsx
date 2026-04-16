"use client";

import { use, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeftIcon, PencilIcon, StarIcon, Trash2Icon } from "lucide-react";

import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { EditProfileDialog } from "@/components/edit-profile-dialog";
import { StatCard } from "@/components/shared/stat-card";
import { TokenStatusBadge } from "@/components/shared/status-badge";
import { ListingsTable } from "@/components/listings/listings-table";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Progress,
  ProgressIndicator,
  ProgressLabel,
  ProgressTrack,
  ProgressValue,
} from "@/components/ui/progress";
import { useActiveAccount } from "@/hooks/use-active-account";
import {
  useListingLimits,
  useRefreshLimits,
} from "@/hooks/use-listing-limits";
import { useAllListings } from "@/hooks/use-listings";
import {
  useDeleteOlxAccount,
  useOlxAccount,
} from "@/hooks/use-olx-accounts";
import { toastMessages } from "@/lib/toast-messages";

const LIMIT_LABELS: Record<string, string> = {
  cars: "Automobili",
  real_estate: "Nekretnine",
  other: "Ostalo",
};

export default function AccountDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const accountId = Number(id);
  const router = useRouter();

  const account = useOlxAccount(accountId);
  const listings = useAllListings();
  const limits = useListingLimits(accountId);
  const refresh = useRefreshLimits(accountId);
  const deleteAccount = useDeleteOlxAccount();
  const { account: active, setAccount } = useActiveAccount();

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const scoped = useMemo(
    () =>
      (listings.data ?? []).filter(
        (l) => (l as { account_id?: number }).account_id === accountId
      ),
    [listings.data, accountId]
  );

  const countBy = (status: string) =>
    scoped.filter((l) => l.status === status).length;

  if (account.isLoading) {
    return (
      <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
        <p className="text-sm text-muted-foreground">Učitavanje…</p>
      </div>
    );
  }

  if (account.isError || !account.data) {
    return (
      <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
        <Button variant="ghost" render={<Link href="/dashboard/accounts" />}>
          <ArrowLeftIcon />
          Nazad
        </Button>
        <p className="text-sm text-destructive">Profil nije pronađen.</p>
      </div>
    );
  }

  const handleDelete = () => {
    deleteAccount.mutate(accountId, {
      onSuccess: () => {
        toast.success(toastMessages.deleted);
        if (active?.id === accountId) setAccount(null);
        router.push("/dashboard/accounts");
      },
      onError: (err) => {
        toast.error(err.message || toastMessages.errorDelete);
      },
    });
  };

  const isActive = active?.id === accountId;

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            render={<Link href="/dashboard/accounts" />}
          >
            <ArrowLeftIcon />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">{account.data.username}</h1>
            <div className="mt-1 flex items-center gap-2 text-sm text-muted-foreground">
              <span>OLX ID: {account.data.olx_user_id ?? "—"}</span>
              <TokenStatusBadge expiresAt={account.data.token_expires_at} />
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant={isActive ? "secondary" : "outline"}
            onClick={() => setAccount(account.data ?? null)}
            disabled={isActive}
          >
            <StarIcon />
            {isActive ? "Aktivan" : "Aktiviraj"}
          </Button>
          <Button variant="outline" onClick={() => setEditOpen(true)}>
            <PencilIcon />
            Uredi
          </Button>
          <Button
            variant="destructive"
            onClick={() => setDeleteOpen(true)}
          >
            <Trash2Icon />
            Obriši
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 @xl/main:grid-cols-4">
        <StatCard label="Aktivni" value={countBy("active")} />
        <StatCard label="Drafts" value={countBy("draft")} />
        <StatCard label="Završeni" value={countBy("finished")} />
        <StatCard label="Istekli" value={countBy("expired")} />
      </div>

      <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-3">
        {(["cars", "real_estate", "other"] as const).map((key) => {
          const info = limits.data?.[key];
          const used = info?.used ?? 0;
          const total = info?.limit ?? 0;
          const pct = total > 0 ? Math.round((used / total) * 100) : 0;
          return (
            <Card key={key}>
              <CardHeader>
                <CardDescription>{LIMIT_LABELS[key]}</CardDescription>
                <CardTitle className="text-2xl tabular-nums">
                  {used} / {total}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Progress value={pct}>
                  <ProgressLabel>Iskorišteno</ProgressLabel>
                  <ProgressValue>{() => `${pct}%`}</ProgressValue>
                  <ProgressTrack>
                    <ProgressIndicator />
                  </ProgressTrack>
                </Progress>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <Card>
        <CardHeader>
          <CardDescription>Osvježavanja</CardDescription>
          <CardTitle>
            {refresh.data?.free_count ?? 0} / {refresh.data?.free_limit ?? 0}{" "}
            besplatnih
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-3 text-sm @xl/main:grid-cols-3">
          <div className="rounded-md border p-3">
            <div className="text-xs text-muted-foreground">
              Plaćena osvježavanja
            </div>
            <div className="text-lg font-semibold tabular-nums">
              {refresh.data?.paid_count ?? 0}
            </div>
          </div>
          <div className="rounded-md border p-3">
            <div className="text-xs text-muted-foreground">
              Artikli pod profilom
            </div>
            <div className="text-lg font-semibold tabular-nums">
              {refresh.data?.listing_count ?? scoped.length}
            </div>
          </div>
          <div className="rounded-md border p-3">
            <div className="text-xs text-muted-foreground">Iskorišteno %</div>
            <div className="text-lg font-semibold tabular-nums">
              {refresh.data && refresh.data.free_limit > 0
                ? Math.round(
                    (refresh.data.free_count / refresh.data.free_limit) * 100
                  )
                : 0}
              %
            </div>
          </div>
        </CardContent>
      </Card>

      <ListingsTable
        listings={scoped}
        isLoading={listings.isLoading}
        isError={listings.isError}
        hasAccount
        total={scoped.length}
      />

      <EditProfileDialog
        account={account.data}
        open={editOpen}
        onOpenChange={setEditOpen}
      />
      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Obriši OLX profil"
        description={`Profil "${account.data.username}" će biti trajno obrisan.`}
        confirmLabel="Obriši"
        destructive
        loading={deleteAccount.isPending}
        onConfirm={handleDelete}
      />
    </div>
  );
}
