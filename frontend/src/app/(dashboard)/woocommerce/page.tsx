"use client"

import { useState } from "react"
import Link from "next/link"
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
  ExternalLinkIcon,
  Loader2Icon,
  GlobeIcon,
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
import { useWooStores, useDeleteWooStore } from "@/hooks/use-woo-stores"
import { toast } from "sonner"
import { toastMessages } from "@/lib/toast-messages"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { formatDate } from "@/lib/utils"
import { PageHeader } from "@/components/shared/page-header"

export default function WooCommercePage() {
  const { data: stores, isLoading } = useWooStores()
  const deleteStore = useDeleteWooStore()

  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [infoDialogOpen, setInfoDialogOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [search, setSearch] = useState("")
  const [perPage, setPerPage] = useState<string>("10")

  const filteredStores = stores?.filter((store) => {
    const matchesSearch =
      store.name.toLowerCase().includes(search.toLowerCase()) ||
      store.store_url.toLowerCase().includes(search.toLowerCase())
    return matchesSearch
  })

  const handleDelete = () => {
    if (deleteId) {
      deleteStore.mutate(deleteId, {
        onSuccess: () => {
          toast.success(toastMessages.woo.deleteSuccess)
          setDeleteId(null)
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : toastMessages.woo.deleteError
          toast.error(message)
        },
      })
    }
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <PageHeader
        title="WooCommerce"
        description="Upravljajte vašim povezanim WooCommerce prodavnicama."
      >
        <div className="flex items-center gap-2">
          <Button variant="outline" size="icon" className="size-8" onClick={() => setInfoDialogOpen(true)}>
            <InfoIcon className="size-4" />
          </Button>

          <Button variant="outline" size="sm">
            <DownloadIcon className="mr-2 size-4" />
            Preuzmi plugin
          </Button>

          <Button size="sm" onClick={() => setAddDialogOpen(true)}>
            <PlusIcon className="mr-2 size-4" />
            Dodaj Web Shop
          </Button>
        </div>
      </PageHeader>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative">
          <SearchIcon className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Pretraži shopove..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-52 pl-8"
          />
        </div>

        <div className="ml-auto flex items-center gap-2">
          <Select value={perPage} onValueChange={(v) => v && setPerPage(v)}>
            <SelectTrigger className="h-9 w-24">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="10">10 / str</SelectItem>
              <SelectItem value="20">20 / str</SelectItem>
              <SelectItem value="50">50 / str</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Table */}
      <Card>
        <CardHeader>
          <CardTitle>Povezani WooCommerce Shopovi</CardTitle>
          <CardDescription>
            {isLoading ? (
              "Učitavanje..."
            ) : (
              <>
                {filteredStores?.length || 0}{" "}
                {(filteredStores?.length || 0) === 1 ? "povezan shop" : "povezanih shopova"}
              </>
            )}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Naziv</TableHead>
                <TableHead>URL Prodavnice</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Povezano</TableHead>
                <TableHead className="w-20 text-right">Akcije</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} className="h-32 text-center">
                    <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : !filteredStores || filteredStores.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="h-32 text-center text-muted-foreground"
                  >
                    Nema povezanih WooCommerce shopova.
                  </TableCell>
                </TableRow>
              ) : (
                filteredStores.map((store) => (
                  <TableRow key={store.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <GlobeIcon className="size-4 text-muted-foreground" />
                        <Link
                          href={`/woocommerce/${store.id}`}
                          className="hover:underline"
                        >
                          {store.name}
                        </Link>
                      </div>
                    </TableCell>
                    <TableCell>
                      <a
                        href={store.store_url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1 text-muted-foreground hover:text-foreground hover:underline"
                      >
                        {store.store_url}
                        <ExternalLinkIcon className="size-3" />
                      </a>
                    </TableCell>
                    <TableCell>
                      <Badge variant="default">Povezano</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDate(store.created_at)}
                    </TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          nativeButton
                          render={
                            <Button
                              variant="ghost"
                              size="icon"
                              className="size-8"
                            >
                              <MoreHorizontalIcon className="size-4" />
                            </Button>
                          }
                        />
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem render={<Link href={`/woocommerce/${store.id}`} />}>
                              <PencilIcon className="mr-2 size-4" />
                              Uredi
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            variant="destructive"
                            onClick={() => setDeleteId(store.id)}
                          >
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

      <ConfirmDialog
        open={!!deleteId}
        onOpenChange={(open) => !open && setDeleteId(null)}
        onConfirm={handleDelete}
        title="Obriši Web Shop?"
        description="Ova akcija će trajno ukloniti konekciju sa ovim WooCommerce shopom. Sva mapiranja povezana sa ovim shopom će biti obrisana."
        confirmLabel="Obriši"
        destructive
        loading={deleteStore.isPending}
      />

      <Dialog open={infoDialogOpen} onOpenChange={setInfoDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Uputstva za WooCommerce integraciju</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4 text-sm text-muted-foreground">
            <p>
              AutoVendor se povezuje sa vašim WooCommerce shopom koristeći naš namjenski WordPress plugin.
            </p>
            <div>
              <p className="font-semibold text-foreground">Kako se povezati?</p>
              <ol className="ml-4 mt-1 list-decimal space-y-1">
                <li>Preuzmite i instalirajte AutoVendor plugin na vaš WordPress sajt.</li>
                <li>Aktivirajte plugin i idite na WooCommerce &rarr; AutoVendor.</li>
                <li>Kopirajte generisani API ključ.</li>
                <li>Kliknite &quot;+ Dodaj Web Shop&quot; ovdje i unesite URL sajta i API ključ.</li>
              </ol>
            </div>
            <div className="flex flex-col gap-1">
              <p className="font-semibold text-foreground">Šta dobijate integracijom?</p>
              <p>
                - Automatski uvoz proizvoda sa vašeg sajta.<br />
                - Sinhronizaciju stanja zaliha i cijena.<br />
                - Mogućnost mapiranja WooCommerce kategorija sa OLX kategorijama.<br />
                - Automatsko objavljivanje novih proizvoda na OLX.
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
