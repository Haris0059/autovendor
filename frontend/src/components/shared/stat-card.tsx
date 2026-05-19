"use client";

import { TrendingDownIcon, TrendingUpIcon } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string | number;
  hint?: string;
  delta?: number;
  icon?: React.ReactNode;
  isLoading?: boolean;
  className?: string;
}

export function StatCard({
  label,
  value,
  hint,
  delta,
  icon,
  isLoading,
  className,
}: StatCardProps) {
  const formattedValue =
    typeof value === "number" ? value.toLocaleString("bs-BA") : value;

  return (
    <Card className={cn("gap-2", className)}>
      <CardHeader className="pb-0">
        <div className="flex items-start justify-between gap-2">
          <CardDescription>{label}</CardDescription>
          {icon ? (
            <div className="text-muted-foreground [&>svg]:size-4">{icon}</div>
          ) : null}
        </div>
        <CardTitle className="mt-1 text-2xl font-semibold tabular-nums">
          {isLoading ? (
            <Skeleton className="h-7 w-24" />
          ) : (
            formattedValue
          )}
        </CardTitle>
      </CardHeader>
      {(hint || delta !== undefined) && !isLoading ? (
        <CardContent className="flex items-center gap-2 pt-0 text-sm text-muted-foreground">
          {delta !== undefined ? (
            <Badge variant={delta >= 0 ? "default" : "destructive"}>
              {delta >= 0 ? (
                <TrendingUpIcon />
              ) : (
                <TrendingDownIcon />
              )}
              {delta >= 0 ? "+" : ""}
              {delta}%
            </Badge>
          ) : null}
          {hint ? <span>{hint}</span> : null}
        </CardContent>
      ) : null}
    </Card>
  );
}
