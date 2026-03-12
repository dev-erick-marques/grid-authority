import "./styles/global.css"
import { useState } from 'react'
import type { MetricConfig } from './constants'
import { useDevices } from './hooks/useDevices'
import { LoadingScreen } from './components/LoadingScreen'
import { MetricSelector } from './components/MetricSelector'
import { DeviceCard } from './components/DeviceCard'
import { usePolicy } from './hooks/usePolicy'
import { AuditLog } from './components/Auditlog'

export default function App() {
  const [metricKey, setMetricKey] = useState<MetricConfig['key']>('cv')
  
  const { devices, loading: loadingDevices, error: errorDevices } = useDevices()
  const { policy,  loading: loadingPolicy,  error: errorPolicy  } = usePolicy()

  const loading = loadingDevices || loadingPolicy
  const error   = errorDevices  ?? errorPolicy
  


  if (loading || error || !policy ||devices.length === 0) {
    return <LoadingScreen error={error} />
  }

  return (
    <div className="app-shell">
      <div className="ambient-glow" />

      <div className="app-wrapper">
        <header className="app-header">
          <div>
            <p className="app-eyebrow">Coordinator Monitor</p>
            <h1 className="app-title">GridAuthority</h1>
            <p className="app-subtitle">
              Sliding window · SSE stream · CV threshold {policy.thresholdCV}%
            </p>
          </div>
          <MetricSelector value={metricKey} onChange={setMetricKey} />
        </header>

        <div className="device-grid">
          {devices.map((d) => (
            <DeviceCard key={d.id} device={d} metricKey={metricKey} thresholdCV={policy.thresholdCV}/>
          ))}
        </div>

        <div className="legend-bar">
          {([
            { color: '#2ed573', label: 'ACTIVE — CV within threshold' },
            { color: '#ff4757', label: 'SHUTDOWN — CV exceeded 12.5%' },
            { color: '#ff9f43', label: 'SURGE ACTIVE — manual spike' },
            { color: '#a29bfe', label: 'CYCLE ACTIVE — auto loop' },
          ] as const).map(({ color, label }) => (
            <div key={label} className="legend-item">
              <span className="legend-dot" style={{ background: color }} />
              {label}
            </div>
          ))}
        </div>
        <AuditLog />
      </div>
    </div>
  )
}