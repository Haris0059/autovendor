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

export function AddProfileDialog({
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
          <DialogTitle>Dodaj OLX Profil</DialogTitle>
          <DialogDescription>
            Unesite podatke za prijavu na OLX.ba račun
          </DialogDescription>
        </DialogHeader>

        <form className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor="add-username">Korisničko ime</Label>
            <Input id="add-username" placeholder="npr. shop_sarajevo" />
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="add-password">Lozinka</Label>
            <Input id="add-password" type="password" placeholder="••••••••" />
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
