import { useState, useEffect } from 'react'

interface Policy {
  thresholdCV: number
  standard:    string
  description: string
}

interface UsePolicyReturn {
  policy:  Policy | null
  loading: boolean
  error:   string | null
}

export function usePolicy(): UsePolicyReturn {
  const [policy,  setPolicy]  = useState<Policy | null>(null)
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    fetch('/api/policy')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<Policy>
      })
      .then((data) => { if (!cancelled) setPolicy(data) })
      .catch((e: unknown) => {
        if (!cancelled)
          setError(e instanceof Error ? e.message : 'Failed to fetch policy')
      })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [])

  return { policy, loading, error }
}