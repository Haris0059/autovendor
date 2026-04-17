"use client";

import { useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import {
  MoreHorizontalIcon,
  PencilIcon,
  PlusIcon,
  StarIcon,
  Trash2Icon,
  UserIcon,
} from "lucide-react";
import type { ColumnDef } from "@tanstack/react-table";

import { AddProfileDialog } from "@/components/add-profile-dialog";
import { EditProfileDialog } from "@/components/edit-profile-dialog";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { DataTable } from "@/components/shared/data-table";
import { EmptyState } from "@/components/shared/empty-state";
import { TokenStatusBadge } from "@/components/shared/status-badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useActiveAccount } from "@/hooks/use-active-account";
import { useAllListings } from "@/hooks/use-listings";
import {
  useDeleteOlxAccount,
  useOlxAccounts,
} from "@/hooks/use-olx-accounts";
import { toastMessages } from "@/lib/toast-messages";
import type { OlxAccount } from "@/types/olx";
import { PageHeader } from "@/components/shared/page-header";

function formatDate(value: string): string {
  try {
    return new Date(value).toLocaleDateString("bs-BA");
  } catch {
    return value;
  }
}

export default function AccountsPage() {
  const { data: accounts = [], isLoading, isError } = useOlxAccounts();
  const allListings = useAllListings();
  const deleteAccount = useDeleteOlxAccount();
  const { account: active, setAccount } = useActiveAccount();

  const [addOpen, setAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<OlxAccount | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<OlxAccount | null>(null);

  const countFor = (id: number) =>
    (allListings.data ?? []).filter(
      (l) => (l as { account_id?: number }).account_id === id
    ).length;

  const columns: ColumnDef<OlxAccount, unknown>[] = [
    {
      id: "username",
      header: "Profil",
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <UserIcon className="size-4 text-muted-foreground" />
          <Link
            href={`/dashboard/accounts/${row.original.id}`}
            className="font-medium hover:underline"
          >
            {row.original.username}
          </Link>
          {active?.id === row.original.id ? (
            <span className="inline-flex items-center gap-1 rounded-sm bg-primary/10 px-1.5 py-0.5 text-xs font-medium text-primary">
              <StarIcon className="size-3" /> Aktivan
            </span>
          ) : null}
        </div>
      ),
    },
    {
      accessorKey: "olx_user_id",
      header: "OLX ID",
      size: 140,
      cell: ({ row }) => (
        <span className="text-muted-foreground tabular-nums">
          {row.original.olx_user_id ?? "—"}
        </span>
      ),
    },
    {
      id: "token",
      header: "Token",
      size: 160,
      cell: ({ row }) => (
        <TokenStatusBadge expiresAt={row.original.token_expires_at} />
      ),
    },
    {
      id: "listings",
      header: "Artikli",
      size: 100,
      cell: ({ row }) => (
        <span className="tabular-nums">{countFor(row.original.id)}</span>
      ),
    },
    {
      accessorKey: "created_at",
      header: "Kreirano",
      size: 160,
      cell: ({ row }) => (
        <span className="text-muted-foreground">
          {formatDate(row.original.created_at)}
        </span>
      ),
    },
    {
      id: "actions",
      header: "",
      size: 56,
      cell: ({ row }) => (
        <DropdownMenu>
          <DropdownMenuTrigger
            render={<Button variant="ghost" size="icon" className="size-8" />}
          >
            <MoreHorizontalIcon className="size-4" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => setAccount(row.original)}>
              <StarIcon />
              Postavi kao aktivan
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setEditTarget(row.original)}>
              <PencilIcon />
              Uredi
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              variant="destructive"
              onClick={() => setDeleteTarget(row.original)}
            >
              <Trash2Icon />
              Obriši
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteAccount.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success(toastMessages.deleted);
        if (active?.id === deleteTarget.id) setAccount(null);
        setDeleteTarget(null);
      },
      onError: (err) => {
        toast.error(err.message || toastMessages.errorDelete);
      },
    });
  };

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <PageHeader
        title="OLX Profili"
        description="Upravljanje povezanim OLX.ba računima."
      >
        <Button onClick={() => setAddOpen(true)}>
          <PlusIcon />
          Dodaj profil
        </Button>
      </PageHeader>

      {!isLoading && !isError && accounts.length === 0 ? (
        <EmptyState
          title="Nema povezanih profila"
          description="Dodajte svoj prvi OLX.ba račun da započnete."
        >
          <Button onClick={() => setAddOpen(true)}>
            <PlusIcon />
            Dodaj profil
          </Button>
        </EmptyState>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle>Povezani računi</CardTitle>
            <CardDescription>
              Pregled svih OLX.ba profila povezanih sa vašim nalogom.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <DataTable
              columns={columns}
              data={accounts}
              isLoading={isLoading}
              isError={isError}
            />
          </CardContent>
        </Card>
      )}

      <AddProfileDialog open={addOpen} onOpenChange={setAddOpen} />

      <EditProfileDialog
        open={!!editTarget}
        onOpenChange={(o) => !o && setEditTarget(null)}
        account={editTarget}
      />

      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Obriši profil"
        description={
          deleteTarget
            ? `Jeste li sigurni da želite obrisati profil "${deleteTarget.username}"? Ova akcija je nepovratna.`
            : ""
        }
        confirmLabel="Obriši"
        destructive
        onConfirm={handleDelete}
      />
    </div>
  );
}
