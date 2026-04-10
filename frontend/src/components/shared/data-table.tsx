"use client"

import {
  flexRender,
  getCoreRowModel,
  useReactTable,
  type ColumnDef,
} from "@tanstack/react-table"

import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

interface DataTableProps<TData> {
  columns: ColumnDef<TData, unknown>[]
  data: TData[]
  isLoading?: boolean
  isError?: boolean
  emptyMessage?: string
  errorMessage?: string
  loadingRowCount?: number
}

export function DataTable<TData>({
  columns,
  data,
  isLoading = false,
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

  return (
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
        {isLoading ? (
          Array.from({ length: loadingRowCount }).map((_, rowIdx) => (
            <TableRow key={`skeleton-${rowIdx}`}>
              {columns.map((_col, colIdx) => (
                <TableCell key={`skeleton-${rowIdx}-${colIdx}`}>
                  <Skeleton className="h-4 w-full max-w-32" />
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
  )
}
