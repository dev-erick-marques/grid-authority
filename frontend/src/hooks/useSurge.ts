import { useState, useCallback } from 'react'
import type { DeviceSurgeState } from './useDeviceStream'


type SurgeAction = 'start' | 'stop' | 'cycle/start' | 'cycle/stop'

interface UseSurgeReturn {
  surgeState: DeviceSurgeState | null
  loading:    boolean
  error:      string | null
  trigger:    (action: SurgeAction) => Promise<void>
}

export function useSurge(deviceId: string): UseSurgeReturn {
  const [surgeState, setSurgeState] = useState<DeviceSurgeState | null>(null)
  const [loading, setLoading]       = useState(false)
  const [error,   setError]         = useState<string | null>(null)

  const trigger = useCallback(async (action: SurgeAction) => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`/api/devices/${deviceId}/surge/${action}`, { method: 'POST' })

      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        throw new Error(body?.detail ?? `HTTP ${res.status}`)
      }

      const data = await res.json() as { surgeState: DeviceSurgeState }
      setSurgeState(data.surgeState)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }, [deviceId])

  return { surgeState, loading, error, trigger }
}