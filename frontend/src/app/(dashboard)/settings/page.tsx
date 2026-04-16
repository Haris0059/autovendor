"use client"

import { useState } from "react"
import { useAuth } from "@/hooks/use-auth"
import { useTheme } from "next-themes"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs"
import { Switch } from "@/components/ui/switch"
import { 
  UserIcon, 
  BellIcon, 
  PaletteIcon, 
  MoonIcon,
  SunIcon,
  MonitorIcon,
  Loader2Icon
} from "lucide-react"
import { toast } from "sonner"

export default function SettingsPage() {
  const { user } = useAuth()
  const { theme, setTheme } = useTheme()
  const [loading, setLoading] = useState(false)

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setTimeout(() => {
      setLoading(false)
      toast.success("Profil je uspješno ažuriran.")
    }, 800)
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <h1 className="text-2xl font-bold tracking-tight">Postavke</h1>

      <Tabs defaultValue="profile" className="w-full">
        <TabsList className="grid w-full grid-cols-3 lg:w-[400px]">
          <TabsTrigger value="profile">
            <UserIcon className="mr-2 size-4" />
            Profil
          </TabsTrigger>
          <TabsTrigger value="notifications">
            <BellIcon className="mr-2 size-4" />
            Obavijesti
          </TabsTrigger>
          <TabsTrigger value="appearance">
            <PaletteIcon className="mr-2 size-4" />
            Izgled
          </TabsTrigger>
        </TabsList>

        <TabsContent value="profile" className="mt-6">
          <div className="grid gap-6">
            <Card>
              <CardHeader>
                <CardTitle>Lični podaci</CardTitle>
                <CardDescription>Upravljajte vašim korisničkim podacima i email adresom.</CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSaveProfile} className="grid gap-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="grid gap-2">
                      <Label htmlFor="first-name">Ime</Label>
                      <Input id="first-name" defaultValue={user?.email.split('@')[0]} />
                    </div>
                    <div className="grid gap-2">
                      <Label htmlFor="last-name">Prezime</Label>
                      <Input id="last-name" placeholder="Prezime" />
                    </div>
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="email">Email adresa</Label>
                    <Input id="email" type="email" defaultValue={user?.email} disabled />
                    <p className="text-[10px] text-muted-foreground">Email adresa se ne može mijenjati.</p>
                  </div>
                  <Button type="submit" className="w-fit" disabled={loading}>
                    {loading && <Loader2Icon className="mr-2 size-4 animate-spin" />}
                    Sačuvaj promjene
                  </Button>
                </form>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Promjena lozinke</CardTitle>
                <CardDescription>Redovno mijenjajte lozinku radi veće sigurnosti vašeg naloga.</CardDescription>
              </CardHeader>
              <CardContent className="grid gap-4">
                <div className="grid gap-2">
                  <Label htmlFor="current">Trenutna lozinka</Label>
                  <Input id="current" type="password" />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="new">Nova lozinka</Label>
                  <Input id="new" type="password" />
                </div>
                <Button variant="outline" className="w-fit">Promijeni lozinku</Button>
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="notifications" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Email obavijesti</CardTitle>
              <CardDescription>Odaberite koje obavijesti želite primati na email.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-6">
              <div className="flex items-center justify-between space-x-2">
                <div className="flex flex-col space-y-1">
                  <Label className="text-sm font-medium leading-none">Greške u sinhronizaciji</Label>
                  <p className="text-xs text-muted-foreground text-balance">Primite email ukoliko sinhronizacija artikla ne uspije.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <div className="flex items-center justify-between space-x-2">
                <div className="flex flex-col space-y-1">
                  <Label className="text-sm font-medium leading-none">Istek sponzorstva</Label>
                  <p className="text-xs text-muted-foreground text-balance">Obavještenje 24h prije isteka OLX sponzorstva.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <div className="flex items-center justify-between space-x-2">
                <div className="flex flex-col space-y-1">
                  <Label className="text-sm font-medium leading-none">Niski balans kredita</Label>
                  <p className="text-xs text-muted-foreground text-balance">Obavijesti me kada stanje OLX kredita padne ispod 50.</p>
                </div>
                <Switch />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="appearance" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Tema interfejsa</CardTitle>
              <CardDescription>Prilagodite izgled aplikacije vašim željama.</CardDescription>
            </CardHeader>
            <CardContent className="grid gap-4">
              <div className="grid grid-cols-3 gap-4">
                <Button 
                  variant={theme === "light" ? "default" : "outline"} 
                  className="flex flex-col gap-2 h-auto py-4"
                  onClick={() => setTheme("light")}
                >
                  <SunIcon className="size-6" />
                  <span>Svijetla</span>
                </Button>
                <Button 
                  variant={theme === "dark" ? "default" : "outline"} 
                  className="flex flex-col gap-2 h-auto py-4"
                  onClick={() => setTheme("dark")}
                >
                  <MoonIcon className="size-6" />
                  <span>Tamna</span>
                </Button>
                <Button 
                  variant={theme === "system" ? "default" : "outline"} 
                  className="flex flex-col gap-2 h-auto py-4"
                  onClick={() => setTheme("system")}
                >
                  <MonitorIcon className="size-6" />
                  <span>Sistemska</span>
                </Button>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
