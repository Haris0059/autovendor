import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold">OLX Dashboard</h1>
        <p className="mt-4 text-lg text-muted-foreground">
          Manage OLX.ba listings and sync with WooCommerce from a single
          dashboard.
        </p>
        <div className="mt-8 flex gap-4 justify-center">
          <Button render={<Link href="/login" />}>Login</Button>
          <Button variant="outline" render={<Link href="/register" />}>
            Register
          </Button>
        </div>
      </div>
    </div>
  );
}
