import { useEffect } from "react";
import { create } from "zustand";
import { persist } from "zustand/middleware";
import { useOlxAccounts } from "@/hooks/use-olx-accounts";
import type { OlxAccount } from "@/types/olx";

interface ActiveAccountState {
  account: OlxAccount | null;
  setAccount: (account: OlxAccount | null) => void;
}

export const useActiveAccount = create<ActiveAccountState>()(
  persist(
    (set) => ({
      account: null,
      setAccount: (account) => set({ account }),
    }),
    { name: "active-account" }
  )
);

/**
 * The persisted active account can go stale: it may belong to a previous
 * session/user (or the mock layer) and not exist in the current user's account
 * list, or its snapshot (e.g. default_city_id) may be outdated. Reconciles the
 * store against the fetched accounts: unknown → first account, known → fresh
 * snapshot. Mount once per page that relies on the active account.
 */
export function useActiveAccountSync() {
  const { account, setAccount } = useActiveAccount();
  const accountsQuery = useOlxAccounts();
  const accounts = accountsQuery.data;

  useEffect(() => {
    if (!accounts) return;
    if (accounts.length === 0) {
      if (account) setAccount(null);
      return;
    }
    const match = account ? accounts.find((a) => a.id === account.id) : undefined;
    if (!match) {
      setAccount(accounts[0]);
    } else if (JSON.stringify(match) !== JSON.stringify(account)) {
      setAccount(match);
    }
  }, [account, accounts, setAccount]);
}
