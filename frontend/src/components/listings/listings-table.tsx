"use client"

import { useMemo } from "react"
import type { ColumnDef } from "@tanstack/react-table"
import { ImageIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { DataTable } from "@/components/shared/data-table"
import type { OlxListing } from "@/types/olx"

interface ListingsTableProps {
  listings: OlxListing[]
  isLoading: boolean
  isError: boolean
  hasAccount: boolean
  total: number
}

function statusVariant(status: string): "default" | "secondary" | "destructive" {
  switch (status) {
    case "active":
      return "default"
    case "expired":
    case "hidden":
      return "destructive"
    default:
      return "secondary"
  }
}

function formatPrice(price: number | null): string {
  if (price == null) return "—"
  return `${price.toLocaleString("bs-BA")} KM`
}

function formatDate(value: string): string {
  try {
    return new Date(value).toLocaleDateString("bs-BA")
  } catch {
    return value
  }
}

export function ListingsTable({
  listings,
  isLoading,
  isError,
  hasAccount,
  total,
}: ListingsTableProps) {
  const columns = useMemo<ColumnDef<OlxListing, unknown>[]>(
    () => [
      {
        id: "image",
        header: "Slika",
        size: 64,
        cell: ({ row }) => {
          const listing = row.original
          const mainImage =
            listing.images.find((img) => img.is_main) ?? listing.images[0]
          if (!mainImage) {
            return (
              <div className="flex size-12 items-center justify-center rounded-md bg-muted text-muted-foreground">
                <ImageIcon className="size-5" />
              </div>
            )
          }
          return (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={mainImage.url}
              alt={listing.title}
              className="size-12 rounded-md object-cover"
            />
          )
        },
      },
      {
        accessorKey: "title",
        header: "Naslov",
        cell: ({ row }) => (
          <span className="font-medium">{row.original.title}</span>
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
        cell: ({ row }) => (
          <Badge variant={statusVariant(row.original.status)}>
            {row.original.status}
          </Badge>
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
    ],
    []
  )

  return (
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
  )
}
