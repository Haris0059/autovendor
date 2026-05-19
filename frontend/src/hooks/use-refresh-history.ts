import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/api-client"
import { USE_MOCKS, mockDelay } from "@/lib/mocks"

export function useRefreshHistory(accountId?: number) {
  return useQuery<{ date: string; count: number; budget_used: number }[]>({
    queryKey: ["analytics", "refresh", accountId],
    queryFn: async () => {
      if (USE_MOCKS) {
        await mockDelay(null, 300)
        const data = []
        const now = new Date()
        for (let i = 14; i >= 0; i--) {
          const d = new Date(now)
          d.setDate(d.getDate() - i)
          data.push({
            date: d.toISOString().split('T')[0],
            count: Math.floor(100 + Math.random() * 150),
            budget_used: Math.floor(20 + Math.random() * 30),
          })
        }
        return data
      }
      return api.get(`/analytics/refresh${accountId ? `?account_id=${accountId}` : ""}`)
    }
  })
}
