"use client"

import { useProductLinks, useCategoryMappings, useSyncHistory } from "@/hooks/use-sync"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import {
  RefreshCwIcon,
  ArrowRightLeftIcon,
  LayersIcon,
  HistoryIcon,
  CheckCircle2Icon,
  AlertCircleIcon,
  ClockIcon,
  ArrowUpRightIcon,
} from "lucide-react"
import Link from "next/link"
import { StatCard } from "@/components/shared/stat-card"
import { StatusBadge } from "@/components/shared/status-badge"
import { formatDate } from "@/lib/utils"
import { Loader2Icon } from "lucide-react"

export default function SyncPage() {
  const { data: links, isLoading: linksLoading } = useProductLinks()
  const { data: mappings, isLoading: mappingsLoading } = useCategoryMappings()
  const { data: history, isLoading: historyLoading } = useSyncHistory({ per_page: 5 })

  const totalLinks = links?.length || 0
  const totalMappings = mappings?.length || 0
  const successRate = history?.data.length
    ? Math.round((history.data.filter(log => log.status === "success").length / history.data.length) * 100)
    : 100

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Sinhronizacija</h1>
        <Button variant="outline" size="sm">
          <RefreshCwIcon className="mr-2 size-4" />
          Osvježi podatke
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Povezani artikli"
          value={totalLinks}
          hint="WooCommerce ↔ OLX"
          icon={<ArrowRightLeftIcon className="size-4 text-muted-foreground" />}
          isLoading={linksLoading}
        />
        <StatCard
          label="Mapirane kategorije"
          value={totalMappings}
          hint="Aktivna pravila"
          icon={<LayersIcon className="size-4 text-muted-foreground" />}
          isLoading={mappingsLoading}
        />
        <StatCard
          label="Uspješnost sync-a"
          value={`${successRate}%`}
          hint="Zadnjih 50 zapisa"
          icon={<CheckCircle2Icon className="size-4 text-green-500" />}
          isLoading={historyLoading}
        />
        <StatCard
          label="Aktivni poslovi"
          value="Automatski"
          hint="Svakih 30 minuta"
          icon={<ClockIcon className="size-4 text-blue-500" />}
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <div className="grid gap-1">
              <CardTitle>Zadnje aktivnosti</CardTitle>
              <CardDescription>
                Pregled zadnjih 5 pokušaja sinhronizacije.
              </CardDescription>
            </div>
            <Button variant="ghost" size="sm" render={<Link href="/sync/history" />}>
              Pogledaj sve
              <ArrowUpRightIcon className="ml-2 size-4" />
            </Button>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {historyLoading ? (
                <div className="flex h-32 items-center justify-center">
                  <Loader2Icon className="size-6 animate-spin text-muted-foreground" />
                </div>
              ) : !history?.data || history.data.length === 0 ? (
                <div className="flex h-32 items-center justify-center text-muted-foreground">
                  Nema zapisa o sinhronizaciji.
                </div>
              ) : (
                history.data.map((log) => (
                  <div key={log.id} className="flex items-center justify-between border-b pb-4 last:border-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      {log.status === "success" ? (
                        <div className="rounded-full bg-green-100 p-1.5 dark:bg-green-900/30">
                          <CheckCircle2Icon className="size-4 text-green-600 dark:text-green-400" />
                        </div>
                      ) : (
                        <div className="rounded-full bg-red-100 p-1.5 dark:bg-red-900/30">
                          <AlertCircleIcon className="size-4 text-red-600 dark:text-red-400" />
                        </div>
                      )}
                      <div className="flex flex-col">
                        <span className="text-sm font-medium leading-none">
                          {log.action === "create" ? "Kreiranje artikla" : 
                           log.action === "update" ? "Ažuriranje artikla" : 
                           log.action === "stock" ? "Sinhronizacija stanja" : "Ručna akcija"}
                        </span>
                        <span className="text-xs text-muted-foreground mt-1">
                          {log.message}
                        </span>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      <span className="text-xs text-muted-foreground">
                        {formatDate(log.created_at)}
                      </span>
                      <StatusBadge status={log.status === "success" ? "active" : "inactive"} />
                    </div>
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Brze Akcije</CardTitle>
            <CardDescription>Upravljajte postavkama sinhronizacije.</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-3">
            <Button className="w-full justify-start" variant="outline" render={<Link href="/sync/mappings" />}>
              <LayersIcon className="mr-2 size-4" />
              Mapiranje kategorija
            </Button>
            <Button className="w-full justify-start" variant="outline" render={<Link href="/woocommerce" />}>
              <RefreshCwIcon className="mr-2 size-4" />
              Poveži nove proizvode
            </Button>
            <Button className="w-full justify-start" variant="outline" render={<Link href="/sync/history" />}>
              <HistoryIcon className="mr-2 size-4" />
              Kompletna historija
            </Button>
            
            <div className="mt-4 rounded-lg bg-muted p-4">
              <h4 className="text-sm font-semibold mb-2">Pomoć pri sinhronizaciji</h4>
              <p className="text-xs text-muted-foreground leading-relaxed">
                Sinhronizacija se odvija automatski svakih 30 minuta. 
                Ako želite odmah osvježiti određeni artikal, koristite opciju 
                &quot;Sinhronizuj&quot; na stranici detalja artikla.
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
