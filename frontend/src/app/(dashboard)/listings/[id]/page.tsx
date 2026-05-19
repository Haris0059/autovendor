"use client";

import { use, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import {
  ArrowLeftIcon,
  ImagesIcon,
  Trash2Icon,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { ListingForm } from "@/components/listings/listing-form";
import { OlxStatusBadge } from "@/components/shared/status-badge";
import { useDeleteListing, useListing } from "@/hooks/use-listings";
import { toastMessages } from "@/lib/toast-messages";
import { PageHeader } from "@/components/shared/page-header";

export default function EditListingPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const listingId = Number(id);
  const router = useRouter();

  const listing = useListing(listingId);
  const remove = useDeleteListing();
  const [deleteOpen, setDeleteOpen] = useState(false);

  const handleDelete = () => {
    remove.mutate(listingId, {
      onSuccess: () => {
        toast.success(toastMessages.deleted);
        router.push("/listings");
      },
      onError: (err) => {
        toast.error(err.message || toastMessages.errorDelete);
      },
    });
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
      <PageHeader
        title={listing.data.title}
        description={`ID: ${listing.data.id}`}
      >
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            className="mr-2"
            render={<Link href="/listings" />}
          >
            <ArrowLeftIcon />
          </Button>
          <OlxStatusBadge status={listing.data.status} />
          <div className="ml-4 flex items-center gap-2">
            <Button
              variant="outline"
              render={<Link href={`/listings/${listingId}/images`} />}
            >
              <ImagesIcon />
              Slike
            </Button>
            <Button
              variant="destructive"
              onClick={() => setDeleteOpen(true)}
            >
              <Trash2Icon />
              Obriši
            </Button>
          </div>
        </div>
      </PageHeader>

      <Tabs defaultValue="details">
        <TabsList>
          <TabsTrigger value="details">Detalji</TabsTrigger>
          <TabsTrigger value="history">Historija</TabsTrigger>
          <TabsTrigger value="sponsored" disabled>
            Sponzorisanje
          </TabsTrigger>
        </TabsList>

        <TabsContent value="details" className="mt-4 space-y-4">
          <ListingForm listing={listing.data} />
        </TabsContent>

        <TabsContent value="history" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle>Historija promjena</CardTitle>
              <CardDescription>
                Pregled svih promjena na ovom artiklu.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="py-8 text-center text-sm text-muted-foreground">
                Uskoro…
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <ConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title="Obriši artikal"
        description={`Jeste li sigurni da želite obrisati artikal "${listing.data.title}"? Ova akcija je nepovratna.`}
        confirmLabel="Obriši"
        destructive
        onConfirm={handleDelete}
      />
    </div>
  );
}
