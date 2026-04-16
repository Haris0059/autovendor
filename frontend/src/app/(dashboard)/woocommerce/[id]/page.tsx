"use client"

import { use, useState } from "react"
import Link from "next/link"
import {
  useWooStore,
  useWooStoreProducts,
  useWooStoreCategories,
  useWooStoreAttributes,
  useTestWooConnection,
} from "@/hooks/use-woo-stores"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  ChevronLeftIcon,
  ExternalLinkIcon,
  RefreshCwIcon,
  Loader2Icon,
  PackageIcon,
  LayersIcon,
  TagsIcon,
  HistoryIcon,
  LinkIcon,
  GlobeIcon,
} from "lucide-react"
import { toast } from "sonner"
import { formatDate } from "@/lib/utils"
import { StatusBadge } from "@/components/shared/status-badge"

export default function StoreDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: storeIdStr } = use(params)
  const storeId = parseInt(storeIdStr)

  const { data: store, isLoading: storeLoading } = useWooStore(storeId)
  const { data: products, isLoading: productsLoading } = useWooStoreProducts(storeId)
  const { data: categories, isLoading: categoriesLoading } = useWooStoreCategories(storeId)
  const { data: attributes, isLoading: attributesLoading } = useWooStoreAttributes(storeId)
  const testConnection = useTestWooConnection()

  const [activeTab, setActiveTab] = useState("products")

  const handleTest = () => {
    if (!store) return
    testConnection.mutate(
      { store_url: store.store_url, api_key: "********" }, // actual key is handled by backend, placeholder for mock
      {
        onSuccess: (data) => {
          if (data.ok) {
            toast.success(`Konekcija uspješna! Pronađeno ${data.products_count} proizvoda.`)
          } else {
            toast.error("Konekcija nije uspjela. Provjerite postavke na WordPressu.")
          }
        },
        onError: (error: unknown) => {
          const message = error instanceof Error ? error.message : "Greška pri testiranju konekcije."
          toast.error(message)
        },
      }
    )
  }

  if (storeLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2Icon className="size-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (!store) {
    return (
      <div className="flex flex-col items-center justify-center gap-4 py-20">
        <p className="text-muted-foreground">Prodavnica nije pronađena.</p>
        <Button variant="outline" render={<Link href="/woocommerce" />}>
          Nazad na listu
        </Button>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex items-center gap-4">
        <Button variant="outline" size="icon" className="size-8" render={<Link href="/woocommerce" />}>
          <ChevronLeftIcon className="size-4" />
        </Button>
        <div className="flex flex-col">
          <h1 className="text-xl font-bold tracking-tight">{store.name}</h1>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <GlobeIcon className="size-3" />
            <a
              href={store.store_url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1 hover:text-foreground hover:underline"
            >
              {store.store_url}
              <ExternalLinkIcon className="size-3" />
            </a>
          </div>
        </div>
        <div className="ml-auto flex items-center gap-2">
          <Button
            variant="outline"
            onClick={handleTest}
            disabled={testConnection.isPending}
          >
            {testConnection.isPending ? (
              <Loader2Icon className="mr-2 size-4 animate-spin" />
            ) : (
              <RefreshCwIcon className="mr-2 size-4" />
            )}
            Testiraj konekciju
          </Button>
          <Button>Uredi postavke</Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Ukupno proizvoda</CardTitle>
            <PackageIcon className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{products?.length || 0}</div>
            <p className="text-xs text-muted-foreground">Sinkronizirano iz kataloga</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Status konekcije</CardTitle>
            <GlobeIcon className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <StatusBadge status="active" />
              <span className="text-sm font-medium">Aktivan</span>
            </div>
            <p className="text-xs text-muted-foreground">Zadnja provjera: Maloprije</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Povezano sa OLX</CardTitle>
            <LinkIcon className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">0 / {products?.length || 0}</div>
            <p className="text-xs text-muted-foreground">Mapiranih artikala</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Zadnji Sync</CardTitle>
            <HistoryIcon className="size-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-sm font-medium">{formatDate(store.created_at)}</div>
            <p className="text-xs text-muted-foreground">Inicijalno povezivanje</p>
          </CardContent>
        </Card>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList>
          <TabsTrigger value="products">
            <PackageIcon className="mr-2 size-4" />
            Proizvodi
          </TabsTrigger>
          <TabsTrigger value="categories">
            <LayersIcon className="mr-2 size-4" />
            Kategorije
          </TabsTrigger>
          <TabsTrigger value="attributes">
            <TagsIcon className="mr-2 size-4" />
            Atributi
          </TabsTrigger>
          <TabsTrigger value="webhooks">
            <HistoryIcon className="mr-2 size-4" />
            Webhook Logovi
          </TabsTrigger>
        </TabsList>

        <TabsContent value="products" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>WooCommerce Katalog</CardTitle>
              <CardDescription>
                Lista proizvoda preuzetih sa vašeg WooCommerce sajta.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Slika</TableHead>
                    <TableHead>Naziv / SKU</TableHead>
                    <TableHead>Cijena</TableHead>
                    <TableHead>Zaliha</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="w-20 text-right">Akcije</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {productsLoading ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center">
                        <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ) : !products || products.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                        Nema proizvoda u katalogu.
                      </TableCell>
                    </TableRow>
                  ) : (
                    products.map((product) => (
                      <TableRow key={product.id}>
                        <TableCell>
                          {product.images?.[0]?.src ? (
                            <img
                              src={product.images[0].src}
                              alt={product.name}
                              className="size-10 rounded object-cover"
                            />
                          ) : (
                            <div className="flex size-10 items-center justify-center rounded bg-muted">
                              <PackageIcon className="size-4 text-muted-foreground" />
                            </div>
                          )}
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-col">
                            <span className="font-medium">{product.name}</span>
                            <span className="text-xs text-muted-foreground">{product.sku || "Nema SKU"}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          {product.price} {product.currency || "KM"}
                        </TableCell>
                        <TableCell>
                          {product.stock_status === "instock" ? (
                            <Badge variant="outline" className="text-green-600 border-green-200 bg-green-50">
                              {product.stock_quantity || "Na stanju"}
                            </Badge>
                          ) : (
                            <Badge variant="secondary">Nema na stanju</Badge>
                          )}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className="capitalize">
                            {product.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          <Button size="sm" variant="secondary">
                            <LinkIcon className="mr-2 size-3" />
                            Poveži
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="categories" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Kategorije Proizvoda</CardTitle>
              <CardDescription>
                Kategorije preuzete sa vašeg WooCommerce sajta.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Naziv Kategorije</TableHead>
                    <TableHead>Slug</TableHead>
                    <TableHead>Broj Proizvoda</TableHead>
                    <TableHead className="w-20 text-right">Mapirano</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {categoriesLoading ? (
                    <TableRow>
                      <TableCell colSpan={4} className="h-32 text-center">
                        <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ) : !categories || categories.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} className="h-32 text-center text-muted-foreground">
                        Nema kategorija.
                      </TableCell>
                    </TableRow>
                  ) : (
                    categories.map((category) => (
                      <TableRow key={category.id}>
                        <TableCell className="font-medium">{category.name}</TableCell>
                        <TableCell className="text-muted-foreground text-xs">{category.slug}</TableCell>
                        <TableCell>{category.count}</TableCell>
                        <TableCell className="text-right">
                          <Badge variant="secondary">Nije mapirano</Badge>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="attributes" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Atributi i Varijacije</CardTitle>
              <CardDescription>
                Globalni atributi dostupni u vašem WooCommerce katalogu.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Naziv Atributa</TableHead>
                    <TableHead>Vrijednosti</TableHead>
                    <TableHead className="w-20 text-right">Varijacija</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {attributesLoading ? (
                    <TableRow>
                      <TableCell colSpan={3} className="h-32 text-center">
                        <Loader2Icon className="mx-auto size-6 animate-spin text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ) : !attributes || attributes.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={3} className="h-32 text-center text-muted-foreground">
                        Nema atributa.
                      </TableCell>
                    </TableRow>
                  ) : (
                    attributes.map((attr) => (
                      <TableRow key={attr.id}>
                        <TableCell className="font-medium">{attr.name}</TableCell>
                        <TableCell className="max-w-md">
                          <div className="flex flex-wrap gap-1">
                            {attr.options?.map((opt: string) => (
                              <Badge key={opt} variant="outline" className="text-[10px]">
                                {opt}
                              </Badge>
                            )) || (
                              <span className="text-xs text-muted-foreground">
                                {attr.terms?.map((t: { name: string }) => t.name).join(", ")}
                              </span>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-right">
                          {attr.variation ? (
                            <Badge variant="default">Da</Badge>
                          ) : (
                            <Badge variant="secondary">Ne</Badge>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="webhooks" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Webhook Logovi</CardTitle>
              <CardDescription>
                Pregled zadnjih aktivnosti zaprimljenih putem webkooka.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex h-32 flex-col items-center justify-center text-center">
                <HistoryIcon className="mb-2 size-8 text-muted-foreground opacity-20" />
                <p className="text-sm text-muted-foreground">Još nema zaprimljenih webhook događaja.</p>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
