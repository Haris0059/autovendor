"use client";

import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Loader2Icon, SaveIcon, UploadCloudIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Field, FieldError, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ImageUploader, type UploaderImage } from "@/components/shared/image-uploader";
import {
  useOlxCategories,
  useCategoryAttributes,
  useCategoryBrands,
  useBrandModels,
} from "@/hooks/use-categories";
import {
  useCountries,
  useStates,
  useCantons,
  useCities,
} from "@/hooks/use-locations";
import {
  useCreateListing,
  useUpdateListing,
  useUploadListingImages,
} from "@/hooks/use-listings";
import { useActiveAccount } from "@/hooks/use-active-account";
import { toastMessages } from "@/lib/toast-messages";
import type { OlxListing } from "@/types/olx";

const schema = z.object({
  title: z.string().min(3, "Naslov mora imati najmanje 3 znaka."),
  short_description: z.string().optional(),
  description: z.string().optional(),
  listing_type: z.enum(["sell", "buy", "rent"]),
  state: z.enum(["new", "used"]),
  price: z.number().min(0, "Cijena ne može biti negativna.").optional(),
  sku_number: z.string().optional(),
  available: z.boolean(),
  top_category_id: z.number().optional(),
  category_id: z.number().optional(),
  brand_id: z.number().optional(),
  model_id: z.number().optional(),
  country_id: z.number().optional(),
  state_id: z.number().optional(),
  canton_id: z.number().optional(),
  city_id: z.number().optional(),
});

type FormValues = z.infer<typeof schema>;

interface ListingFormProps {
  listing?: OlxListing;
  onSaved?: (id: number) => void;
}

