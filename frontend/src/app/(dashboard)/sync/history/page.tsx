"use client"

import { useState } from "react"
import { useSyncHistory, useTriggerSync } from "@/hooks/use-sync"
import { useWooStores } from "@/hooks/use-woo-stores"
import { useOlxAccounts } from "@/hooks/use-olx-accounts"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  RefreshCwIcon,
  Loader2Icon,
  CheckCircle2Icon,
  AlertCircleIcon,
  FilterIcon,
  ArrowLeftIcon,
} from "lucide-react"
import Link from "next/link"
import { formatDate } from "@/lib/utils"
import { toast } from "sonner"
import { PageHeader } from "@/components/shared/page-header"

export default function SyncHistoryPage() {
  const [page, setPage] = useState(1)
  const [statusFilter, setStatusFilter] = useState<string>("Svi statusi")
  const [storeFilter, setStoreFilter] = useState<string>("Svi shopovi")
  const [accountFilter, setAccountFilter] = useState<string>("Svi profili")

  const { data: stores } = useWooStores()
  const { data: accounts } = useOlxAccounts()
  const triggerSync = useTriggerSync()

  const { data: history, isLoading } = useSyncHistory({
    page,
    per_page: 20,
    status: statusFilter === "Svi statusi" ? undefined : statusFilter,
    store_id: storeFilter === "Svi shopovi" ? undefined : parseInt(storeFilter),
    account_id: accountFilter === "Svi profili" ? undefined : parseInt(accountFilter),
  })

  const handleRetry = (linkId: number) => {
    triggerSync.mutate(linkId, {
      onSuccess: () => {
        toast.success("Sinhronizacija ponovo pokrenuta.")
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : "Greška pri pokretanju sinhronizacije."
        toast.error(message)
      }
    })
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <PageHeader title="Historija sinhronizacije">
        <Button 
          variant="outline" 
          size="icon" 
          className="size-8"
          render={<Link href="/sync" />}
        >
          <ArrowLeftIcon className="size-4" />
        </Button>
      </PageHeader>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="grid gap-1">
              <CardTitle>Logovi aktivnosti</CardTitle>
              <CardDescription>
                Detaljan pregled svih procesa sinhronizacije.
              </CardDescription>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Select
                items={[
                  { value: "Svi statusi", label: "Svi statusi" },
                  { value: "success", label: "Uspješno" },
                  { value: "failed", label: "Greška" },
                  { value: "pending", label: "Na čekanju" },
                ]}
                value={statusFilter}
                onValueChange={(v) => setStatusFilter(v ?? "Svi statusi")}
              >
                <SelectTrigger className="w-[140px] h-9">
                  <FilterIcon className="mr-2 size-3 text-muted-foreground" />
                  <SelectValue placeholder="Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Svi statusi">Svi statusi</SelectItem>
                  <SelectItem value="success">Uspješno</SelectItem>
                  <SelectItem value="failed">Greška</SelectItem>
                  <SelectItem value="pending">Na čekanju</SelectItem>
                </SelectContent>
              </Select>

              <Select
                items={[
                  { value: "Svi shopovi", label: "Svi shopovi" },
                  ...(stores ?? []).map((s) => ({ value: s.id.toString(), label: s.name })),
                ]}
                value={storeFilter}
                onValueChange={(v) => setStoreFilter(v ?? "Svi shopovi")}
              >
                <SelectTrigger className="w-[160px] h-9">
                  <SelectValue placeholder="Shop" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Svi shopovi">Svi shopovi</SelectItem>
                  {stores?.map(s => (
                    <SelectItem key={s.id} value={s.id.toString()}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Select
                items={[
                  { value: "Svi profili", label: "Svi profili" },
                  ...(accounts ?? []).map((a) => ({ value: a.id.toString(), label: a.username })),
                ]}
                value={accountFilter}
                onValueChange={(v) => setAccountFilter(v ?? "Svi profili")}
              >
                <SelectTrigger className="w-[160px] h-9">
                  <SelectValue placeholder="Profil" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Svi profili">Svi profili</SelectItem>
                  {accounts?.map(a => (
                    <SelectItem key={a.id} value={a.id.toString()}>{a.username}</SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <Button variant="outline" size="sm" onClick={() => {
                setStatusFilter("Svi statusi")
                setStoreFilter("Svi shopovi")
                setAccountFilter("Svi profili")
                setPage(1)
              }}>
                Poništi
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Vrijeme</TableHead>
                <TableHead>Akcija</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Poruka</TableHead>
                <TableHead className="w-20 text-right">Akcije</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-32 text-center">
                    <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : !history?.data || history.data.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-32 text-center text-muted-foreground">
                    Nema zapisa koji odgovaraju filterima.
                  </TableCell>
                </TableRow>
              ) : (
                history.data.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {formatDate(log.created_at)}
                    </TableCell>
                    <TableCell>
                      <span className="font-medium capitalize">
                        {log.action === "create" ? "Kreiranje" : 
                         log.action === "update" ? "Ažuriranje" : 
                         log.action === "stock" ? "Zalihe" : log.action}
                      </span>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        {log.status === "success" ? (
                          <CheckCircle2Icon className="size-4 text-green-500" />
                        ) : log.status === "failed" ? (
                          <AlertCircleIcon className="size-4 text-destructive" />
                        ) : (
                          <Loader2Icon className="size-4 animate-spin text-blue-500" />
                        )}
                        <span className={log.status === "failed" ? "text-destructive font-medium" : ""}>
                          {log.status === "success" ? "Uspješno" : 
                           log.status === "failed" ? "Greška" : "U toku"}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs truncate text-muted-foreground">
                      {log.message}
                    </TableCell>
                    <TableCell className="text-right">
                      {log.status === "failed" && log.product_link_id != null && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleRetry(log.product_link_id!)}
                          disabled={triggerSync.isPending}
                        >
                          <RefreshCwIcon className={`size-4 ${triggerSync.isPending ? "animate-spin" : ""}`} />
                          <span className="sr-only">Ponovi</span>
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {history && history.last_page > 1 && (
            <div className="mt-4 flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.max(1, p - 1))}
                disabled={page === 1}
              >
                Prethodna
              </Button>
              <span className="text-sm text-muted-foreground">
                Stranica {page} od {history.last_page}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.min(history.last_page, p + 1))}
                disabled={page === history.last_page}
              >
                Sljedeća
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
