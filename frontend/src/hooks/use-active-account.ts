import { create } from "zustand";
import type { OlxAccount } from "@/types/olx";

interface ActiveAccountState {
  account: OlxAccount | null;
  setAccount: (account: OlxAccount | null) => void;
}

export const useActiveAccount = create<ActiveAccountState>((set) => ({
  account: null,
  setAccount: (account) => set({ account }),
}));
