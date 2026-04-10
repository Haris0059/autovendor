"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SearchIcon,
  InfoIcon,
  DownloadIcon,
  PlusIcon,
  MoreHorizontalIcon,
  PencilIcon,
  Trash2Icon,
  CheckCircleIcon,
} from "lucide-react"
import { AddWebShopDialog } from "@/components/add-webshop-dialog"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

type WooStore = {
  id: string
  olxProfile: string
  endpoint: string
  status: "Aktivan" | "Neaktivan" | "Greška"
  interval: string
  lastCheck: string
  createdAt: string
}

const mockStores: WooStore[] = []

export default function WooCommercePage() {
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [infoDialogOpen, setInfoDialogOpen] = useState(false)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("Svi")
  const [perPage, setPerPage] = useState<string>("8")

  const filteredStores = mockStores.filter((store) => {
    const matchesSearch =
      store.olxProfile.toLowerCase().includes(search.toLowerCase()) ||
      store.endpoint.toLowerCase().includes(search.toLowerCase())
    const matchesStatus =
      statusFilter === "Svi" || store.status === statusFilter
    return matchesSearch && matchesStatus
  })

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <SearchIcon className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Pretraži importe..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-52 pl-8"
          />
        </div>

        <Button variant="outline" size="icon" className="size-8" onClick={() => setInfoDialogOpen(true)}>
          <InfoIcon className="size-4" />
        </Button>

        <Button variant="outline">
          <DownloadIcon className="mr-2 size-4" />
          Preuzmi plugin
        </Button>

        <Button onClick={() => setAddDialogOpen(true)}>
          <PlusIcon className="mr-2 size-4" />
          Dodaj Web Shop
        </Button>

        <div className="ml-auto flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Filtriraj po:</span>
          <Select value={statusFilter} onValueChange={(v) => v && setStatusFilter(v)}>
            <SelectTrigger>
              <CheckCircleIcon className="size-4 text-muted-foreground" />
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Svi">Svi</SelectItem>
              <SelectItem value="Aktivan">Aktivan</SelectItem>
              <SelectItem value="Neaktivan">Neaktivan</SelectItem>
              <SelectItem value="Greška">Greška</SelectItem>
            </SelectContent>
          </Select>

          <Select value={perPage} onValueChange={(v) => v && setPerPage(v)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="8">8</SelectItem>
              <SelectItem value="16">16</SelectItem>
              <SelectItem value="32">32</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Table */}
      <Card>
        <CardHeader>
          <CardTitle>Web Shopovi</CardTitle>
          <CardDescription>
            {filteredStores.length}{" "}
            {filteredStores.length === 1 ? "import zapis" : "import zapisa"}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-16">R.B</TableHead>
              <TableHead>OLX Profil</TableHead>
              <TableHead>Endpoint</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Interval</TableHead>
              <TableHead>Zadnja Provjera</TableHead>
              <TableHead>Kreirano</TableHead>
              <TableHead className="w-20 text-right">Akcije</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredStores.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={8}
                  className="h-32 text-center text-muted-foreground"
                >
                  Nema import zapisa.
                </TableCell>
              </TableRow>
            ) : (
              filteredStores.map((store, index) => (
                <TableRow key={store.id}>
                  <TableCell className="text-muted-foreground">
                    {index + 1}
                  </TableCell>
                  <TableCell className="font-medium">
                    {store.olxProfile}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {store.endpoint}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={
                        store.status === "Aktivan"
                          ? "default"
                          : store.status === "Greška"
                            ? "destructive"
                            : "secondary"
                      }
                    >
                      {store.status}
                    </Badge>
                  </TableCell>
                  <TableCell>{store.interval}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {store.lastCheck}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {store.createdAt}
                  </TableCell>
                  <TableCell className="text-right">
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        render={
                          <Button
                            variant="ghost"
                            size="icon"
                            className="size-8"
                          />
                        }
                      >
                        <MoreHorizontalIcon className="size-4" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem>
                          <PencilIcon className="mr-2 size-4" />
                          Uredi
                        </DropdownMenuItem>
                        <DropdownMenuItem variant="destructive">
                          <Trash2Icon className="mr-2 size-4" />
                          Obriši
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
          </Table>
        </CardContent>
      </Card>

      <AddWebShopDialog
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
      />

      <Dialog open={infoDialogOpen} onOpenChange={setInfoDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Uputstva za WooCommerce import</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4 text-sm text-muted-foreground">
            <p>
              Svaki red u tabeli predstavlja jednu konekciju između
              WooCommerce sajta i OLX profila.
            </p>
            <div>
              <p className="font-semibold text-foreground">Kako početi?</p>
              <p>
                Kliknite &quot;+ Dodaj Web Shop&quot; da povežete WooCommerce
                sajt sa željenim OLX profilom i odaberete interval
                sinhronizacije.
              </p>
            </div>
            <div className="flex flex-col gap-1">
              <p className="font-semibold text-foreground">Start / Stop dugme</p>
              <p>
                <span className="font-medium text-foreground">Start</span> —
                pokreće automatsko preuzimanje artikala sa vašeg WooCommerce
                sajta. Artikli se ažuriraju prema odabranom intervalu.
              </p>
              <p>
                <span className="font-medium text-foreground">Stop</span> —
                zaustavlja preuzimanje podataka sa WooCommerce sajta.
              </p>
            </div>
            <div className="flex flex-col gap-1">
              <p className="font-semibold text-foreground">Play / Pause dugme</p>
              <p>Kontroliše objavu artikala na OLX.</p>
              <p>
                Kada je aktivno, sistem svake minute objavljuje po 1
                neobjavljeni artikal sa WooCommerce sajta.
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
