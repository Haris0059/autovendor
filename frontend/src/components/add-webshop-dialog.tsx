"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { toast } from "sonner"
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
import { Label } from "@/components/ui/label"
import { useCreateWooStore, useTestWooConnection } from "@/hooks/use-woo-stores"
import { Loader2Icon, CheckCircle2Icon, XCircleIcon } from "lucide-react"
import { toastMessages } from "@/lib/toast-messages"

const formSchema = z.object({
  name: z.string().min(2, "Naziv mora imati barem 2 znaka"),
  store_url: z.string().url("Unesite ispravan URL (npr. https://mojshop.ba)"),
  api_key: z.string().min(10, "API ključ mora imati barem 10 znakova"),
})

type FormValues = z.infer<typeof formSchema>

export function AddWebShopDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const createStore = useCreateWooStore()
  const testConnection = useTestWooConnection()

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      store_url: "",
      api_key: "",
    },
  })

  const handleOpenChange = (newOpen: boolean) => {
    if (!newOpen) {
      form.reset()
      testConnection.reset()
    }
    onOpenChange(newOpen)
  }

  const onTest = async () => {
    const isValid = await form.trigger(["store_url", "api_key"])
    if (!isValid) return

    const values = form.getValues()
    testConnection.mutate(
      { store_url: values.store_url, api_key: values.api_key },
      {
        onSuccess: (data) => {
          if (data.ok) {
            toast.success(`Konekcija uspješna! Pronađeno ${data.products_count} proizvoda.`)
          } else {
            toast.error("Konekcija nije uspjela. Provjerite podatke.")
          }
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : "Greška pri testiranju konekcije."
          toast.error(message)
        },
      }
    )
  }

  const onSubmit = (values: FormValues) => {
    createStore.mutate(values, {
      onSuccess: () => {
        toast.success(toastMessages.woo.createSuccess)
        handleOpenChange(false)
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : toastMessages.woo.createError
        toast.error(message)
      },
    })
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Dodaj Web Shop</DialogTitle>
          <DialogDescription>
            Povežite WooCommerce web shop koristeći AutoVendor plugin
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="name">Naziv shopa</Label>
            <Input
              id="name"
              placeholder="npr. Moj WooCommerce Shop"
              {...form.register("name")}
            />
            {form.formState.errors.name && (
              <p className="text-xs text-destructive">{form.formState.errors.name.message}</p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="store_url">URL shopa</Label>
            <Input
              id="store_url"
              placeholder="https://example.com"
              {...form.register("store_url")}
            />
            {form.formState.errors.store_url && (
              <p className="text-xs text-destructive">{form.formState.errors.store_url.message}</p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor="api_key">API ključ</Label>
            <div className="relative">
              <Input
                id="api_key"
                type="password"
                placeholder="Unesite ključ iz plugina"
                {...form.register("api_key")}
              />
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                {testConnection.isPending && <Loader2Icon className="size-4 animate-spin text-muted-foreground" />}
                {testConnection.isSuccess && testConnection.data?.ok && (
                  <CheckCircle2Icon className="size-4 text-green-500" />
                )}
                {testConnection.isError && <XCircleIcon className="size-4 text-destructive" />}
              </div>
            </div>
            {form.formState.errors.api_key && (
              <p className="text-xs text-destructive">{form.formState.errors.api_key.message}</p>
            )}
            <p className="text-[10px] text-muted-foreground">
              Ključ možete pronaći u postavkama AutoVendor plugina na vašem WordPress sajtu.
            </p>
          </div>

          <Button
            type="button"
            variant="secondary"
            size="sm"
            className="w-full"
            onClick={onTest}
            disabled={testConnection.isPending}
          >
            {testConnection.isPending ? (
              <Loader2Icon className="mr-2 size-4 animate-spin" />
            ) : (
              "Testiraj konekciju"
            )}
          </Button>

          <DialogFooter className="mt-2">
            <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
              Otkaži
            </Button>
            <Button type="submit" disabled={createStore.isPending}>
              {createStore.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
              Dodaj
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
