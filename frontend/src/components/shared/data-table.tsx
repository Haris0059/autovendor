"use client"

import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
} from "@tanstack/react-table"
import { Loader2Icon } from "lucide-react"

import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

const SKELETON_WIDTHS = ["max-w-32", "max-w-20", "max-w-40", "max-w-16"]

interface DataTableProps<TData> {
  columns: ColumnDef<TData, unknown>[]
  data: TData[]
  isLoading?: boolean
  /** Background refetch in progress — existing rows stay visible, dimmed under a spinner. */
  isFetching?: boolean
  isError?: boolean
  emptyMessage?: string
  errorMessage?: string
  loadingRowCount?: number
}

export function DataTable<TData>({
  columns,
  data,
  isLoading = false,
  isFetching = false,
  isError = false,
  emptyMessage = "Nema podataka.",
  errorMessage = "Greška pri učitavanju podataka.",
  loadingRowCount = 5,
}: DataTableProps<TData>) {
  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
  })

  const columnCount = columns.length
  // No rows on screen while fetching → skeletons (an overlay would have nothing
  // to dim); rows present → dim them under the spinner instead.
  const showSkeletons = isLoading || (isFetching && data.length === 0)
  const showFetchingOverlay = isFetching && !showSkeletons

  return (
    <div className="relative">
      {showFetchingOverlay ? (
        <div className="absolute inset-0 z-10 flex items-center justify-center rounded-md bg-background/60">
          <div className="flex items-center gap-2 rounded-md border bg-background px-3 py-1.5 text-sm text-muted-foreground shadow-sm">
            <Loader2Icon className="size-4 animate-spin" />
            Učitavanje…
          </div>
        </div>
      ) : null}
      <Table>
      <TableHeader>
        {table.getHeaderGroups().map((headerGroup) => (
          <TableRow key={headerGroup.id}>
            {headerGroup.headers.map((header) => (
              <TableHead key={header.id} style={{ width: header.getSize() !== 150 ? header.getSize() : undefined }}>
                {header.isPlaceholder
                  ? null
                  : flexRender(
                      header.column.columnDef.header,
                      header.getContext()
                    )}
              </TableHead>
            ))}
          </TableRow>
        ))}
      </TableHeader>
      <TableBody>
        {showSkeletons ? (
          Array.from({ length: loadingRowCount }).map((_, rowIdx) => (
            <TableRow key={`skeleton-${rowIdx}`}>
              {columns.map((_col, colIdx) => (
                <TableCell key={`skeleton-${rowIdx}-${colIdx}`}>
                  <Skeleton
                    className={`h-4 w-full ${
                      SKELETON_WIDTHS[(rowIdx + colIdx) % SKELETON_WIDTHS.length]
                    }`}
                  />
                </TableCell>
              ))}
            </TableRow>
          ))
        ) : isError ? (
          <TableRow>
            <TableCell
              colSpan={columnCount}
              className="h-32 text-center text-muted-foreground"
            >
              {errorMessage}
            </TableCell>
          </TableRow>
        ) : table.getRowModel().rows.length === 0 ? (
          <TableRow>
            <TableCell
              colSpan={columnCount}
              className="h-32 text-center text-muted-foreground"
            >
              {emptyMessage}
            </TableCell>
          </TableRow>
        ) : (
          table.getRowModel().rows.map((row) => (
            <TableRow key={row.id}>
              {row.getVisibleCells().map((cell) => (
                <TableCell key={cell.id}>
                  {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </TableCell>
              ))}
            </TableRow>
          ))
        )}
      </TableBody>
      </Table>
    </div>
  )
}
