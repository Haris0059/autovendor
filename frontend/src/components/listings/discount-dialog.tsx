"use client"

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
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { useCreateDiscount } from "@/hooks/use-sponsored"
import { toast } from "sonner"
import { Loader2Icon, TagIcon, InfoIcon, TrendingDownIcon } from "lucide-react"
import { Field, FieldLabel, FieldError } from "@/components/ui/field"

const discountSchema = z.object({
  original_price: z.number().positive("Cijena mora biti pozitivan broj"),
  discount_price: z.number().positive("Snižena cijena mora biti pozitivan broj"),
  days: z.enum(["3", "7", "30"]),
}).refine((data) => data.discount_price < data.original_price, {
  message: "Snižena cijena mora biti manja od originalne",
  path: ["discount_price"],
})

type DiscountFormValues = z.infer<typeof discountSchema>

interface DiscountDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  listingId: number
  listingTitle: string
  currentPrice?: number
}

export function DiscountDialog({
  open,
  onOpenChange,
  listingId,
  listingTitle,
  currentPrice,
}: DiscountDialogProps) {
  const createDiscount = useCreateDiscount()
  
  const form = useForm<DiscountFormValues>({
    resolver: zodResolver(discountSchema),
    defaultValues: {
      original_price: currentPrice || 0,
      discount_price: 0,
      days: "7",
    },
  })

  const { formState: { errors } } = form
  const { original_price, discount_price } = form.watch()
  const saving = original_price && discount_price ? original_price - discount_price : 0
  const savingPercent = original_price && discount_price ? Math.round((saving / original_price) * 100) : 0

  const onSubmit = (values: DiscountFormValues) => {
    createDiscount.mutate({
      listing_id: listingId,
      original_price: values.original_price,
      discount_price: values.discount_price,
      days: parseInt(values.days) as 3 | 7 | 30,
    }, {
      onSuccess: () => {
        toast.success("Sniženje je uspješno aktivirano.")
        onOpenChange(false)
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : "Greška pri aktivaciji sniženja."
        toast.error(message)
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <TrendingDownIcon className="size-5 text-red-500" />
            Aktiviraj sniženje
          </DialogTitle>
          <DialogDescription className="truncate">
            Postavite sniženu cijenu za: <span className="font-semibold text-foreground">{listingTitle}</span>
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 py-4">
          <div className="grid grid-cols-2 gap-4">
            <Field data-invalid={!!errors.original_price || undefined}>
              <FieldLabel htmlFor="original_price">Originalna cijena</FieldLabel>
              <div className="relative">
                <Input
                  id="original_price"
                  type="number"
                  step="0.01"
                  {...form.register("original_price", { valueAsNumber: true })}
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-muted-foreground font-medium">KM</span>
              </div>
              <FieldError errors={errors.original_price ? [errors.original_price] : undefined} />
            </Field>
            
            <Field data-invalid={!!errors.discount_price || undefined}>
              <FieldLabel htmlFor="discount_price">Snižena cijena</FieldLabel>
              <div className="relative">
                <Input
                  id="discount_price"
                  type="number"
                  step="0.01"
                  {...form.register("discount_price", { valueAsNumber: true })}
                  className="border-red-200 focus-visible:ring-red-500"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-red-500 font-bold">KM</span>
              </div>
              <FieldError errors={errors.discount_price ? [errors.discount_price] : undefined} />
            </Field>
          </div>

          {saving > 0 && (
            <div className="rounded-md bg-green-50 p-3 flex items-center justify-between dark:bg-green-900/20">
              <div className="flex items-center gap-2">
                <TagIcon className="size-4 text-green-600" />
                <span className="text-sm font-medium text-green-800 dark:text-green-300">Ušteda za kupce:</span>
              </div>
              <span className="text-sm font-bold text-green-700 dark:text-green-400">
                {saving.toFixed(2)} KM ({savingPercent}%)
              </span>
            </div>
          )}

          <div className="grid gap-3">
            <FieldLabel>Trajanje sniženja</FieldLabel>
            <RadioGroup 
              value={form.watch("days")} 
              onValueChange={(v) => form.setValue("days", v as "3" | "7" | "30")}
              className="grid grid-cols-3 gap-2"
            >
              {[3, 7, 30].map(d => (
                <div key={d}>
                  <RadioGroupItem value={d.toString()} id={`d-${d}`} className="peer sr-only" />
                  <label
                    htmlFor={`d-${d}`}
                    className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-popover p-2 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-red-500 [&:has([data-state=checked])]:border-red-500 cursor-pointer"
                  >
                    <span className="text-sm font-bold">{d}</span>
                    <span className="text-[10px] text-muted-foreground uppercase">dana</span>
                  </label>
                </div>
              ))}
            </RadioGroup>
          </div>

          <div className="rounded-md bg-blue-50 p-3 flex gap-3 dark:bg-blue-900/20">
            <InfoIcon className="size-4 text-blue-600 shrink-0 mt-0.5" />
            <p className="text-xs text-blue-800 dark:text-blue-300">
              Artikal će na OLX-u biti prikazan sa precrtanom starom cijenom i oznakom SNIŽENO.
            </p>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Otkaži
          </Button>
          <Button 
            className="bg-red-600 hover:bg-red-700 text-white"
            onClick={form.handleSubmit(onSubmit)} 
            disabled={createDiscount.isPending}
          >
            {createDiscount.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
            Aktiviraj sniženje
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
