"use client";

import { use, useMemo, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { ArrowLeftIcon, SaveIcon, Loader2Icon } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  ImageUploader,
  type UploaderImage,
} from "@/components/shared/image-uploader";
import { useListing, useUpdateListing } from "@/hooks/use-listings";
import { toastMessages } from "@/lib/toast-messages";

export default function ListingImagesPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const listingId = Number(id);

  const listing = useListing(listingId);
  const update = useUpdateListing();

  const [draft, setDraft] = useState<UploaderImage[] | null>(null);

  const serverImages = useMemo<UploaderImage[]>(
    () =>
      (listing.data?.images ?? []).map((img) => ({
        id: `remote-${img.id}`,
        url: img.url,
        is_main: img.is_main,
      })),
    [listing.data]
  );

  const images = draft ?? serverImages;
  const setImages = (next: UploaderImage[]) => setDraft(next);

  const handleSave = () => {
    update.mutate(
      {
        id: listingId,
        images: images.map((img, idx) => ({
          url: img.url,
          is_main: img.is_main,
          order: idx,
        })),
      },
      {
        onSuccess: () => toast.success(toastMessages.saved),
        onError: (err) =>
          toast.error(err.message || toastMessages.errorSave),
      }
    );
  };

  if (listing.isLoading) {
    return (
      <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
        <p className="text-sm text-muted-foreground">Učitavanje…</p>
      </div>
    );
  }

  if (listing.isError || !listing.data) {
    return (
      <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
        <Button variant="ghost" render={<Link href="/listings" />}>
          <ArrowLeftIcon />
          Nazad
        </Button>
        <p className="text-sm text-destructive">Artikal nije pronađen.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            render={<Link href={`/listings/${listingId}`} />}
          >
            <ArrowLeftIcon />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">Slike artikla</h1>
            <p className="text-sm text-muted-foreground">
              {listing.data.title}
            </p>
          </div>
        </div>
        <Button onClick={handleSave} disabled={update.isPending}>
          {update.isPending ? (
            <Loader2Icon className="animate-spin" />
          ) : (
            <SaveIcon />
          )}
          Sačuvaj
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Galerija</CardTitle>
          <CardDescription>
            Dodajte, uklonite ili preuredite slike. Prva slika ili označena kao
            glavna prikazuje se u pretragama.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ImageUploader images={images} onChange={setImages} />
        </CardContent>
      </Card>
    </div>
  );
}
