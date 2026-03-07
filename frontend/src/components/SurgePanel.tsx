import { useState } from 'react'
import type { DeviceSurgeState } from '../hooks/useDeviceStream'
import { useSurge } from '../hooks/useSurge'

const SURGE_CONFIG: Record<DeviceSurgeState, { label: string; color: string; glow: string }> = {
    INACTIVE: { label: 'INACTIVE', color: '#3a4050', glow: 'transparent' },
    SURGE_ACTIVE: { label: 'SURGE ACTIVE', color: '#ff9f43', glow: 'rgba(255,159,67,0.35)' },
    CYCLE_ACTIVE: { label: 'CYCLE ACTIVE', color: '#a29bfe', glow: 'rgba(162,155,254,0.35)' },
}

interface SurgeToggleProps {
    surgeState: DeviceSurgeState
    open: boolean
    onClick: () => void
}

export function SurgeToggle({ surgeState, open, onClick }: SurgeToggleProps) {
    const cfg = SURGE_CONFIG[surgeState]
    const isActive = surgeState !== 'INACTIVE'

    return (
        <button
            onClick={onClick}
            title="Surge Controls"
            className="surge-toggle"
            style={{
                border: `1px solid ${isActive ? cfg.color + '60' : 'rgba(255,255,255,0.08)'}`,
                background: open ? `${cfg.color}18` : isActive ? `${cfg.color}0e` : 'rgba(255,255,255,0.03)',
                color: isActive ? cfg.color : '#444',
                boxShadow: isActive ? `0 0 12px ${cfg.glow}` : 'none',
            }}
        >
            <span style={{
                fontSize: 12,
                animation: isActive ? 'surge-pulse 1.4s ease-in-out infinite' : 'none',
                display: 'inline-block',
            }}>
                ⚡
            </span>
            <span style={{ fontSize: 9 }}>
                {isActive ? cfg.label : 'SURGE'}
            </span>
            <span style={{
                display: 'inline-block',
                width: 4, height: 4, borderRadius: '50%',
                background: isActive ? cfg.color : '#2a2a2a',
                boxShadow: isActive ? `0 0 6px ${cfg.color}` : 'none',
                transition: 'all 0.3s ease',
                animation: isActive ? 'surge-pulse 1.4s ease-in-out infinite' : 'none',
            }} />
            <span
                className="surge-toggle__chevron"
                style={{ transform: open ? 'rotate(180deg)' : 'rotate(0deg)' }}
            >
                ▾
            </span>
        </button>
    )
}

interface SurgePanelProps {
    deviceId: string
    streamSurgeState: DeviceSurgeState
}

export function SurgePanel({ deviceId, streamSurgeState }: SurgePanelProps) {
    const { surgeState: localState, loading, error, trigger } = useSurge(deviceId)

    const effective = localState ?? streamSurgeState
    const cfg = SURGE_CONFIG[effective]
    const isSurge = effective === 'SURGE_ACTIVE'
    const isCycle = effective === 'CYCLE_ACTIVE'
    const isActive = isSurge || isCycle

    return (
        <div
            className="surge-panel"
            style={{
                borderTop: `1px solid ${isActive ? cfg.color + '30' : 'rgba(255,255,255,0.05)'}`,
            }}
        >
            <div className="surge-panel__header">
                <div className="surge-panel__label">SURGE CONTROL</div>
                <div className="surge-panel__state" style={{ color: cfg.color }}>
                    <span style={{
                        width: 5, height: 5, borderRadius: '50%',
                        background: cfg.color,
                        boxShadow: isActive ? `0 0 8px ${cfg.color}` : 'none',
                        transition: 'all 0.4s',
                        animation: isActive ? 'surge-pulse 1.4s ease-in-out infinite' : 'none',
                    }} />
                    {cfg.label}
                </div>
            </div>

            <div className="surge-panel__grid">
                <SurgeBtn
                    label={isSurge ? '⚡ Stop Surge' : '⚡ Force Surge'}
                    description="Manual voltage spike"
                    active={isSurge}
                    color="#ff9f43"
                    loading={loading}
                    onClick={() => trigger(isSurge ? 'stop' : 'start')}
                />
                <SurgeBtn
                    label={isCycle ? '↺ Stop Cycle' : '↺ Auto Cycle'}
                    description="Periodic surge loop"
                    active={isCycle}
                    color="#a29bfe"
                    loading={loading}
                    onClick={() => trigger(isCycle ? 'cycle/stop' : 'cycle/start')}
                />
            </div>

            {error && (
                <div className="surge-panel__error">✕ {error}</div>
            )}
        </div>
    )
}

interface SurgeBtnProps {
    label: string
    description: string
    active: boolean
    color: string
    loading: boolean
    onClick: () => void
}

function SurgeBtn({ label, description, active, color, loading, onClick }: SurgeBtnProps) {
    const [hover, setHover] = useState(false)

    return (
        <button
            onClick={onClick}
            disabled={loading}
            onMouseEnter={() => setHover(true)}
            onMouseLeave={() => setHover(false)}
            className="surge-btn"
            style={{
                border: `1px solid ${active || hover ? color + '50' : 'rgba(255,255,255,0.07)'}`,
                background: active ? `${color}18` : hover ? `${color}0d` : 'rgba(255,255,255,0.025)',
                color: active ? color : hover ? color : '#555',
                cursor: loading ? 'wait' : 'pointer',
                boxShadow: active ? `0 0 20px ${color}20, inset 0 0 12px ${color}08` : 'none',
                opacity: loading ? 0.6 : 1,
            }}
        >
            <div className="surge-btn__label">
                {loading && (
                    <span style={{ animation: 'spin 0.8s linear infinite', display: 'inline-block' }}>◌</span>
                )}
                {label}
            </div>
            <div className="surge-btn__description">{description}</div>
        </button>
    )
}