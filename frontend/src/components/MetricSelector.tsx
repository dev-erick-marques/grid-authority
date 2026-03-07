import type { MetricConfig } from '../constants'
import { METRICS } from '../constants'

interface MetricSelectorProps {
    value: MetricConfig['key']
    onChange: (key: MetricConfig['key']) => void
}

export function MetricSelector({ value, onChange }: MetricSelectorProps) {
    return (
        <div className="metric-selector">
            {METRICS.map((m) => {
                const active = value === m.key
                return (
                    <button
                        key={m.key}
                        onClick={() => onChange(m.key)}
                        className="metric-btn"
                        style={{
                            border: active ? `1px solid ${m.color}50` : '1px solid rgba(255,255,255,0.07)',
                            background: active ? `${m.color}12` : 'rgba(255,255,255,0.025)',
                            color: active ? m.color : '#444',
                        }}
                    >
                        {m.label}
                        {active && (
                            <span style={{
                                display: 'inline-block',
                                width: 5, height: 5, borderRadius: '50%',
                                background: m.color,
                                marginLeft: 7, verticalAlign: 'middle',
                                boxShadow: `0 0 6px ${m.color}`,
                            }} />
                        )}
                    </button>
                )
            })}
        </div>
    )
}