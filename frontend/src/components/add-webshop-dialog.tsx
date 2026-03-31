"use client"

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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

export function AddWebShopDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Dodaj Web Shop</DialogTitle>
          <DialogDescription>
            Povežite WooCommerce web shop sa OLX profilom
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="olx-profile">OLX Profil</Label>
            <Select>
              <SelectTrigger id="olx-profile" className="w-full">
                <SelectValue placeholder="Odaberi profil" />
              </SelectTrigger>
              <SelectContent alignItemWithTrigger={false}>
                <SelectItem value="alpus">Alpus</SelectItem>
                <SelectItem value="tech_store_ba">tech_store_ba</SelectItem>
                <SelectItem value="auto_dijelovi">auto_dijelovi</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="woo-endpoint">Endpoint (Woo URL)</Label>
            <Input
              id="woo-endpoint"
              placeholder="https://example.com/wp-json/wc/v3"
            />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="sync-interval">Interval sinhronizacije</Label>
            <Select>
              <SelectTrigger id="sync-interval" className="w-full">
                <SelectValue placeholder="Odaberi interval" />
              </SelectTrigger>
              <SelectContent alignItemWithTrigger={false}>
                <SelectItem value="1">1 sat</SelectItem>
                <SelectItem value="3">3 sata</SelectItem>
                <SelectItem value="6">6 sati</SelectItem>
                <SelectItem value="12">12 sati</SelectItem>
                <SelectItem value="24">24 sata</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Otkaži
          </Button>
          <Button>Dodaj</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
