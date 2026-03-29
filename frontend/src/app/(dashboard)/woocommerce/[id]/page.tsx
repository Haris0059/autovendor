export default function WooStoreDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Store Details</h1>
      <p className="text-muted-foreground">
        Store products and sync status — coming soon
      </p>
    </div>
  );
}
