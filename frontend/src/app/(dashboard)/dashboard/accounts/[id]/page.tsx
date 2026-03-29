export default function AccountDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <div>
      <h1 className="mb-6 text-3xl font-bold">Account Details</h1>
      <p className="text-muted-foreground">
        Account overview, limits, and token status — coming soon
      </p>
    </div>
  );
}
