import { useState, useEffect } from 'react'
import type { DeviceMetricsDTO } from './useDeviceStream'

export interface DiscoveredDevice {
  id:    string
  label: string
}

interface UseDevicesReturn {
  devices: DiscoveredDevice[]
  loading: boolean
  error:   string | null
}

export function useDevices(): UseDevicesReturn {
  const [devices, setDevices] = useState<DiscoveredDevice[]>([])
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    fetch('/api/devices/metrics')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json() as Promise<Record<string, DeviceMetricsDTO[]>>
      })
      .then((data) => {
        if (cancelled) return

        const discovered: DiscoveredDevice[] = Object.entries(data).map(([id, history]) => ({
          id,
          label: history[0]?.deviceName ?? id,
        }))

        if (discovered.length === 0) {
          setError('No devices found. Make sure at least one device is sending telemetry.')
        } else {
          setDevices(discovered)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to reach coordinator')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => { cancelled = true }
  }, [])

  return { devices, loading, error }
}