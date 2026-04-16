"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeftIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ListingForm } from "@/components/listings/listing-form";

export default function NewListingPage() {
  const router = useRouter();

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          render={<Link href="/listings" />}
        >
          <ArrowLeftIcon />
        </Button>
        <div>
          <h1 className="text-2xl font-bold">Novi artikal</h1>
          <p className="text-sm text-muted-foreground">
            Popunite podatke za novi OLX oglas.
          </p>
        </div>
      </div>

      <ListingForm
        onSaved={(id) => router.push(`/listings/${id}`)}
      />
    </div>
  );
}
