"use client"

import * as React from "react"
import { Area, AreaChart, CartesianGrid, XAxis } from "recharts"
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
  active: {
    label: "Aktivni",
    color: "#10b981",
  },
  drafts: {
    label: "Draftovi",
    color: "#6b7280",
  },
  finished: {
    label: "Završeni",
    color: "#f59e0b",
  },
} satisfies ChartConfig

interface ListingsOverTimeProps {
  data: {
    date: string;
    active: number;
    drafts: number;
    finished: number;
  }[];
  loading?: boolean;
}

export function ListingsOverTime({ data, loading }: ListingsOverTimeProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Broj artikala kroz vrijeme</CardTitle>
          <CardDescription>Zadnjih 30 dana</CardDescription>
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
        <CardTitle>Broj artikala kroz vrijeme</CardTitle>
        <CardDescription>Trend rasta i statusa artikala u zadnjih 30 dana</CardDescription>
      </CardHeader>
      <CardContent className="px-2 pt-4 sm:px-6 sm:pt-6">
        <ChartContainer
          config={chartConfig}
          className="aspect-auto h-[250px] w-full"
        >
          <AreaChart data={data}>
            <defs>
              <linearGradient id="fillActive" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="var(--color-active)" stopOpacity={0.1} />
                <stop offset="95%" stopColor="var(--color-active)" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="fillDrafts" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="var(--color-drafts)" stopOpacity={0.1} />
                <stop offset="95%" stopColor="var(--color-drafts)" stopOpacity={0} />
              </linearGradient>
            </defs>
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
                  month: "short",
                  day: "numeric",
                })
              }}
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
                  indicator="dot"
                />
              }
            />
            <Area
              dataKey="active"
              type="monotone"
              fill="url(#fillActive)"
              stroke="var(--color-active)"
              stackId="a"
            />
            <Area
              dataKey="drafts"
              type="monotone"
              fill="url(#fillDrafts)"
              stroke="var(--color-drafts)"
              stackId="a"
            />
          </AreaChart>
        </ChartContainer>
      </CardContent>
    </Card>
  )
}
