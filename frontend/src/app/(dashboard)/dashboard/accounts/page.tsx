"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { PlusIcon, MoreHorizontalIcon, PencilIcon, Trash2Icon, UserIcon } from "lucide-react"
import { EditProfileDialog, type OlxAccount } from "@/components/edit-profile-dialog"
import { AddProfileDialog } from "@/components/add-profile-dialog"

const mockAccounts: OlxAccount[] = [
  {
    id: "1",
    username: "Alpus",
    email: "pusculalen@gmail.com",
    phone: "38761644432",
    businessName: "Alpus d.o.o",
    package: "Bronze",
    credits: 117,
    tokenValid: true,
    status: "Aktivan",
    listings: 42,
    lastSync: "Prije 2 sata",
    avatar: "https://d4n0y8dshd77z.cloudfront.net/avatars/3936696/1763226022.86756 2-d72a159ac35a.png",
  },
  {
    id: "2",
    username: "tech_store_ba",
    email: "info@techstore.ba",
    phone: "38762123456",
    businessName: "Tech Store d.o.o",
    package: "Silver",
    credits: 250,
    tokenValid: true,
    status: "Aktivan",
    listings: 18,
    lastSync: "Prije 5 sati",
  },
  {
    id: "3",
    username: "auto_dijelovi",
    email: "auto@dijelovi.ba",
    phone: "38763987654",
    businessName: "Auto Dijelovi",
    package: "Free",
    credits: 0,
    tokenValid: false,
    status: "Istekao token",
    listings: 7,
    lastSync: "Prije 3 dana",
  },
]

export default function AccountsPage() {
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [editAccount, setEditAccount] = useState<OlxAccount | null>(null)
  const [editDialogOpen, setEditDialogOpen] = useState(false)

  return (
    <div className="flex flex-col gap-4 px-4 py-4 md:gap-6 md:py-6 lg:px-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">OLX Profili</h1>
          <p className="text-muted-foreground">Upravljanje povezanim OLX.ba računima</p>
        </div>
        <Button onClick={() => setAddDialogOpen(true)}>
          <PlusIcon className="mr-2 size-4" />
          Dodaj Profil
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Povezani Profili</CardTitle>
          <CardDescription>{mockAccounts.length} profila povezano</CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Profil</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Artikli</TableHead>
                <TableHead>Zadnja sinhronizacija</TableHead>
                <TableHead className="w-12"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {mockAccounts.map((account) => (
                <TableRow key={account.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <UserIcon className="size-4 text-muted-foreground" />
                      <span className="font-medium">{account.username}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={account.tokenValid ? "default" : "destructive"}>
                      {account.status}
                    </Badge>
                  </TableCell>
                  <TableCell>{account.listings}</TableCell>
                  <TableCell className="text-muted-foreground">{account.lastSync}</TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="size-8">
                          <MoreHorizontalIcon className="size-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => {
                            setEditAccount(account)
                            setEditDialogOpen(true)
                          }}
                        >
                          <PencilIcon className="mr-2 size-4" />
                          Uredi
                        </DropdownMenuItem>
                        <DropdownMenuItem variant="destructive">
                          <Trash2Icon className="mr-2 size-4" />
                          Obriši
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <AddProfileDialog
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
      />
      <EditProfileDialog
        account={editAccount}
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
      />
    </div>
  )
}
