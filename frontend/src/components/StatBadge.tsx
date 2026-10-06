interface StatBadgeProps {
  label: string
  value: string | undefined
  unit: string
  color: string
  alert?: boolean
}

export function StatBadge({ label, value, unit, color, alert = false }: StatBadgeProps) {
  return (
    <div
      className="stat-badge"
      style={{
        background: alert ? 'rgba(255,71,87,0.05)' : 'rgba(255,255,255,0.03)',
        border: `1px solid ${alert ? 'rgba(255,71,87,0.3)' : 'rgba(255,255,255,0.06)'}`,
      }}
    >
      <div className="stat-badge__label">{label}</div>
      <div
        className="stat-badge__value"
        style={{
          color: alert ? '#ff4757' : color,
        }}
      >
        {value ?? <span style={{ color: '#2a2a2a', fontSize: 14 }}>—</span>}
        <span style={{ fontSize: 11, color: alert ? '#ff475780' : '#333', marginLeft: 3 }}>
          {unit}
        </span>
      </div>
    </div>
  )
}