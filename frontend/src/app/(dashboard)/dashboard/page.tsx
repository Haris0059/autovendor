"use client";

import { useMemo } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  XAxis,
  YAxis,
} from "recharts";
import {
  CheckCircle2Icon,
  ClockIcon,
  EyeOffIcon,
  FileTextIcon,
  FlameIcon,
  RefreshCwIcon,
} from "lucide-react";

import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import {
  Progress,
  ProgressIndicator,
  ProgressLabel,
  ProgressTrack,
  ProgressValue,
} from "@/components/ui/progress";
import { useAllListings } from "@/hooks/use-listings";
import { useOlxAccounts } from "@/hooks/use-olx-accounts";
import { useListingLimits, useRefreshLimits } from "@/hooks/use-listing-limits";

const LIMIT_LABELS: Record<string, string> = {
  cars: "Automobili",
  real_estate: "Nekretnine",
  other: "Ostalo",
};

const STATUS_LABEL: Record<string, string> = {
  active: "Aktivni",
  draft: "Draft",
  expired: "Istekli",
  hidden: "Sakriveni",
  finished: "Završeni",
};

const STATUS_COLORS: Record<string, string> = {
  active: "var(--chart-1)",
  draft: "var(--chart-2)",
  expired: "var(--chart-3)",
  hidden: "var(--chart-4)",
  finished: "var(--chart-5)",
};

function computeListingsOverTime(
  data: Array<{ created_at: string }>
): Array<{ date: string; count: number }> {
  const buckets = new Map<string, number>();
  const now = Date.now();
  for (let i = 89; i >= 0; i--) {
    const day = new Date(now - i * 24 * 3600 * 1000)
      .toISOString()
      .slice(0, 10);
    buckets.set(day, 0);
  }
  data.forEach((l) => {
    const day = l.created_at.slice(0, 10);
    if (buckets.has(day)) buckets.set(day, (buckets.get(day) ?? 0) + 1);
  });
  return Array.from(buckets, ([date, count]) => ({ date, count }));
}

