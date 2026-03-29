export default function EditListingPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Edit Listing</h1>
      <p className="text-muted-foreground">
        Edit listing details — coming soon
      </p>
    </div>
  );
}
