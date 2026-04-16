"use client"

import { useState } from "react"
import { 
  useSponsoredListings, 
  useDiscounts, 
  useEndSponsor, 
  useEndDiscount 
} from "@/hooks/use-sponsored"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  ZapIcon,
  TrendingDownIcon,
  Loader2Icon,
  XCircleIcon,
  ClockIcon,
  WalletIcon,
} from "lucide-react"
import { toast } from "sonner"
import { formatDate } from "@/lib/utils"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"

export default function SponsoredPage() {
  const { data: sponsored, isLoading: sponsoredLoading } = useSponsoredListings()
  const { data: discounts, isLoading: discountsLoading } = useDiscounts()
  const endSponsor = useEndSponsor()
  const endDiscount = useEndDiscount()

  const [endSponsorId, setEndSponsorId] = useState<number | null>(null)
  const [endDiscountId, setEndDiscountId] = useState<number | null>(null)

  const handleEndSponsor = () => {
    if (endSponsorId) {
      endSponsor.mutate(endSponsorId, {
        onSuccess: () => {
          toast.success("Sponzorstvo uspješno prekinuto.")
          setEndSponsorId(null)
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : "Greška pri prekidu sponzorstva."
          toast.error(message)
        }
      })
    }
  }

  const handleEndDiscount = () => {
    if (endDiscountId) {
      endDiscount.mutate(endDiscountId, {
        onSuccess: () => {
          toast.success("Sniženje uspješno prekinuto.")
          setEndDiscountId(null)
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : "Greška pri prekidu sniženja."
          toast.error(message)
        }
      })
    }
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Sponzorstva i Sniženja</h1>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Aktivna sponzorstva</CardTitle>
            <ZapIcon className="size-4 text-yellow-500 fill-yellow-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{sponsored?.length || 0}</div>
            <p className="text-xs text-muted-foreground">Artikala sa istaknutom vidljivosti</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Aktivna sniženja</CardTitle>
            <TrendingDownIcon className="size-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{discounts?.length || 0}</div>
            <p className="text-xs text-muted-foreground">Artikala sa sniženom cijenom</p>
          </CardContent>
        </Card>
      </div>

      <Tabs defaultValue="sponsored" className="w-full">
        <TabsList>
          <TabsTrigger value="sponsored">
            <ZapIcon className="mr-2 size-4" />
            Sponzorstva
          </TabsTrigger>
          <TabsTrigger value="discounts">
            <TrendingDownIcon className="mr-2 size-4" />
            Sniženja
          </TabsTrigger>
        </TabsList>

        <TabsContent value="sponsored" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Aktivna Sponzorstva</CardTitle>
              <CardDescription>Pregled artikala koji trenutno koriste OLX sponzorstvo.</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Artikal ID</TableHead>
                    <TableHead>Tip</TableHead>
                    <TableHead>Trajanje</TableHead>
                    <TableHead>Završava</TableHead>
                    <TableHead>Cijena</TableHead>
                    <TableHead className="w-20 text-right">Akcije</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {sponsoredLoading ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center">
                        <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ) : !sponsored || sponsored.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                        Nema aktivnih sponzorstava.
                      </TableCell>
                    </TableRow>
                  ) : (
                    sponsored.map((s) => (
                      <TableRow key={s.id}>
                        <TableCell className="font-medium">#{s.listing_id}</TableCell>
                        <TableCell>
                          {s.type === 2 ? "Izdvojena" : "U kategoriji"}
                        </TableCell>
                        <TableCell>{s.days} dana</TableCell>
                        <TableCell className="text-muted-foreground">
                          {formatDate(s.ends_at)}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-1 font-medium">
                            <WalletIcon className="size-3 text-muted-foreground" />
                            {s.price_total}
                          </div>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="size-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                            onClick={() => setEndSponsorId(s.id)}
                          >
                            <XCircleIcon className="size-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="discounts" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Aktivna Sniženja</CardTitle>
              <CardDescription>Pregled artikala sa aktivnim popustom na OLX-u.</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Artikal ID</TableHead>
                    <TableHead>Stara cijena</TableHead>
                    <TableHead>Nova cijena</TableHead>
                    <TableHead>Popust</TableHead>
                    <TableHead>Završava</TableHead>
                    <TableHead className="w-20 text-right">Akcije</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {discountsLoading ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center">
                        <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ) : !discounts || discounts.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                        Nema aktivnih sniženja.
                      </TableCell>
                    </TableRow>
                  ) : (
                    discounts.map((d) => (
                      <TableRow key={d.id}>
                        <TableCell className="font-medium">#{d.listing_id}</TableCell>
                        <TableCell className="text-muted-foreground line-through">
                          {d.original_price.toFixed(2)} KM
                        </TableCell>
                        <TableCell className="font-bold text-red-600">
                          {d.discount_price.toFixed(2)} KM
                        </TableCell>
                        <TableCell>
                          <div className="rounded bg-red-50 px-1.5 py-0.5 text-xs font-bold text-red-600 dark:bg-red-900/30">
                            -{Math.round(((d.original_price - d.discount_price) / d.original_price) * 100)}%
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          <div className="flex items-center gap-1.5">
                            <ClockIcon className="size-3" />
                            {formatDate(d.ends_at)}
                          </div>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="size-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                            onClick={() => setEndDiscountId(d.id)}
                          >
                            <XCircleIcon className="size-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={!!endSponsorId}
        onOpenChange={(open) => !open && setEndSponsorId(null)}
        onConfirm={handleEndSponsor}
        title="Prekini sponzorstvo?"
        description="Ova akcija će odmah prekinuti sponzorstvo artikla. Preostali krediti neće biti vraćeni."
        confirmText="Prekini"
        variant="destructive"
        isLoading={endSponsor.isPending}
      />

      <ConfirmDialog
        open={!!endDiscountId}
        onOpenChange={(open) => !open && setEndDiscountId(null)}
        onConfirm={handleEndDiscount}
        title="Prekini sniženje?"
        description="Artikal će se vratiti na originalnu cijenu na OLX.ba."
        confirmText="Prekini"
        variant="destructive"
        isLoading={endDiscount.isPending}
      />
    </div>
  )
}