export default function DashboardPage() {
  const listings = useAllListings();
  const accounts = useOlxAccounts();
  const primaryAccountId = accounts.data?.[0]?.id ?? 0;
  const limits = useListingLimits(primaryAccountId);
  const refresh = useRefreshLimits(primaryAccountId);

  const byStatus = useMemo(() => {
    const counts: Record<string, number> = {
      active: 0,
      draft: 0,
      expired: 0,
      hidden: 0,
      finished: 0,
    };
    (listings.data ?? []).forEach((l) => {
      counts[l.status] = (counts[l.status] ?? 0) + 1;
    });
    return counts;
  }, [listings.data]);

  const listingsOverTime = useMemo(
    () => computeListingsOverTime(listings.data ?? []),
    [listings.data]
  );

  const statusData = Object.entries(byStatus).map(([status, count]) => ({
    status,
    count,
    label: STATUS_LABEL[status] ?? status,
    fill: STATUS_COLORS[status],
  }));

  const chartConfig: ChartConfig = {
    count: { label: "Artikli", color: "var(--chart-1)" },
  };

  const isLoading = listings.isLoading || accounts.isLoading;

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <PageHeader
        title="Dashboard"
        description="Pregled svih OLX profila i WooCommerce prodavnica."
      />

      <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-2 @5xl/main:grid-cols-4">
        <StatCard
          label="Aktivni artikli"
          value={byStatus.active}
          isLoading={isLoading}
          icon={<CheckCircle2Icon />}
        />
        <StatCard
          label="Drafts"
          value={byStatus.draft}
          isLoading={isLoading}
          icon={<FileTextIcon />}
        />
        <StatCard
          label="Istekli"
          value={byStatus.expired}
          isLoading={isLoading}
          icon={<ClockIcon />}
        />
        <StatCard
          label="Sakriveni"
          value={byStatus.hidden}
          isLoading={isLoading}
          icon={<EyeOffIcon />}
        />
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
                  <ProgressValue>{pct}%</ProgressValue>
                  <ProgressTrack>
                    <ProgressIndicator />
                  </ProgressTrack>
                </Progress>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-3">
        <Card className="@xl/main:col-span-1">
          <CardHeader>
            <CardDescription>Osvježavanja</CardDescription>
            <CardTitle className="flex items-center gap-2 text-2xl tabular-nums">
              <RefreshCwIcon className="size-5 text-primary" />
              {refresh.data?.free_count ?? 0} / {refresh.data?.free_limit ?? 0}
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3 text-sm">
            <Progress
              value={
                refresh.data && refresh.data.free_limit > 0
                  ? Math.round(
                      (refresh.data.free_count / refresh.data.free_limit) * 100
                    )
                  : 0
              }
            >
              <ProgressLabel>Besplatna</ProgressLabel>
              <ProgressTrack>
                <ProgressIndicator />
              </ProgressTrack>
            </Progress>
            <div className="flex justify-between text-muted-foreground">
              <span>Plaćena osvježavanja</span>
              <span className="tabular-nums text-foreground">
                {refresh.data?.paid_count ?? 0}
              </span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Ukupno pod artikala</span>
              <span className="tabular-nums text-foreground">
                {refresh.data?.listing_count ?? 0}
              </span>
            </div>
          </CardContent>
        </Card>

        <Card className="@xl/main:col-span-2">
          <CardHeader>
            <CardDescription>Artikli kreirani (90 dana)</CardDescription>
            <CardTitle className="flex items-center gap-2">
              <FlameIcon className="size-5 text-primary" />
              Kreiranja kroz vrijeme
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer config={chartConfig} className="h-[220px] w-full">
              <AreaChart data={listingsOverTime}>
                <defs>
                  <linearGradient id="fillCount" x1="0" y1="0" x2="0" y2="1">
                    <stop
                      offset="5%"
                      stopColor="var(--chart-1)"
                      stopOpacity={0.8}
                    />
                    <stop
                      offset="95%"
                      stopColor="var(--chart-1)"
                      stopOpacity={0.1}
                    />
                  </linearGradient>
                </defs>
                <CartesianGrid vertical={false} strokeDasharray="3 3" />
                <XAxis
                  dataKey="date"
                  tickLine={false}
                  axisLine={false}
                  tickMargin={8}
                  tickFormatter={(v: string) =>
                    new Date(v).toLocaleDateString("bs-BA", {
                      day: "2-digit",
                      month: "2-digit",
                    })
                  }
                  minTickGap={32}
                />
                <YAxis hide />
                <ChartTooltip
                  content={<ChartTooltipContent indicator="dot" />}
                />
                <Area
                  dataKey="count"
                  type="natural"
                  fill="url(#fillCount)"
                  stroke="var(--chart-1)"
                />
              </AreaChart>
            </ChartContainer>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 @xl/main:grid-cols-2">
        <Card>
          <CardHeader>
            <CardDescription>Status distribucija</CardDescription>
            <CardTitle>Artikli po statusu</CardTitle>
          </CardHeader>
          <CardContent>
            <ChartContainer
              config={chartConfig}
              className="mx-auto aspect-square max-h-[260px]"
            >
              <PieChart>
                <ChartTooltip content={<ChartTooltipContent />} />
                <Pie
                  data={statusData}
                  dataKey="count"
                  nameKey="label"
                  innerRadius={50}
                  strokeWidth={2}
                >
                  {statusData.map((entry) => (
                    <Cell key={entry.status} fill={entry.fill} />
                  ))}
                </Pie>
              </PieChart>
            </ChartContainer>
            <div className="mt-4 grid grid-cols-2 gap-2 text-sm">
              {statusData.map((s) => (
                <div key={s.status} className="flex items-center gap-2">
                  <span
                    className="size-3 rounded-sm"
                    style={{ backgroundColor: s.fill }}
                  />
                  <span className="flex-1 text-muted-foreground">
                    {s.label}
                  </span>
                  <span className="tabular-nums">{s.count}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Profili</CardDescription>
            <CardTitle>OLX profili pod upravom</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {(accounts.data ?? []).map((acc) => {
              const count = (listings.data ?? []).filter(
                (l) => (l as { account_id?: number }).account_id === acc.id
              ).length;
              return (
                <div
                  key={acc.id}
                  className="flex items-center justify-between rounded-md border px-3 py-2"
                >
                  <div>
                    <div className="font-medium">{acc.username}</div>
                    <div className="text-xs text-muted-foreground">
                      OLX ID: {acc.olx_user_id ?? "—"}
                    </div>
                  </div>
                  <span className="tabular-nums text-muted-foreground">
                    {count} artikala
                  </span>
                </div>
              );
            })}
            {!accounts.isLoading && (accounts.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Nema dodanih profila.
              </p>
            ) : null}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
