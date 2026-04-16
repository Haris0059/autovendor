"use client"

import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart"

const chartConfig = {
  count: {
    label: "Broj obnova",
    color: "var(--primary)",
  },
  budget_used: {
    label: "Potrošeni budžet (KM)",
    color: "#f59e0b",
  },
} satisfies ChartConfig

interface RefreshBurnProps {
  data: {
    date: string;
    count: number;
    budget_used: number;
  }[];
  loading?: boolean;
}

export function RefreshBurn({ data, loading }: RefreshBurnProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Potrošnja budžeta za obnavljanje</CardTitle>
          <CardDescription>Zadnjih 14 dana</CardDescription>
        </CardHeader>
        <CardContent className="h-[250px] flex items-center justify-center">
          <div className="size-6 border-2 border-primary border-t-transparent animate-spin rounded-full" />
        </CardContent>
      </Card>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Potrošnja budžeta za obnavljanje</CardTitle>
        <CardDescription>Dnevni pregled iskorištenog budžeta i broja obnova</CardDescription>
      </CardHeader>
      <CardContent className="px-2 pt-4 sm:px-6 sm:pt-6">
        <ChartContainer
          config={chartConfig}
          className="aspect-auto h-[250px] w-full"
        >
          <BarChart data={data}>
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="date"
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              minTickGap={32}
              tickFormatter={(value) => {
                const date = new Date(value)
                return date.toLocaleDateString("bs-BA", {
                  day: "numeric",
                  month: "short",
                })
              }}
            />
            <YAxis
              tickLine={false}
              axisLine={false}
              tickMargin={8}
              tickFormatter={(value) => `${value}`}
            />
            <ChartTooltip
              cursor={false}
              content={
                <ChartTooltipContent
                  labelFormatter={(value) => {
                    return new Date(value).toLocaleDateString("bs-BA", {
                      month: "long",
                      day: "numeric",
                      year: "numeric",
                    })
                  }}
                />
              }
            />
            <Bar
              dataKey="count"
              fill="var(--color-count)"
              radius={[4, 4, 0, 0]}
              barSize={20}
            />
            <Bar
              dataKey="budget_used"
              fill="var(--color-budget_used)"
              radius={[4, 4, 0, 0]}
              barSize={20}
            />
          </BarChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
