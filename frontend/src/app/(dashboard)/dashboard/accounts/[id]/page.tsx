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
import { PageHeader } from "@/components/shared/page-header";

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
        (l) => l.account_id === accountId
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
      <PageHeader
        title={account.data.username}
        description={`OLX ID: ${account.data.olx_user_id ?? "—"}`}
      >
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            className="mr-2"
            render={<Link href="/dashboard/accounts" />}
          >
            <ArrowLeftIcon />
          </Button>
          <TokenStatusBadge expiresAt={account.data.token_expires_at} />
          <div className="ml-4 flex items-center gap-2">
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
      </PageHeader>

      <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-2 @5xl/main:grid-cols-4">
        <StatCard
          label="Aktivni artikli"
          value={countBy("active")}
          isLoading={listings.isLoading}
        />
        <StatCard
          label="Drafts"
          value={countBy("draft")}
          isLoading={listings.isLoading}
        />
        <StatCard
          label="Istekli"
          value={countBy("expired")}
          isLoading={listings.isLoading}
        />
        <StatCard
          label="Završeni"
          value={countBy("finished")}
          isLoading={listings.isLoading}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 @4xl/main:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Limiti objavljivanja</CardTitle>
            <CardDescription>
              Preostali broj besplatnih objava po kategorijama.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-6">
            {Object.entries(limits.data ?? {}).map(([key, value]) => (
              <Progress key={key} value={(value.used / value.limit) * 100}>
                <ProgressLabel>{LIMIT_LABELS[key] ?? key}</ProgressLabel>
                <ProgressValue>
                  {() => `${value.used} / ${value.limit}`}
                </ProgressValue>
                <ProgressTrack>
                  <ProgressIndicator />
                </ProgressTrack>
              </Progress>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Budžet za obnavljanje</CardTitle>
            <CardDescription>
              Pregled preostalih besplatnih i plaćenih obnova.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-6">
            <Progress
              value={
                ((refresh.data?.free_limit ?? 0) -
                  (refresh.data?.free_count ?? 0) /
                    (refresh.data?.free_limit ?? 1)) *
                100
              }
            >
              <ProgressLabel>Besplatne obnove</ProgressLabel>
              <ProgressValue>
                {() => `${refresh.data?.free_count} / ${refresh.data?.free_limit}`}
              </ProgressValue>
              <ProgressTrack>
                <ProgressIndicator />
              </ProgressTrack>
            </Progress>
            <div className="flex items-center justify-between rounded-lg border p-4">
              <div className="flex flex-col gap-1">
                <span className="text-sm font-medium">Plaćene obnove</span>
                <span className="text-xs text-muted-foreground">
                  Dostupne za sve artikle
                </span>
              </div>
              <span className="text-2xl font-bold">
                {refresh.data?.paid_count ?? 0}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      <ListingsTable
        listings={scoped}
        isLoading={listings.isLoading}
        isError={listings.isError}
        hasAccount={true}
        total={scoped.length}
        enableRowActions
      />

      <EditProfileDialog
        open={editOpen}
        onOpenChange={setEditOpen}
        account={account.data}
      />

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Obriši profil"
        description={`Jeste li sigurni da želite obrisati profil "${account.data.username}"? Ova akcija je nepovratna.`}
        confirmLabel="Obriši"
        destructive
        onConfirm={handleDelete}
      />
    </div>
  );
}
