"use client"

import { useState } from "react"
import {
  useCategoryMappings,
  useCreateCategoryMapping,
  useUpdateCategoryMapping,
  useDeleteCategoryMapping,
} from "@/hooks/use-sync"
import { useWooStores, useWooStoreCategories } from "@/hooks/use-woo-stores"
import {
  useOlxCategories,
  useCategoryAttributes,
  useCategorySuggestions,
} from "@/hooks/use-categories"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { cn } from "@/lib/utils"
import type { CategoryMapping } from "@/types/sync"
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
  PencilIcon,
  ArrowRightIcon,
  Loader2Icon,
  SearchIcon,
  LayersIcon,
} from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { toast } from "sonner"
import { toastMessages } from "@/lib/toast-messages"
import { ConfirmDialog } from "@/components/shared/confirm-dialog"
import { Input } from "@/components/ui/input"
import { PageHeader } from "@/components/shared/page-header"

export default function CategoryMappingsPage() {
  const { data: mappings, isLoading: mappingsLoading } = useCategoryMappings()
  const { data: stores } = useWooStores()
  const createMapping = useCreateCategoryMapping()
  const updateMapping = useUpdateCategoryMapping()
  const deleteMapping = useDeleteCategoryMapping()

  const [selectedStoreId, setSelectedStoreId] = useState<number | null>(null)
  const { data: wooCategories, isLoading: wooLoading } = useWooStoreCategories(selectedStoreId || 0)

  const [isAddOpen, setIsAddOpen] = useState(false)
  const [editingMapping, setEditingMapping] = useState<CategoryMapping | null>(null)
  const [deleteId, setDeleteId] = useState<number | null>(null)
  const [search, setSearch] = useState("")

  // Form state
  const [wooCatId, setWooCatId] = useState("")
  // OLX category drill-down: one selection per level, deepest selection wins.
  // OLX nests categories 3-4 deep; attribute-bearing categories are leaves.
  const [catPath, setCatPath] = useState<({ id: number; name: string } | null)[]>([null, null, null, null])
  const [attrDefaults, setAttrDefaults] = useState<Record<string, string>>({})
  // OLX's own keyword→category suggester; a clicked suggestion selects the OLX
  // category directly, the cascade below stays as the manual fallback.
  const [suggestedOlxCat, setSuggestedOlxCat] = useState<{ id: number; name: string; path?: string } | null>(null)
  const [keyword, setKeyword] = useState("")
  const debouncedKeyword = useDebouncedValue(keyword)
  const suggestions = useCategorySuggestions(isAddOpen ? debouncedKeyword : "")

  const level1 = useOlxCategories()
  const level2 = useOlxCategories(catPath[0]?.id)
  const level3 = useOlxCategories(catPath[1]?.id)
  const level4 = useOlxCategories(catPath[2]?.id)
  const levels = [level1, level2, level3, level4]

  const deepestSelected = [...catPath].reverse().find(Boolean) ?? null
  const effectiveOlxCat = deepestSelected
    ?? suggestedOlxCat
    ?? (editingMapping
      ? { id: editingMapping.olx_category_id, name: editingMapping.olx_category_name }
      : null)

  const { data: olxAttributes, isLoading: attrsLoading } = useCategoryAttributes(
    effectiveOlxCat?.id ?? 0
  )
  const requiredMissing = (olxAttributes ?? []).some(
    (a) => a.required && !attrDefaults[a.name]?.trim()
  )

  const selectCategoryAt = (level: number, value: string | null) => {
    const options = levels[level].data ?? []
    const chosen = options.find((c) => c.id.toString() === value) ?? null
    setCatPath((prev) => {
      const next = [...prev]
      next[level] = chosen
      for (let i = level + 1; i < next.length; i++) next[i] = null
      return next
    })
    setSuggestedOlxCat(null)
    setAttrDefaults({})
  }

  const pickSuggestion = (s: { id: number; name: string; path?: string }) => {
    setSuggestedOlxCat({ id: s.id, name: s.name, path: s.path })
    setCatPath([null, null, null, null])
    setAttrDefaults({})
  }

  const openAdd = () => {
    setEditingMapping(null)
    setWooCatId("")
    setCatPath([null, null, null, null])
    setAttrDefaults({})
    setSuggestedOlxCat(null)
    setKeyword("")
    setIsAddOpen(true)
  }

  const openEdit = (mapping: CategoryMapping) => {
    setEditingMapping(mapping)
    setWooCatId(mapping.woo_category_id.toString())
    setCatPath([null, null, null, null])
    setAttrDefaults(mapping.attribute_defaults ?? {})
    setSuggestedOlxCat(null)
    setKeyword(mapping.woo_category_name)
    setIsAddOpen(true)
  }

  const filteredMappings = mappings?.filter(m => 
    m.woo_category_name.toLowerCase().includes(search.toLowerCase()) ||
    m.olx_category_name.toLowerCase().includes(search.toLowerCase())
  )

  const handleSave = () => {
    const olxCat = effectiveOlxCat
    if (!olxCat) {
      toast.error("Morate odabrati OLX kategoriju.")
      return
    }

    const cleaned = Object.fromEntries(
      Object.entries(attrDefaults).filter(([, v]) => v?.trim())
    )
    const attributeDefaults = Object.keys(cleaned).length > 0 ? cleaned : null

    const onSuccess = () => {
      toast.success(toastMessages.sync.mappingCreateSuccess)
      setIsAddOpen(false)
    }
    const onError = (error: unknown) => {
      const message = error instanceof Error ? error.message : toastMessages.sync.mappingCreateError
      toast.error(message)
    }

    if (editingMapping) {
      updateMapping.mutate({
        id: editingMapping.id,
        data: {
          olx_category_id: olxCat.id,
          olx_category_name: olxCat.name,
          attribute_defaults: attributeDefaults,
        },
      }, { onSuccess, onError })
      return
    }

    const wooCat = wooCategories?.find(c => c.id.toString() === wooCatId)
    if (!wooCat) {
      toast.error("Morate odabrati obje kategorije.")
      return
    }
    createMapping.mutate({
      woo_category_id: wooCat.id,
      woo_category_name: wooCat.name,
      olx_category_id: olxCat.id,
      olx_category_name: olxCat.name,
      attribute_defaults: attributeDefaults,
    }, { onSuccess, onError })
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
        <Button onClick={openAdd}>
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
                        {mapping.attribute_defaults && Object.keys(mapping.attribute_defaults).length > 0 && (
                          <Badge variant="outline" className="text-[10px]">
                            {Object.keys(mapping.attribute_defaults).length} atr.
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="size-8"
                          onClick={() => openEdit(mapping)}
                        >
                          <PencilIcon className="size-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="size-8 text-destructive hover:bg-destructive/10 hover:text-destructive"
                          onClick={() => setDeleteId(mapping.id)}
                        >
                          <Trash2Icon className="size-4" />
                        </Button>
                      </div>
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
            <DialogTitle>
              {editingMapping ? "Uredi mapiranje kategorija" : "Novo mapiranje kategorija"}
            </DialogTitle>
            <DialogDescription>
              Povežite kategoriju iz vašeg WooCommerce shopa sa odgovarajućom OLX kategorijom.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="woo-cat">WooCommerce Kategorija</Label>
              {editingMapping ? (
                <Input value={editingMapping.woo_category_name} disabled />
              ) : (
              <Select
                items={(wooCategories ?? []).map((c) => ({ value: c.id.toString(), label: c.name }))}
                value={wooCatId}
                onValueChange={(v) => {
                  setWooCatId(v ?? "")
                  const wooCat = wooCategories?.find((c) => c.id.toString() === v)
                  if (wooCat) setKeyword(wooCat.name)
                }}
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
              )}
            </div>

            <div className="flex justify-center py-1">
              <div className="rounded-full bg-muted p-2">
                <ArrowRightIcon className="size-4 rotate-90 text-muted-foreground sm:rotate-0" />
              </div>
            </div>

            <div className="grid gap-2">
              <Label>OLX Kategorija</Label>
              {editingMapping && !deepestSelected && !suggestedOlxCat && (
                <p className="text-xs text-muted-foreground">
                  Trenutno: <span className="font-medium text-foreground">{editingMapping.olx_category_name}</span> — odaberite ispod da promijenite.
                </p>
              )}

              <div className="grid gap-1.5 rounded-md border bg-muted/30 p-2.5">
                <Label htmlFor="olx-keyword" className="text-xs font-normal text-muted-foreground">
                  Prijedlozi na osnovu ključne riječi (OLX pretraga)
                </Label>
                <Input
                  id="olx-keyword"
                  placeholder="npr. dijamantski kroneri"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                />
                {suggestions.isFetching && (
                  <div className="flex items-center gap-2 py-1 text-xs text-muted-foreground">
                    <Loader2Icon className="size-3 animate-spin" />
                    Tražim prijedloge...
                  </div>
                )}
                {!suggestions.isFetching && (suggestions.data ?? []).length > 0 && (
                  <div className="grid max-h-44 gap-1 overflow-y-auto pr-1">
                    {suggestions.data!.map((s) => (
                      <button
                        type="button"
                        key={s.id}
                        onClick={() => pickSuggestion(s)}
                        className={cn(
                          "flex items-center justify-between gap-2 rounded-md border bg-background px-2.5 py-1.5 text-left text-sm transition-colors hover:bg-muted",
                          suggestedOlxCat?.id === s.id && !deepestSelected &&
                            "border-primary ring-1 ring-primary"
                        )}
                      >
                        <span className="min-w-0">
                          <span className="block truncate font-medium">{s.name}</span>
                          {s.path && (
                            <span className="block truncate text-[10px] text-muted-foreground">
                              {s.path}
                            </span>
                          )}
                        </span>
                        <Badge variant="secondary" className="shrink-0 text-[10px]">
                          {s.count}
                        </Badge>
                      </button>
                    ))}
                  </div>
                )}
                {!suggestions.isFetching &&
                  debouncedKeyword.trim().length >= 2 &&
                  (suggestions.data ?? []).length === 0 &&
                  suggestions.isFetched && (
                    <p className="py-1 text-xs text-muted-foreground">
                      Nema prijedloga — odaberite kategoriju ručno ispod.
                    </p>
                  )}
              </div>

              {levels.map((level, i) => {
                const options = level.data ?? []
                if (i > 0 && (!catPath[i - 1] || options.length === 0)) return null
                return (
                  <Select
                    key={i}
                    items={options.map((c) => ({ value: c.id.toString(), label: c.name }))}
                    value={catPath[i]?.id.toString() ?? ""}
                    onValueChange={(v) => selectCategoryAt(i, v)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={
                        level.isLoading ? "Učitavanje..." :
                        i === 0 ? "Odaberi kategoriju ručno" : "Odaberi podkategoriju (opciono)"
                      } />
                    </SelectTrigger>
                    <SelectContent>
                      {options.map(c => (
                        <SelectItem key={c.id} value={c.id.toString()}>{c.name}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )
              })}
              <p className="text-[10px] text-muted-foreground">
                Napomena: Preporučujemo mapiranje do najnižih podkategorija radi tačnijih atributa.
              </p>
              {effectiveOlxCat && (
                <div className="flex items-center justify-between gap-2 rounded-md border border-primary/50 bg-primary/5 px-3 py-2">
                  <div className="min-w-0 text-sm">
                    <span className="text-[10px] text-muted-foreground">Odabrana OLX kategorija</span>
                    <span className="block truncate font-medium">{effectiveOlxCat.name}</span>
                  </div>
                  {suggestedOlxCat && !deepestSelected && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="shrink-0"
                      onClick={() => setSuggestedOlxCat(null)}
                    >
                      Poništi
                    </Button>
                  )}
                </div>
              )}
            </div>

            {effectiveOlxCat && attrsLoading && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Loader2Icon className="size-3 animate-spin" />
                Učitavanje atributa kategorije...
              </div>
            )}
            {effectiveOlxCat && !attrsLoading && (olxAttributes ?? []).length > 0 && (
              <div className="grid gap-3 rounded-lg border bg-muted/30 p-3">
                <p className="text-xs font-medium text-muted-foreground">
                  Zadane vrijednosti OLX atributa (proizvod ih može prepisati vlastitim atributima)
                </p>
                {olxAttributes!.map((attr) => (
                  <div key={attr.name} className="grid gap-1.5">
                    <Label className="text-xs">
                      {attr.display_name}
                      {attr.required && <span className="text-destructive"> *</span>}
                    </Label>
                    {attr.options && attr.options.length > 0 ? (
                      <Select
                        items={[
                          { value: "__none", label: "—" },
                          ...attr.options.map((o) => ({ value: o, label: o })),
                        ]}
                        value={attrDefaults[attr.name] ?? "__none"}
                        onValueChange={(v) =>
                          setAttrDefaults((prev) => {
                            const next = { ...prev }
                            if (!v || v === "__none") delete next[attr.name]
                            else next[attr.name] = v
                            return next
                          })
                        }
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="—" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="__none">—</SelectItem>
                          {attr.options.map((o) => (
                            <SelectItem key={o} value={o}>{o}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    ) : (
                      <Input
                        value={attrDefaults[attr.name] ?? ""}
                        onChange={(e) =>
                          setAttrDefaults((prev) => {
                            const next = { ...prev }
                            if (!e.target.value) delete next[attr.name]
                            else next[attr.name] = e.target.value
                            return next
                          })
                        }
                        placeholder="Zadana vrijednost"
                      />
                    )}
                  </div>
                ))}
                {requiredMissing && (
                  <p className="text-[10px] text-destructive">
                    Obavezni atributi (*) moraju imati zadanu vrijednost.
                  </p>
                )}
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAddOpen(false)}>
              Otkaži
            </Button>
            <Button
              onClick={handleSave}
              disabled={
                createMapping.isPending ||
                updateMapping.isPending ||
                (!editingMapping && !wooCatId) ||
                !effectiveOlxCat ||
                attrsLoading ||
                requiredMissing
              }
            >
              {(createMapping.isPending || updateMapping.isPending) && (
                <Loader2Icon className="mr-2 size-4 animate-spin" />
              )}
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
