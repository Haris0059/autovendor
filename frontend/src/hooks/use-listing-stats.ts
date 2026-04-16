import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/api-client"
import { USE_MOCKS, mockDelay } from "@/lib/mocks"

export function useListingStats(accountId?: number) {
  return useQuery({
    queryKey: ["analytics", "listings", accountId],
    queryFn: async () => {
      if (USE_MOCKS) {
        await mockDelay(null, 400)
        // Generate mock data for the last 30 days
        const data = []
        const now = new Date()
        for (let i = 30; i >= 0; i--) {
          const d = new Date(now)
          d.setDate(d.getDate() - i)
          data.push({
            date: d.toISOString().split('T')[0],
            active: Math.floor(40 + Math.random() * 20),
            drafts: Math.floor(5 + Math.random() * 10),
            finished: Math.floor(10 + Math.random() * 15),
          })
        }
        
        const categories = [
          { name: "Vozila", count: 45, value: 450000 },
          { name: "Nekretnine", count: 12, value: 2400000 },
          { name: "Tehnika", count: 85, value: 35000 },
          { name: "Moj dom", count: 64, value: 12000 },
          { name: "Ostalo", count: 32, value: 5000 },
        ]

        return {
          history: data,
          categories,
          distribution: [
            { name: "Aktivni", value: 65, color: "#10b981" },
            { name: "Završeni", value: 25, color: "#6b7280" },
            { name: "Skriveni", value: 10, color: "#f59e0b" },
          ]
        }
      }
      return api.get(`/analytics/listings${accountId ? `?account_id=${accountId}` : ""}`)
    }
  })
}
