"use client"

import { useState } from "react"
import { useOlxAccounts } from "@/hooks/use-olx-accounts"
import { useListingStats } from "@/hooks/use-listing-stats"
import { useRefreshHistory } from "@/hooks/use-refresh-history"
import { useSponsorHistory } from "@/hooks/use-sponsor-history"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Button } from "@/components/ui/button"
import { ListingsOverTime } from "@/components/analytics/listings-over-time"
import { RefreshBurn } from "@/components/analytics/refresh-burn"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { 
  DownloadIcon, 
  CalendarIcon, 
  UsersIcon,
  BarChart3Icon,
  TrendingUpIcon,
  ZapIcon,
  Loader2Icon
} from "lucide-react"
import { StatCard } from "@/components/shared/stat-card"

export default function AnalyticsPage() {
  const [selectedAccountId, setSelectedAccountId] = useState<string>("all")
  const { data: accounts } = useOlxAccounts()
  
  const accountId = selectedAccountId === "all" ? undefined : parseInt(selectedAccountId)
  
  const { data: stats, isLoading: statsLoading } = useListingStats(accountId)
  const { data: refresh, isLoading: refreshLoading } = useRefreshHistory(accountId)
  const { data: sponsors, isLoading: sponsorsLoading } = useSponsorHistory(accountId)

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Analitika</h1>
          <p className="text-sm text-muted-foreground">
            Pratite performanse vaših OLX profila i sinhronizacije.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Select value={selectedAccountId} onValueChange={(v) => setSelectedAccountId(v ?? "all")}>
            <SelectTrigger className="w-[180px] h-9">
              <UsersIcon className="mr-2 size-3 text-muted-foreground" />
              <SelectValue placeholder="Svi profili" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Svi profili</SelectItem>
              {accounts?.map(a => (
                <SelectItem key={a.id} value={a.id.toString()}>{a.username}</SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Button variant="outline" size="sm">
            <CalendarIcon className="mr-2 size-4" />
            Zadnjih 30 dana
          </Button>

          <Button variant="outline" size="sm">
            <DownloadIcon className="mr-2 size-4" />
            Izvještaj
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Ukupno obnova"
          value={refresh?.reduce((acc: number, curr: { count: number }) => acc + curr.count, 0) || 0}
          hint="Zadnjih 14 dana"
          icon={<BarChart3Icon className="size-4 text-muted-foreground" />}
          isLoading={refreshLoading}
        />
        <StatCard
          label="Potrošeno kredita"
          value={sponsors?.reduce((acc: number, curr: { credits: number }) => acc + curr.credits, 0) || 0}
          hint="Zadnjih 6 mjeseci"
          icon={<ZapIcon className="size-4 text-yellow-500" />}
          isLoading={sponsorsLoading}
        />
        <StatCard
          label="Aktivnih oglasa"
          value={stats?.history[stats.history.length - 1]?.active || 0}
          hint="Trenutni status"
          icon={<TrendingUpIcon className="size-4 text-green-500" />}
          isLoading={statsLoading}
        />
        <StatCard
          label="Novi oglasi"
          value={12}
          hint="Ovaj mjesec"
          icon={<BarChart3Icon className="size-4 text-blue-500" />}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <ListingsOverTime 
          data={stats?.history || []} 
          loading={statsLoading} 
        />
        <RefreshBurn 
          data={refresh || []} 
          loading={refreshLoading} 
        />
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Top Kategorije</CardTitle>
            <CardDescription>Raspodjela oglasa i vrijednosti po kategorijama</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {statsLoading ? (
                <div className="flex h-32 items-center justify-center">
                  <Loader2Icon className="size-6 animate-spin text-muted-foreground" />
                </div>
              ) : (
                stats?.categories.map((cat: { name: string; count: number }) => (
                  <div key={cat.name} className="flex items-center gap-4">
                    <div className="w-24 text-sm font-medium">{cat.name}</div>
                    <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                      <div 
                        className="h-full bg-primary" 
                        style={{ width: `${(cat.count / 100) * 100}%` }}
                      />
                    </div>
                    <div className="w-12 text-right text-sm text-muted-foreground">{cat.count}</div>
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Troškovi sponzorstva</CardTitle>
            <CardDescription>Mjesečni pregled (OLX Krediti)</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {sponsorsLoading ? (
                <div className="flex h-32 items-center justify-center">
                  <Loader2Icon className="size-6 animate-spin text-muted-foreground" />
                </div>
              ) : (
                sponsors?.map((s: { month: string; credits: number }) => (
                  <div key={s.month} className="flex items-center justify-between">
                    <span className="text-sm font-medium">{s.month}</span>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold">{s.credits}</span>
                      <span className="text-[10px] text-muted-foreground uppercase">kredita</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
