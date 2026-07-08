"use client"

import { useState } from "react"
import { 
  useCategoryMappings, 
  useCreateCategoryMapping, 
  useDeleteCategoryMapping 
} from "@/hooks/use-sync"
import { useWooStores, useWooStoreCategories } from "@/hooks/use-woo-stores"
import { useOlxCategories } from "@/hooks/use-categories"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Button } from "@/components/ui/button"
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { 
  PlusIcon, 
  Trash2Icon, 
  ArrowRightIcon, 
  Loader2Icon,
  SearchIcon,
  LayersIcon,
} from "lucide-react"
import { toast } from "sonner"
import { toastMessages } from "@/lib/toast-messages"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { Input } from "@/components/ui/input"
import { PageHeader } from "@/components/shared/page-header"

export default function CategoryMappingsPage() {
  const { data: mappings, isLoading: mappingsLoading } = useCategoryMappings()
  const { data: stores } = useWooStores()
  const createMapping = useCreateCategoryMapping()
  const deleteMapping = useDeleteCategoryMapping()

  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null)
  const { data: wooCategories, isLoading: wooLoading } = useWooStoreCategories(selectedStoreId || 0)
  const { data: olxCategories, isLoading: olxLoading } = useOlxCategories()

  const [isAddOpen, setIsAddOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [search, setSearch] = useState("")

  // Form state
  const [wooCatId, setWooCatId] = useState("")
  const [olxCatId, setOlxCatId] = useState("")

  const filteredMappings = mappings?.filter(m => 
    m.woo_category_name.toLowerCase().includes(search.toLowerCase()) ||
    m.olx_category_name.toLowerCase().includes(search.toLowerCase())
  )

  const handleAdd = () => {
    const wooCat = wooCategories?.find(c => c.id.toString() === wooCatId)
    const olxCat = olxCategories?.find(c => c.id.toString() === olxCatId)

    if (!wooCat || !olxCat) {
      toast.error("Morate odabrati obje kategorije.")
      return
    }

    createMapping.mutate({
      woo_category_id: wooCat.id,
      woo_category_name: wooCat.name,
      olx_category_id: olxCat.id,
      olx_category_name: olxCat.name,
    }, {
      onSuccess: () => {
        toast.success(toastMessages.sync.mappingCreateSuccess)
        setIsAddOpen(false)
        setWooCatId("")
        setOlxCatId("")
      },
      onError: (error: unknown) => {
        const message = error instanceof Error ? error.message : toastMessages.sync.mappingCreateError
        toast.error(message)
      }
    })
  }

  const handleDelete = () => {
    if (deleteId) {
      deleteMapping.mutate(deleteId, {
        onSuccess: () => {
          toast.success(toastMessages.sync.mappingDeleteSuccess)
          setDeleteId(null)
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : toastMessages.sync.mappingDeleteError
          toast.error(message)
        }
      })
    }
  }

  // Set initial store if not set
  if (!selectedStoreId && stores && stores.length > 0) {
    setSelectedStoreId(stores[0].id)
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <PageHeader
        title="Mapiranje kategorija"
        description="Definišite u koju OLX kategoriju se smještaju proizvodi iz WooCommerce kategorija."
      >
        <Button onClick={() => setIsAddOpen(true)}>
          <PlusIcon className="mr-2 size-4" />
          Novo mapiranje
        </Button>
      </PageHeader>

      <div className="flex items-center gap-3">
        <div className="relative flex-1 max-w-sm">
          <SearchIcon className="absolute left-2.5 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Pretraži mapiranja..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-8"
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">Woo Store:</span>
          <Select 
            value={stores?.find(s => s.id === selectedStoreId)?.name || ""} 
            onValueChange={(v) => {
              const store = stores?.find(s => s.name === v);
              setSelectedStoreId(store ? store.id : null);
            }}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Odaberi shop" />
            </SelectTrigger>
            <SelectContent>
              {stores?.map(s => (
                <SelectItem key={s.id} value={s.name}>{s.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Aktivna Pravila Mapiranja</CardTitle>
          <CardDescription>
            {filteredMappings?.length || 0} definisanih pravila.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>WooCommerce Kategorija</TableHead>
                <TableHead className="w-12 text-center"></TableHead>
                <TableHead>OLX Kategorija</TableHead>
                <TableHead className="w-20 text-right">Akcije</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mappingsLoading ? (
                <TableRow>
                  <TableCell colSpan={4} className="h-32 text-center">
                    <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : !filteredMappings || filteredMappings.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4} className="h-32 text-center text-muted-foreground">
                    Nema definisanih mapiranja. Kliknite na &quot;Novo mapiranje&quot; da počnete.
                  </TableCell>
                </TableRow>
              ) : (
                filteredMappings.map((mapping) => (
                  <TableRow key={mapping.id}>
                    <TableCell className="font-medium">
                      <div className="flex items-center gap-2">
                        <LayersIcon className="size-4 text-muted-foreground" />
                        {mapping.woo_category_name}
                      </div>
                    </TableCell>
                    <TableCell className="text-center">
                      <ArrowRightIcon className="mx-auto size-4 text-muted-foreground" />
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="rounded bg-blue-50 px-1.5 py-0.5 text-[10px] font-bold text-blue-600 dark:bg-blue-900/30">OLX</div>
                        {mapping.olx_category_name}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="size-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                        onClick={() => setDeleteId(mapping.id)}
                      >
                        <Trash2Icon className="size-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Add Mapping Dialog */}
      <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Novo mapiranje kategorija</DialogTitle>
            <DialogDescription>
              Povežite kategoriju iz vašeg WooCommerce shopa sa odgovarajućom OLX kategorijom.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="woo-cat">WooCommerce Kategorija</Label>
              <Select
                items={(wooCategories ?? []).map((c) => ({ value: c.id.toString(), label: c.name }))}
                value={wooCatId}
                onValueChange={(v) => setWooCatId(v ?? "")}
              >
                <SelectTrigger id="woo-cat">
                  <SelectValue placeholder={wooLoading ? "Učitavanje..." : "Odaberi kategoriju"} />
                </SelectTrigger>
                <SelectContent>
                  {wooCategories?.map(c => (
                    <SelectItem key={c.id} value={c.id.toString()}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="flex justify-center py-1">
              <div className="rounded-full bg-muted p-2">
                <ArrowRightIcon className="size-4 rotate-90 text-muted-foreground sm:rotate-0" />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="olx-cat">OLX Kategorija</Label>
              <Select
                items={(olxCategories ?? []).map((c) => ({ value: c.id.toString(), label: c.name }))}
                value={olxCatId}
                onValueChange={(v) => setOlxCatId(v ?? "")}
              >
                <SelectTrigger id="olx-cat">
                  <SelectValue placeholder={olxLoading ? "Učitavanje..." : "Odaberi kategoriju"} />
                </SelectTrigger>
                <SelectContent>
                  {olxCategories?.map(c => (
                    <SelectItem key={c.id} value={c.id.toString()}>{c.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <p className="text-[10px] text-muted-foreground">
                Napomena: Preporučujemo mapiranje do najnižih podkategorija radi tačnijih atributa.
              </p>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAddOpen(false)}>
              Otkaži
            </Button>
            <Button 
              onClick={handleAdd} 
              disabled={createMapping.isPending || !wooCatId || !olxCatId}
            >
              {createMapping.isPending && <Loader2Icon className="mr-2 size-4 animate-spin" />}
              Sačuvaj mapiranje
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleteId}
        onOpenChange={(open) => !open && setDeleteId(null)}
        onConfirm={handleDelete}
        title="Obriši mapiranje?"
        description="Ova akcija će ukloniti pravilo za automatsko smještanje proizvoda iz ove WooCommerce kategorije."
        confirmLabel="Obriši"
        destructive
        loading={deleteMapping.isPending}
      />
    </div>
  )
}
