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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { useOlxAccounts } from "@/hooks/use-olx-accounts"
import { useAllListings } from "@/hooks/use-listings"
import { useWooStores, useWooStoreProducts } from "@/hooks/use-woo-stores"
import { useCreateProductLink } from "@/hooks/use-sync"
import { toast } from "sonner"
import { toastMessages } from "@/lib/toast-messages"
import { Loader2Icon, ArrowRightLeftIcon, ShoppingCartIcon, LayoutGridIcon } from "lucide-react"
import { Field, FieldLabel, FieldError } from "@/components/ui/field"

const linkSchema = z.object({
  olx_account_id: z.string().min(1, "Odaberite OLX profil"),
  woo_store_id: z.string().min(1, "Odaberite shop"),
  olx_listing_id: z.string().min(1, "Odaberite OLX artikal"),
  woo_product_id: z.string().min(1, "Odaberite WooCommerce proizvod"),
  sync_direction: z.enum(["woo_to_olx", "olx_to_woo"]),
})

type LinkFormValues = z.infer<typeof linkSchema>

interface LinkProductDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialWooStoreId?: number
  initialWooProductId?: number
}

export function LinkProductDialog({
  open,
  onOpenChange,
  initialWooStoreId,
  initialWooProductId,
}: LinkProductDialogProps) {
  const { data: accounts } = useOlxAccounts()
  const { data: stores } = useWooStores()
  const { data: allListings, isLoading: listingsLoading } = useAllListings()
  const createLink = useCreateProductLink()

  const form = useForm<LinkFormValues>({
    resolver: zodResolver(linkSchema),
    defaultValues: {
      olx_account_id: "",
      woo_store_id: initialWooStoreId?.toString() || "",
      olx_listing_id: "",
      woo_product_id: initialWooProductId?.toString() || "",
      sync_direction: "woo_to_olx",
    },
  })

  const { formState: { errors } } = form
  const selectedStoreId = form.watch("woo_store_id")
  const selectedAccountId = form.watch("olx_account_id")
  
  const { data: wooProducts, isLoading: wooLoading } = useWooStoreProducts(
    selectedStoreId ? parseInt(selectedStoreId) : 0
  )

  const accountListings = allListings?.filter(
    l => l.account_id === (selectedAccountId ? parseInt(selectedAccountId) : 0)
  )

  const onSubmit = (values: LinkFormValues) => {
    createLink.mutate({
      olx_account_id: parseInt(values.olx_account_id),
      woo_store_id: parseInt(values.woo_store_id),
      olx_listing_id: parseInt(values.olx_listing_id),
      woo_product_id: parseInt(values.woo_product_id),
      sync_direction: values.sync_direction,
    }, {
      onSuccess: () => {
        toast.success(toastMessages.sync.linkSuccess)
        onOpenChange(false)
        form.reset()
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : toastMessages.sync.linkError
        toast.error(message)
      }
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Poveži artikle</DialogTitle>
          <DialogDescription>
            Ručno povežite postojeći OLX artikal sa WooCommerce proizvodom.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-6 py-4">
          <div className="grid grid-cols-2 gap-4">
            <Field data-invalid={!!errors.woo_store_id || undefined}>
              <FieldLabel>WooCommerce Shop</FieldLabel>
              <Select 
                value={form.watch("woo_store_id")} 
                onValueChange={(v) => form.setValue("woo_store_id", v ?? "")}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Odaberi shop" />
                </SelectTrigger>
                <SelectContent>
                  {stores?.map(s => (
                    <SelectItem key={s.id} value={s.id.toString()}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FieldError errors={errors.woo_store_id ? [errors.woo_store_id] : undefined} />
            </Field>
            
            <Field data-invalid={!!errors.olx_account_id || undefined}>
              <FieldLabel>OLX Profil</FieldLabel>
              <Select 
                value={form.watch("olx_account_id")} 
                onValueChange={(v) => form.setValue("olx_account_id", v ?? "")}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Odaberi profil" />
                </SelectTrigger>
                <SelectContent>
                  {accounts?.map(a => (
                    <SelectItem key={a.id} value={a.id.toString()}>{a.username}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FieldError errors={errors.olx_account_id ? [errors.olx_account_id] : undefined} />
            </Field>
          </div>

          <div className="grid gap-4 rounded-lg border p-4 bg-muted/30">
            <Field data-invalid={!!errors.woo_product_id || undefined}>
              <FieldLabel className="flex items-center gap-2">
                <ShoppingCartIcon className="size-3" />
                WooCommerce Proizvod
              </FieldLabel>
              <Select 
                value={form.watch("woo_product_id")} 
                onValueChange={(v) => form.setValue("woo_product_id", v ?? "")}
                disabled={!selectedStoreId || wooLoading}
              >
                <SelectTrigger>
                  <SelectValue placeholder={wooLoading ? "Učitavanje..." : "Odaberi proizvod"} />
                </SelectTrigger>
                <SelectContent>
                  {wooProducts?.map(p => (
                    <SelectItem key={p.id} value={p.id.toString()}>
                      {p.name} {p.sku ? `(${p.sku})` : ""}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FieldError errors={errors.woo_product_id ? [errors.woo_product_id] : undefined} />
            </Field>

            <div className="flex justify-center">
              <ArrowRightLeftIcon className="size-4 text-muted-foreground" />
            </div>

            <Field data-invalid={!!errors.olx_listing_id || undefined}>
              <FieldLabel className="flex items-center gap-2">
                <LayoutGridIcon className="size-3" />
                OLX Artikal
              </FieldLabel>
              <Select 
                value={form.watch("olx_listing_id")} 
                onValueChange={(v) => form.setValue("olx_listing_id", v ?? "")}
                disabled={!selectedAccountId || listingsLoading}
              >
                <SelectTrigger>
                  <SelectValue placeholder={listingsLoading ? "Učitavanje..." : "Odaberi artikal"} />
                </SelectTrigger>
                <SelectContent>
                  {accountListings?.map(l => (
                    <SelectItem key={l.id} value={l.id.toString()}>
                      {l.title} (ID: {l.id})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FieldError errors={errors.olx_listing_id ? [errors.olx_listing_id] : undefined} />
            </Field>
          </div>

          <div className="grid gap-3">
            <FieldLabel>Smjer sinhronizacije</FieldLabel>
            <RadioGroup 
              value={form.watch("sync_direction")} 
              onValueChange={(v) => form.setValue("sync_direction", v as "woo_to_olx" | "olx_to_woo")}
              className="flex flex-col gap-2"
            >
              <div className="flex items-center space-x-2 rounded-md border p-3 hover:bg-muted/50 cursor-pointer">
                <RadioGroupItem value="woo_to_olx" id="woo_to_olx" />
                <label htmlFor="woo_to_olx" className="flex flex-1 flex-col cursor-pointer">
                  <span className="font-medium text-sm text-foreground">WooCommerce &rarr; OLX</span>
                  <span className="text-xs text-muted-foreground">Promjene na web shopu će ažurirati OLX artikal.</span>
                </label>
              </div>
              <div className="flex items-center space-x-2 rounded-md border p-3 hover:bg-muted/50 cursor-pointer">
                <RadioGroupItem value="olx_to_woo" id="olx_to_woo" />
                <label htmlFor="olx_to_woo" className="flex flex-1 flex-col cursor-pointer">
                  <span className="font-medium text-sm text-foreground">OLX &rarr; WooCommerce</span>
                  <span className="text-xs text-muted-foreground">Promjene na OLX-u će ažurirati web shop (uskoro).</span>
                </label>
              </div>
            </RadioGroup>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Otkaži
          </Button>
          <Button 
            onClick={form.handleSubmit(onSubmit)} 
            disabled={createLink.isPending}
          >
            {createLink.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
            Poveži artikle
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
