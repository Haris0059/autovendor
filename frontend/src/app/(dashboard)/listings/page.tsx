"use client"

import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"

import { ListingsTable } from "@/components/listings/listings-table"
import { ListingsToolbar } from "@/components/listings/listings-toolbar"
import { useActiveAccount } from "@/hooks/use-active-account"
import { useListings } from "@/hooks/use-listings"
import { useOlxAccounts } from "@/hooks/use-olx-accounts"

const STATUS_TABS = [
  { value: "active", label: "Aktivni" },
  { value: "finished", label: "Završeni" },
  { value: "inactive", label: "Neaktivni" },
  { value: "expired", label: "Istekli" },
  { value: "hidden", label: "Sakriveni" },
] as const

type StatusValue = (typeof STATUS_TABS)[number]["value"]

export default function ListingsPage() {
  const { account: activeAccount, setAccount } = useActiveAccount()
  const accountsQuery = useOlxAccounts()
  const accounts = useMemo(
    () => accountsQuery.data ?? [],
    [accountsQuery.data]
  )

  const [status, setStatus] = useState<StatusValue>("active")
  const [page, setPage] = useState(1)
  const [search, setSearch] = useState("")
  const [perPage, setPerPage] = useState("10")

  // If no active account but accounts have loaded, default to the first one.
  useEffect(() => {
    if (!activeAccount && accounts.length > 0) {
      setAccount(accounts[0])
    }
  }, [activeAccount, accounts, setAccount])

  const listingsQuery = useListings({
    account_id: activeAccount?.id ?? 0,
    status,
    page,
  })

  const handleStatusChange = (value: StatusValue) => {
    setStatus(value)
    setPage(1)
  }

  const filteredListings = useMemo(() => {
    const data = listingsQuery.data?.data ?? []
    if (!search.trim()) return data
    const needle = search.trim().toLowerCase()
    return data.filter((l) => l.title.toLowerCase().includes(needle))
  }, [listingsQuery.data, search])

  const currentPage = listingsQuery.data?.page ?? page
  const lastPage = listingsQuery.data?.last_page ?? 1
  const total = listingsQuery.data?.total ?? 0

  const handleAccountChange = (id: number) => {
    const next = accounts.find((a) => a.id === id)
    if (next) {
      setAccount(next)
      setPage(1)
    }
  }

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <ListingsToolbar
        accounts={accounts}
        activeAccountId={activeAccount?.id ?? null}
        onAccountChange={handleAccountChange}
        search={search}
        onSearchChange={setSearch}
        perPage={perPage}
        onPerPageChange={setPerPage}
      />

      <Tabs
        value={status}
        onValueChange={(v) => handleStatusChange(v as StatusValue)}
      >
        <TabsList>
          {STATUS_TABS.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value}>
              {tab.label}
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      <ListingsTable
        listings={filteredListings}
        isLoading={listingsQuery.isLoading}
        isError={listingsQuery.isError}
        hasAccount={!!activeAccount}
        total={total}
      />

      {activeAccount && lastPage > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground">
            Ukupno {total} {total === 1 ? "artikal" : "artikala"}
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={currentPage <= 1}
            >
              <ChevronLeftIcon className="size-4" />
              Prethodna
            </Button>
            <span className="text-sm text-muted-foreground">
              Stranica {currentPage} od {lastPage}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(lastPage, p + 1))}
              disabled={currentPage >= lastPage}
            >
              Sljedeća
              <ChevronRightIcon className="size-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
