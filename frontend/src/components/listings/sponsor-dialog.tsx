"use client"

import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Card,
  CardContent,
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
import { Checkbox } from "@/components/ui/checkbox"
import { useSponsorPrice, useCreateSponsor } from "@/hooks/use-sponsored"
import { toast } from "sonner"
import { Loader2Icon, ZapIcon, InfoIcon, WalletIcon } from "lucide-react"
import { Field, FieldLabel, FieldError } from "@/components/ui/field"

const sponsorSchema = z.object({
  type: z.enum(["1", "2"]), // 1: Category, 2: Global
  days: z.string().min(1),
  refresh_every: z.string().min(1),
  homepage: z.boolean(),
})

type SponsorFormValues = z.infer<typeof sponsorSchema>

interface SponsorDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  listingId: number
  listingTitle: string
}

export function SponsorDialog({
  open,
  onOpenChange,
  listingId,
  listingTitle,
}: SponsorDialogProps) {
  const createSponsor = useCreateSponsor()
  
  const form = useForm<SponsorFormValues>({
    resolver: zodResolver(sponsorSchema),
    defaultValues: {
      type: "1",
      days: "7",
      refresh_every: "1",
      homepage: false,
    },
  })

  const { formState: { errors } } = form
  const { type, days, refresh_every, homepage } = form.watch()
  
  const [priceInput, setPriceInput] = useState<{
    listing_id: number;
    type: 1 | 2;
    days: number;
    refresh_every: number;
    locations: string[];
  } | null>(null)

  useEffect(() => {
    const timer = setTimeout(() => {
      setPriceInput({
        listing_id: listingId,
        type: parseInt(type) as 1 | 2,
        days: parseInt(days),
        refresh_every: parseInt(refresh_every),
        locations: homepage ? ["homepage"] : [],
      })
    }, 250)
    return () => clearTimeout(timer)
  }, [listingId, type, days, refresh_every, homepage])

  const { data: price, isLoading: priceLoading } = useSponsorPrice(priceInput)

  const onSubmit = (values: SponsorFormValues) => {
    createSponsor.mutate({
      listing_id: listingId,
      type: parseInt(values.type) as 1 | 2,
      days: parseInt(values.days),
      refresh_every: parseInt(values.refresh_every),
      locations: values.homepage ? ["homepage"] : [],
    }, {
      onSuccess: () => {
        toast.success("Artikal je uspješno sponzorisan.")
        onOpenChange(false)
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : "Greška pri aktivaciji sponzorstva."
        toast.error(message)
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ZapIcon className="size-5 text-yellow-500 fill-yellow-500" />
            Sponzoriši artikal
          </DialogTitle>
          <DialogDescription className="truncate">
            Aktivirajte dodatnu vidljivost za: <span className="font-semibold text-foreground">{listingTitle}</span>
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 py-4">
          <Field data-invalid={!!errors.type || undefined}>
            <FieldLabel>Tip sponzorstva</FieldLabel>
            <Select value={type} onValueChange={(v) => form.setValue("type", (v as "1" | "2") ?? "1")}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1">Izdvojena u kategoriji</SelectItem>
                <SelectItem value="2">Izdvojena (Globalno)</SelectItem>
              </SelectContent>
            </Select>
            <FieldError errors={errors.type ? [errors.type] : undefined} />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field data-invalid={!!errors.days || undefined}>
              <FieldLabel>Trajanje (dana)</FieldLabel>
              <Select value={days} onValueChange={(v) => form.setValue("days", v ?? "7")}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {[1, 3, 7, 15, 30].map(d => (
                    <SelectItem key={d} value={d.toString()}>{d} {d === 1 ? "dan" : "dana"}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FieldError errors={errors.days ? [errors.days] : undefined} />
            </Field>
            
            <Field data-invalid={!!errors.refresh_every || undefined}>
              <FieldLabel>Automatsko obnavljanje</FieldLabel>
              <Select value={refresh_every} onValueChange={(v) => form.setValue("refresh_every", v ?? "0")}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="0">Bez obnavljanja</SelectItem>
                  <SelectItem value="1">Svaki sat</SelectItem>
                  <SelectItem value="6">Svakih 6 sati</SelectItem>
                  <SelectItem value="12">Svakih 12 sati</SelectItem>
                  <SelectItem value="24">Jednom dnevno</SelectItem>
                </SelectContent>
              </Select>
              <FieldError errors={errors.refresh_every ? [errors.refresh_every] : undefined} />
            </Field>
          </div>

          <div className="flex items-center space-x-2 rounded-md border p-3 hover:bg-muted/50 cursor-pointer">
            <Checkbox 
              id="homepage" 
              checked={homepage} 
              onCheckedChange={(v) => form.setValue("homepage", !!v)} 
            />
            <FieldLabel htmlFor="homepage" className="flex flex-1 flex-col gap-1 cursor-pointer">
              <span className="font-medium">Prikaži na naslovnici OLX.ba</span>
              <span className="text-xs text-muted-foreground">Maksimalna vidljivost za sve posjetioce.</span>
            </FieldLabel>
          </div>

          <Card className="bg-yellow-50/50 border-yellow-200 dark:bg-yellow-900/10 dark:border-yellow-900/30">
            <CardHeader className="py-3">
              <CardTitle className="text-sm flex items-center gap-2">
                <WalletIcon className="size-4 text-yellow-600" />
                Obračun troškova
              </CardTitle>
            </CardHeader>
            <CardContent className="py-0 pb-3 text-sm">
              <div className="space-y-1.5">
                <div className="flex justify-between text-muted-foreground">
                  <span>Osnovno sponzorstvo:</span>
                  <span>{priceLoading ? "..." : `${price?.search || 0} kredita`}</span>
                </div>
                <div className="flex justify-between text-muted-foreground">
                  <span>Obnavljanje (svakih {refresh_every}h):</span>
                  <span>{priceLoading ? "..." : `${price?.refresh || 0} kredita`}</span>
                </div>
                {homepage && (
                  <div className="flex justify-between text-muted-foreground">
                    <span>Naslovnica:</span>
                    <span>{priceLoading ? "..." : `${price?.locations || 0} kredita`}</span>
                  </div>
                )}
                <div className="flex justify-between border-t pt-2 font-bold text-foreground">
                  <span>Ukupno:</span>
                  <div className="flex items-center gap-2">
                    {priceLoading && <Loader2Icon className="size-3 animate-spin" />}
                    <span>{price?.total || 0} OLX Kredita</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <div className="rounded-md bg-blue-50 p-3 flex gap-3 dark:bg-blue-900/20">
            <InfoIcon className="size-4 text-blue-600 shrink-0 mt-0.5" />
            <p className="text-xs text-blue-800 dark:text-blue-300">
              Sponzorstvo će biti aktivirano odmah nakon potvrde. Provjerite stanje kredita na vašem OLX profilu.
            </p>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Otkaži
          </Button>
          <Button 
            className="bg-yellow-500 hover:bg-yellow-600 text-white"
            onClick={form.handleSubmit(onSubmit)} 
            disabled={createSponsor.isPending || priceLoading}
          >
            {createSponsor.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
            Aktiviraj sponzorisanje
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
