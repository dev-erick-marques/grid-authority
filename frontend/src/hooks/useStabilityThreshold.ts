import { useState, useEffect } from 'react'

interface StabilityThreshold {
  thresholdCV: number
  standard: string
  description: string
}

interface UseStabilityThresholdReturn {
  threshold: StabilityThreshold | null
  loading: boolean
  error: string | null
}

export function useStabilityThreshold(): UseStabilityThresholdReturn {
  const [threshold, setThreshold] = useState<StabilityThreshold | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    fetch('/api/stability/threshold')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<StabilityThreshold>
      })
      .then((data) => { if (!cancelled) setThreshold(data) })
      .catch((e: unknown) => {
        if (!cancelled)
          setError(e instanceof Error ? e.message : 'Failed to fetch threshold')
      })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => { cancelled = true }
  }, [])

  return { threshold, loading, error }
}