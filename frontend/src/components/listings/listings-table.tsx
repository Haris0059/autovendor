"use client";

import { useMemo } from "react";
import Link from "next/link";
import { toast } from "sonner";
import type { ColumnDef } from "@tanstack/react-table";
import {
  CheckCircle2Icon,
  EyeIcon,
  EyeOffIcon,
  FlagIcon,
  ImageIcon,
  ImagesIcon,
  MegaphoneIcon,
  MoreHorizontalIcon,
  PencilIcon,
  PercentIcon,
  RefreshCwIcon,
  Trash2Icon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DataTable } from "@/components/shared/data-table";
import { OlxStatusBadge } from "@/components/shared/status-badge";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  useDeleteListing,
  useListingAction,
  type ListingAction,
} from "@/hooks/use-listings";
import { toastMessages } from "@/lib/toast-messages";
import type { OlxListing } from "@/types/olx";
import { useState } from "react";

interface ListingsTableProps {
  listings: OlxListing[];
  isLoading: boolean;
  isError: boolean;
  hasAccount: boolean;
  total: number;
  selectedIds?: number[];
  onSelectedChange?: (ids: number[]) => void;
  enableSelection?: boolean;
  enableRowActions?: boolean;
}

function formatPrice(price: number | null): string {
  if (price == null) return "—";
  return `${price.toLocaleString("bs-BA")} KM`;
}

function formatDate(value: string): string {
  try {
    return new Date(value).toLocaleDateString("bs-BA");
  } catch {
    return value;
  }
}

type ToastKey = Extract<keyof typeof toastMessages, string>

const ROW_ACTIONS: {
  key: ListingAction;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  toastKey: ToastKey;
}[] = [
  {
    key: "publish",
    label: "Objavi",
    icon: CheckCircle2Icon,
    toastKey: "published",
  },
  {
    key: "refresh",
    label: "Osvježi",
    icon: RefreshCwIcon,
    toastKey: "refreshed",
  },
  { key: "hide", label: "Sakrij", icon: EyeOffIcon, toastKey: "hidden" },
  { key: "unhide", label: "Otkrij", icon: EyeIcon, toastKey: "unhidden" },
  { key: "finish", label: "Završi", icon: FlagIcon, toastKey: "finished" },
];

export function ListingsTable({
  listings,
  isLoading,
  isError,
  hasAccount,
  total,
  selectedIds,
  onSelectedChange,
  enableSelection = false,
  enableRowActions = false,
}: ListingsTableProps) {
  const action = useListingAction();
  const remove = useDeleteListing();
  const [deleteTarget, setDeleteTarget] = useState<OlxListing | null>(null);

  const allSelected =
    enableSelection &&
    listings.length > 0 &&
    (selectedIds?.length ?? 0) === listings.length;

  const toggleAll = () => {
    if (!onSelectedChange) return;
    if (allSelected) onSelectedChange([]);
    else onSelectedChange(listings.map((l) => l.id));
  };

  const toggleOne = (id: number) => {
    if (!onSelectedChange) return;
    const current = selectedIds ?? [];
    onSelectedChange(
      current.includes(id) ? current.filter((i) => i !== id) : [...current, id]
    );
  };

  const runAction = (
    listing: OlxListing,
    key: ListingAction,
    toastKey: ToastKey
  ) => {
    action.mutate(
      { id: listing.id, action: key },
      {
        onSuccess: () => toast.success(toastMessages[toastKey] as string),
        onError: (err) =>
          toast.error(err.message || toastMessages.error),
      }
    );
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    remove.mutate(deleteTarget.id, {
      onSuccess: () => {
        toast.success(toastMessages.deleted);
        setDeleteTarget(null);
      },
      onError: (err) =>
        toast.error(err.message || toastMessages.errorDelete),
    });
  };

  const columns = useMemo<ColumnDef<OlxListing, unknown>[]>(() => {
    const cols: ColumnDef<OlxListing, unknown>[] = [];

    if (enableSelection) {
      cols.push({
        id: "select",
        size: 40,
        header: () => (
          <Checkbox
            checked={allSelected}
            onCheckedChange={() => toggleAll()}
            aria-label="Odaberi sve"
          />
        ),
        cell: ({ row }) => (
          <Checkbox
            checked={(selectedIds ?? []).includes(row.original.id)}
            onCheckedChange={() => toggleOne(row.original.id)}
            aria-label={`Odaberi ${row.original.title}`}
          />
        ),
      });
    }

    cols.push(
      {
        id: "image",
        header: "Slika",
        size: 64,
        cell: ({ row }) => {
          const listing = row.original;
          const mainImage =
            listing.images.find((img) => img.is_main) ?? listing.images[0];
          if (!mainImage) {
            return (
              <div className="flex size-12 items-center justify-center rounded-md bg-muted text-muted-foreground">
                <ImageIcon className="size-5" />
              </div>
            );
          }
          return (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={mainImage.url}
              alt={listing.title}
              className="size-12 rounded-md object-cover"
            />
          );
        },
      },
      {
        accessorKey: "title",
        header: "Naslov",
        cell: ({ row }) => (
          <Link
            href={`/listings/${row.original.id}`}
            className="font-medium hover:underline"
          >
            {row.original.title}
          </Link>
        ),
      },
      {
        accessorKey: "price",
        header: "Cijena",
        size: 128,
        cell: ({ row }) => formatPrice(row.original.price),
      },
      {
        accessorKey: "status",
        header: "Status",
        size: 128,
        cell: ({ row }) => <OlxStatusBadge status={row.original.status} />,
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
      }
    );

    if (enableRowActions) {
      cols.push({
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
              <DropdownMenuItem
                render={<Link href={`/listings/${row.original.id}`} />}
              >
                <PencilIcon />
                Uredi
              </DropdownMenuItem>
              <DropdownMenuItem
                render={
                  <Link href={`/listings/${row.original.id}/images`} />
                }
              >
                <ImagesIcon />
                Slike
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              {ROW_ACTIONS.map(({ key, label, icon: Icon, toastKey }) => (
                <DropdownMenuItem
                  key={key}
                  onClick={() => runAction(row.original, key, toastKey)}
                >
                  <Icon />
                  {label}
                </DropdownMenuItem>
              ))}
              <DropdownMenuSeparator />
              <DropdownMenuItem disabled>
                <MegaphoneIcon />
                Sponzoriši
              </DropdownMenuItem>
              <DropdownMenuItem disabled>
                <PercentIcon />
                Sniženje
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
      });
    }

    return cols;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enableSelection, enableRowActions, selectedIds, listings, allSelected]);

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Artikli profila</CardTitle>
          <CardDescription>
            {hasAccount
              ? `${total} ${total === 1 ? "artikal" : "artikala"}`
              : "Nije odabran profil"}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={hasAccount ? listings : []}
            isLoading={hasAccount && isLoading}
            isError={hasAccount && isError}
            emptyMessage={
              hasAccount
                ? "Nema artikala za prikaz."
                : "Odaberite OLX profil za prikaz artikala."
            }
            errorMessage="Greška pri učitavanju artikala."
          />
        </CardContent>
      </Card>
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Obriši artikal"
        description={
          deleteTarget ? `"${deleteTarget.title}" će biti trajno obrisan.` : undefined
        }
        confirmLabel="Obriši"
        destructive
        loading={remove.isPending}
        onConfirm={handleDelete}
      />
    </>
  );
}

