import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/api-client"
import { USE_MOCKS, mockDelay } from "@/lib/mocks"

export function useSponsorHistory(accountId?: number) {
  return useQuery({
    queryKey: ["analytics", "sponsors", accountId],
    queryFn: async () => {
      if (USE_MOCKS) {
        await mockDelay(null, 300)
        const data = []
        const now = new Date()
        for (let i = 6; i >= 0; i--) {
          const d = new Date(now)
          d.setMonth(d.getMonth() - i)
          data.push({
            month: d.toLocaleString('bs-BA', { month: 'short' }),
            credits: Math.floor(500 + Math.random() * 2000),
            active_sponsors: Math.floor(2 + Math.random() * 8),
          })
        }
        return data
      }
      return api.get(`/analytics/sponsors${accountId ? `?account_id=${accountId}` : ""}`)
    }
  })
}
