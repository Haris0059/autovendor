export default function ImageManagerPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Image Manager</h1>
      <p className="text-muted-foreground">
        Upload, reorder, and manage listing images — coming soon
      </p>
    </div>
  );
}
