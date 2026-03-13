import { useState, useEffect, useRef } from 'react'

export type DeviceState      = 'ACTIVE' | 'SHUTDOWN'
export type DeviceSurgeState = 'INACTIVE' | 'SURGE_ACTIVE' | 'CYCLE_ACTIVE'

export interface ChartEntry {
  t: number 
  mean: number
  std: number
  cv: number
  variance: number 
  shutdown: boolean 
  state: DeviceState
  surgeState: DeviceSurgeState
  decision: string 
  stableCycle: number
  stableCyclesRequired: number
  evaluatedAt?: string
}

export interface DeviceMetricsDTO {
  deviceId: string
  deviceName: string
  mean: number
  std: number
  cv: number
  state: DeviceState
  surgeState: DeviceSurgeState
  stableCycle: number
  stableCyclesRequired: number
  evaluatedAt: string
}

function normalise(raw: DeviceMetricsDTO, index: number): ChartEntry {
  const shutdown = raw.state === 'SHUTDOWN'
  return {
    t: index,
    mean: +raw.mean.toFixed(2),
    std: +raw.std.toFixed(4),
    cv: +raw.cv.toFixed(4),
    variance: +(raw.std * raw.std).toFixed(4),
    shutdown,
    state: raw.state,
    surgeState: raw.surgeState ?? 'INACTIVE',
    decision: shutdown ? 'SHUTDOWN' : 'STABLE',
    stableCycle: raw.stableCycle ?? 0,
    stableCyclesRequired: raw.stableCyclesRequired ?? 0,
    evaluatedAt: raw.evaluatedAt
  }
}

export function useRealStream(deviceId: string): ChartEntry[] {
  const [history, setHistory] = useState<ChartEntry[]>([])
  const tickRef = useRef(0)

  useEffect(() => {
    tickRef.current = 0

    fetch(`/api/devices/${deviceId}/metrics`)
      .then((r) => r.json() as Promise<DeviceMetricsDTO[]>)
      .then((data) => {
        const normalised = data.slice(-30).map((item) => {
          tickRef.current++
          return normalise(item, tickRef.current)
        })
        setHistory(normalised)
      })
      .catch(console.error)

    const source = new EventSource('/api/stream')
    source.addEventListener('metrics', (e: MessageEvent<string>) => {
      const raw = JSON.parse(e.data) as DeviceMetricsDTO
      if (raw.deviceId !== deviceId) return
      tickRef.current++
      setHistory((h) => [...h.slice(-29), normalise(raw, tickRef.current)])
    })
    source.onerror = () => source.close()
    return () => source.close()
  }, [deviceId])

  return history
}