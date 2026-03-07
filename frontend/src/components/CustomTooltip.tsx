import type { ChartEntry } from '../hooks/useDeviceStream'

interface CustomTooltipProps {
    active?: boolean
    payload?: Array<{ value: number; color: string; payload: ChartEntry }>
    label?: number
    unit: string
}

export function CustomTooltip({ active, payload, label, unit }: CustomTooltipProps) {
    if (!active || !payload?.length) return null

    const d = payload[0].payload

    return (
        <div className="chart-tooltip">
            <div className="chart-tooltip__window">WINDOW #{label}</div>

            <div
                className="chart-tooltip__value"
                style={{ color: payload[0].color }}
            >
                {payload[0].value}
                <span className="chart-tooltip__unit">{unit}</span>
            </div>

            <div
                className="chart-tooltip__decision"
                style={{ color: d.shutdown ? '#ff4757' : '#2ed573' }}
            >
                <span style={{
                    width: 6, height: 6, borderRadius: '50%',
                    background: d.shutdown ? '#ff4757' : '#2ed573',
                    display: 'inline-block',
                }} />
                {d.decision}
            </div>
        </div>
    )
}