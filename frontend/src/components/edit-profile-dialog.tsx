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
import { Badge } from "@/components/ui/badge"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"

export type OlxAccount = {
  id: string
  username: string
  email: string
  phone: string
  businessName: string
  package: string
  credits: number
  tokenValid: boolean
  status: string
  listings: number
  lastSync: string
  avatar?: string
}

export function EditProfileDialog({
  account,
  open,
  onOpenChange,
}: {
  account: OlxAccount | null
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  if (!account) return null

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Uredi Profil</DialogTitle>
          <DialogDescription>
            Samo lozinka se može mijenjati. Ostali podaci su preuzeti sa OLX.ba.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex gap-4">
            <div className="flex shrink-0 items-start">
              <Avatar className="size-32">
                <AvatarImage src={account.avatar} alt={account.username} className="object-cover" />
                <AvatarFallback className="text-2xl">
                  {account.username.slice(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
            </div>
            <div className="grid flex-1 grid-cols-2 gap-3">
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Username</Label>
                <p className="text-sm font-medium">{account.username}</p>
              </div>
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Email</Label>
                <p className="text-sm font-medium">{account.email}</p>
              </div>
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Telefon</Label>
                <p className="text-sm font-medium">{account.phone}</p>
              </div>
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Firma</Label>
                <p className="text-sm font-medium">{account.businessName}</p>
              </div>
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Paket</Label>
                <p className="text-sm font-medium">{account.package}</p>
              </div>
              <div className="flex flex-col gap-1">
                <Label className="text-muted-foreground text-xs">Krediti</Label>
                <p className="text-sm font-medium">{account.credits}</p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Label className="text-muted-foreground text-xs">Token:</Label>
            <Badge variant={account.tokenValid ? "default" : "destructive"}>
              {account.tokenValid ? "Validan" : "Istekao"}
            </Badge>
          </div>

          <Separator />

          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Lozinka</Label>
              <Input
                id="password"
                type="password"
                placeholder="Nova lozinka"
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="password-confirm">Potvrdi lozinku</Label>
              <Input
                id="password-confirm"
                type="password"
                placeholder="Ponovi lozinku"
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Koristi se za automatsko osvježavanje tokena
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Otkaži
          </Button>
          <Button>Sačuvaj</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
