import { create } from "zustand";
import { persist } from "zustand/middleware";
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