export function ListingForm({ listing, onSaved }: ListingFormProps) {
  const { account } = useActiveAccount();
  const create = useCreateListing();
  const update = useUpdateListing();
  const uploadImages = useUploadListingImages();

  const isEdit = !!listing;

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: listing?.title ?? "",
      short_description: "",
      description: listing?.description ?? "",
      listing_type: (listing?.listing_type as "sell" | "buy" | "rent") ?? "sell",
      state: (listing?.state as "new" | "used") ?? "used",
      price: listing?.price ?? undefined,
      sku_number: "",
      available: true,
      top_category_id: undefined,
      category_id: listing?.category_id ?? undefined,
      brand_id: undefined,
      model_id: undefined,
      country_id: 49, // OLX id for Bosna i Hercegovina

      state_id: undefined,
      canton_id: undefined,
      city_id: listing?.city_id ?? undefined,
    },
  });

  const topCategoryId = watch("top_category_id");
  const categoryId = watch("category_id");
  const brandId = watch("brand_id");
  const stateId = watch("state_id");
  const cantonId = watch("canton_id");
  const listingType = watch("listing_type");
  const listingState = watch("state");
  const available = watch("available");

  const topCategories = useOlxCategories();
  const subCategories = useOlxCategories(topCategoryId);
  const attributes = useCategoryAttributes(categoryId ?? 0);
  const brands = useCategoryBrands(categoryId ?? 0);
  const models = useBrandModels(categoryId ?? 0, brandId ?? 0);

  const countries = useCountries();
  const states = useStates(1);
  const cantons = useCantons(stateId);
  const cities = useCities({ cantonId: cantonId, stateId: stateId });

  const [images, setImages] = useState<UploaderImage[]>(
    listing
      ? listing.images.map((img) => ({
          id: `remote-${img.id}`,
          url: img.url,
          is_main: img.is_main,
        }))
      : []
  );

  const [tab, setTab] = useState<"basic" | "category" | "location" | "images">(
    "basic"
  );

  useEffect(() => {
    setValue("category_id", undefined);
    setValue("brand_id", undefined);
    setValue("model_id", undefined);
  }, [topCategoryId, setValue]);

  useEffect(() => {
    setValue("brand_id", undefined);
    setValue("model_id", undefined);
  }, [categoryId, setValue]);

  useEffect(() => {
    setValue("model_id", undefined);
  }, [brandId, setValue]);

  useEffect(() => {
    setValue("canton_id", undefined);
    setValue("city_id", undefined);
  }, [stateId, setValue]);

  useEffect(() => {
    setValue("city_id", undefined);
  }, [cantonId, setValue]);

  const isPending =
    create.isPending || update.isPending || uploadImages.isPending || isSubmitting;

  const uploadNewImages = async (listingId: number) => {
    const files = images
      .filter((img) => img.file)
      .map((img) => img.file as File);
    if (files.length > 0) {
      await uploadImages.mutateAsync({ listingId, files });
    }
  };

  const onSubmit = handleSubmit(async (data) => {
    if (!account && !isEdit) {
      toast.error("Odaberite OLX profil prije kreiranja artikla.");
      return;
    }
    try {
      if (isEdit && listing) {
        await update.mutateAsync({ id: listing.id, ...data });
        await uploadNewImages(listing.id);
        toast.success(toastMessages.updated);
        onSaved?.(listing.id);
      } else {
        if (!account) {
          toast.error("Odaberite OLX profil prije kreiranja artikla.");
          return;
        }
        const created = await create.mutateAsync({
          account_id: account.id,
          ...data,
        });
        await uploadNewImages(created.id);
        toast.success(toastMessages.created);
        onSaved?.(created.id);
      }
    } catch (err) {
      toast.error(
        (err as Error).message || toastMessages.errorSave
      );
    }
  });

  const attrFields = useMemo(
    () => attributes.data ?? [],
    [attributes.data]
  );

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <Tabs value={tab} onValueChange={(v) => setTab(v as typeof tab)}>
        <TabsList>
          <TabsTrigger value="basic">Osnovno</TabsTrigger>
          <TabsTrigger value="category">Kategorija</TabsTrigger>
          <TabsTrigger value="location">Lokacija</TabsTrigger>
          <TabsTrigger value="images">Slike</TabsTrigger>
        </TabsList>

        <TabsContent value="basic" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Osnovni podaci</CardTitle>
              <CardDescription>
                Naslov, opis i tip artikla.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <Field data-invalid={!!errors.title || undefined}>
                <FieldLabel htmlFor="title">Naslov</FieldLabel>
                <Input
                  id="title"
                  placeholder="npr. Audi A4 2.0 TDI"
                  {...register("title")}
                />
                <FieldError
                  errors={errors.title ? [errors.title] : undefined}
                />
              </Field>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="listing_type">Tip oglasa</FieldLabel>
                  <Select
                    items={[
                      { value: "sell", label: "Prodaja" },
                      { value: "buy", label: "Kupovina" },
                      { value: "rent", label: "Iznajmljivanje" },
                    ]}
                    value={listingType}
                    onValueChange={(v) =>
                      setValue("listing_type", v as "sell" | "buy" | "rent")
                    }
                  >
                    <SelectTrigger id="listing_type">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="sell">Prodaja</SelectItem>
                      <SelectItem value="buy">Kupovina</SelectItem>
                      <SelectItem value="rent">Iznajmljivanje</SelectItem>
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor="state">Stanje</FieldLabel>
                  <Select
                    items={[
                      { value: "new", label: "Novo" },
                      { value: "used", label: "Korišteno" },
                    ]}
                    value={listingState}
                    onValueChange={(v) =>
                      setValue("state", v as "new" | "used")
                    }
                  >
                    <SelectTrigger id="state">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="new">Novo</SelectItem>
                      <SelectItem value="used">Korišteno</SelectItem>
                    </SelectContent>
                  </Select>
                </Field>
              </div>

              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field data-invalid={!!errors.price || undefined}>
                  <FieldLabel htmlFor="price">Cijena (KM)</FieldLabel>
                  <Input
                    id="price"
                    type="number"
                    min={0}
                    step="0.01"
                    placeholder="0.00"
                    {...register("price", { valueAsNumber: true })}
                  />
                  <FieldError
                    errors={errors.price ? [errors.price] : undefined}
                  />
                </Field>
                <Field>
                  <FieldLabel htmlFor="sku_number">SKU (opcionalno)</FieldLabel>
                  <Input
                    id="sku_number"
                    placeholder="npr. ART-001"
                    {...register("sku_number")}
                  />
                </Field>
              </div>

              <Field>
                <FieldLabel htmlFor="short_description">Kratki opis</FieldLabel>
                <Input
                  id="short_description"
                  placeholder="Sažetak artikla"
                  {...register("short_description")}
                />
              </Field>

              <Field>
                <FieldLabel htmlFor="description">Opis</FieldLabel>
                <Textarea
                  id="description"
                  rows={6}
                  placeholder="Detaljan opis artikla..."
                  {...register("description")}
                />
              </Field>

              <div className="flex items-center justify-between rounded-md border px-3 py-2">
                <div>
                  <p className="text-sm font-medium">Dostupno</p>
                  <p className="text-xs text-muted-foreground">
                    Ako je isključeno, artikal neće biti vidljiv kupcima.
                  </p>
                </div>
                <Switch
                  checked={available}
                  onCheckedChange={(v) => setValue("available", v)}
                />
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="category" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Kategorija</CardTitle>
              <CardDescription>
                Odaberite kategoriju i dodatne atribute.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="top_category_id">Kategorija</FieldLabel>
                  <Select
                    items={(topCategories.data ?? []).map((c) => ({
                      value: String(c.id),
                      label: c.name,
                    }))}
                    value={topCategoryId ? String(topCategoryId) : ""}
                    onValueChange={(v) =>
                      setValue("top_category_id", Number(v))
                    }
                  >
                    <SelectTrigger id="top_category_id">
                      <SelectValue placeholder="Odaberite" />
                    </SelectTrigger>
                    <SelectContent>
                      {(topCategories.data ?? []).map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>

                <Field>
                  <FieldLabel htmlFor="category_id">Podkategorija</FieldLabel>
                  <Select
                    items={(subCategories.data ?? []).map((c) => ({
                      value: String(c.id),
                      label: c.name,
                    }))}
                    value={categoryId ? String(categoryId) : ""}
                    onValueChange={(v) => setValue("category_id", Number(v))}
                    disabled={!topCategoryId}
                  >
                    <SelectTrigger id="category_id">
                      <SelectValue placeholder="Odaberite" />
                    </SelectTrigger>
                    <SelectContent>
                      {(subCategories.data ?? []).map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              </div>

              {(brands.data?.length ?? 0) > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <Field>
                    <FieldLabel htmlFor="brand_id">Marka</FieldLabel>
                    <Select
                      items={(brands.data ?? []).map((b) => ({
                        value: String(b.id),
                        label: b.name,
                      }))}
                      value={brandId ? String(brandId) : ""}
                      onValueChange={(v) => setValue("brand_id", Number(v))}
                    >
                      <SelectTrigger id="brand_id">
                        <SelectValue placeholder="Odaberite" />
                      </SelectTrigger>
                      <SelectContent>
                        {(brands.data ?? []).map((b) => (
                          <SelectItem key={b.id} value={String(b.id)}>
                            {b.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="model_id">Model</FieldLabel>
                    <Select
                      items={(models.data ?? []).map((m) => ({
                        value: String(m.id),
                        label: m.name,
                      }))}
                      value={watch("model_id") ? String(watch("model_id")) : ""}
                      onValueChange={(v) => setValue("model_id", Number(v))}
                      disabled={!brandId}
                    >
                      <SelectTrigger id="model_id">
                        <SelectValue placeholder="Odaberite" />
                      </SelectTrigger>
                      <SelectContent>
                        {(models.data ?? []).map((m) => (
                          <SelectItem key={m.id} value={String(m.id)}>
                            {m.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </Field>
                </div>
              ) : null}

              {attrFields.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {attrFields.map((attr) => (
                    <Field key={attr.name}>
                      <FieldLabel htmlFor={`attr-${attr.name}`}>
                        {attr.display_name}
                        {attr.required ? " *" : ""}
                      </FieldLabel>
                      {attr.input_type === "select" && attr.options ? (
                        <Select>
                          <SelectTrigger id={`attr-${attr.name}`}>
                            <SelectValue placeholder="Odaberite" />
                          </SelectTrigger>
                          <SelectContent>
                            {attr.options.map((opt) => (
                              <SelectItem key={opt} value={opt}>
                                {opt}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <Input
                          id={`attr-${attr.name}`}
                          type={
                            attr.input_type === "number" ? "number" : "text"
                          }
                          placeholder={attr.display_name}
                        />
                      )}
                    </Field>
                  ))}
                </div>
              ) : null}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="location" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Lokacija</CardTitle>
              <CardDescription>
                Država, entitet, kanton i grad.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <Field>
                  <FieldLabel htmlFor="country_id">Država</FieldLabel>
                  <Select
                    items={(countries.data ?? []).map((c) => ({
                      value: String(c.id),
                      label: c.name,
                    }))}
                    value="49"
                    onValueChange={() => {}}
                  >
                    <SelectTrigger id="country_id">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {(countries.data ?? []).map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor="state_id">Entitet</FieldLabel>
                  <Select
                    items={(states.data ?? []).map((s) => ({
                      value: String(s.id),
                      label: s.name,
                    }))}
                    value={stateId ? String(stateId) : ""}
                    onValueChange={(v) => setValue("state_id", Number(v))}
                  >
                    <SelectTrigger id="state_id">
                      <SelectValue placeholder="Odaberite" />
                    </SelectTrigger>
                    <SelectContent>
                      {(states.data ?? []).map((s) => (
                        <SelectItem key={s.id} value={String(s.id)}>
                          {s.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor="canton_id">Kanton</FieldLabel>
                  <Select
                    items={(cantons.data ?? []).map((c) => ({
                      value: String(c.id),
                      label: c.name,
                    }))}
                    value={cantonId ? String(cantonId) : ""}
                    onValueChange={(v) => setValue("canton_id", Number(v))}
                    disabled={!stateId || (cantons.data?.length ?? 0) === 0}
                  >
                    <SelectTrigger id="canton_id">
                      <SelectValue placeholder="Odaberite" />
                    </SelectTrigger>
                    <SelectContent>
                      {(cantons.data ?? []).map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor="city_id">Grad</FieldLabel>
                  <Select
                    items={(cities.data ?? []).map((c) => ({
                      value: String(c.id),
                      label: c.name,
                    }))}
                    value={watch("city_id") ? String(watch("city_id")) : ""}
                    onValueChange={(v) => setValue("city_id", Number(v))}
                    disabled={!stateId}
                  >
                    <SelectTrigger id="city_id">
                      <SelectValue placeholder="Odaberite" />
                    </SelectTrigger>
                    <SelectContent>
                      {(cities.data ?? []).map((c) => (
                        <SelectItem key={c.id} value={String(c.id)}>
                          {c.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </Field>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="images" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Slike</CardTitle>
              <CardDescription>
                Dodajte do 20 slika. Prva slika se koristi kao glavna.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ImageUploader images={images} onChange={setImages} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="flex items-center justify-end gap-2">
        <Button
          type="submit"
          variant="outline"
          disabled={isPending}
        >
          {isPending ? (
            <Loader2Icon className="animate-spin" />
          ) : (
            <SaveIcon />
          )}
          {isEdit ? "Sačuvaj izmjene" : "Sačuvaj kao draft"}
        </Button>
        {!isEdit ? (
          <Button type="submit" disabled={isPending}>
            {isPending ? (
              <Loader2Icon className="animate-spin" />
            ) : (
              <UploadCloudIcon />
            )}
            Objavi
          </Button>
        ) : null}
      </div>
    </form>
  );
}
