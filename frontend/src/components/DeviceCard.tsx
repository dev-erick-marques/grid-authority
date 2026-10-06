import { useState, useEffect } from 'react'
import {
    LineChart, Line, XAxis, YAxis,
    CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine,
} from 'recharts'
import type { DeviceSurgeState } from '../hooks/useDeviceStream'
import { useRealStream } from '../hooks/useDeviceStream'
import type { MetricConfig } from '../constants'
import { METRICS } from '../constants'
import type { DiscoveredDevice } from '../hooks/useDevices'
import { CustomTooltip } from './CustomTooltip'
import { StatBadge } from './StatBadge'
import { SurgeToggle, SurgePanel } from './SurgePanel'

type DeviceState = 'ACTIVE' | 'SHUTDOWN'

function RestartCountdown({ stableCycle, stableCyclesRequired }: { stableCycle: number; stableCyclesRequired: number }) {
    const [tick, setTick] = useState(stableCycle)

    useEffect(() => {
        setTick(stableCycle)
    }, [stableCycle])

    useEffect(() => {
        const id = setInterval(() => setTick((t) => Math.min(t + 1, stableCyclesRequired)), 1000)
        return () => clearInterval(id)
    }, [stableCyclesRequired])

    return (
        <div style={{
            display: 'flex', alignItems: 'center', gap: 5,
            fontFamily: "'IBM Plex Mono', monospace",
            fontSize: 11,
            color: '#ff4757',
            letterSpacing: '0.04em',
        }}>
            {tick}<span style={{ opacity: 0.5 }}>/{stableCyclesRequired}</span>
        </div>
    )
}


function DecisionPill({ state, ready }: { state: DeviceState | undefined; ready: boolean }) {
    const shutdown = state === 'SHUTDOWN'
    const label = !ready ? 'WARMING UP' : shutdown ? 'SHUTDOWN' : 'ACTIVE'
    const color = !ready ? '#333' : shutdown ? '#ff4757' : '#2ed573'
    const bg = !ready ? 'rgba(255,255,255,0.03)' : shutdown ? 'rgba(255,71,87,0.12)' : 'rgba(46,213,115,0.1)'
    const border = !ready ? 'rgba(255,255,255,0.06)' : shutdown ? 'rgba(255,71,87,0.3)' : 'rgba(46,213,115,0.25)'

    return (
        <div className="decision-pill" style={{ background: bg, color, border: `1px solid ${border}` }}>
            {ready && (
                <span style={{
                    width: 5, height: 5, borderRadius: '50%', background: color,
                    boxShadow: `0 0 ${shutdown ? 8 : 6}px ${color}`,
                    animation: shutdown ? 'pulse 1s infinite' : 'none',
                }} />
            )}
            {label}
        </div>
    )
}

function EmptyChart() {
    return (
        <div className="empty-chart">
            <div className="empty-chart__bars">
                {[0, 1, 2, 3, 4].map((i) => (
                    <div
                        key={i}
                        className="empty-chart__bar"
                        style={{
                            animation: `bar-grow 1.2s ease-in-out ${i * 0.15}s infinite alternate`,
                            height: 20 + i * 6,
                        }}
                    />
                ))}
            </div>
            <span className="empty-chart__label">connecting to stream…</span>
        </div>
    )
}

function surgeCardStyle(surgeState: DeviceSurgeState, isShutdown: boolean) {
    if (surgeState === 'SURGE_ACTIVE') return {
        border: '1px solid rgba(255,159,67,0.45)',
        boxShadow: '0 0 60px rgba(255,159,67,0.1), inset 0 0 50px rgba(255,159,67,0.04)',
        accentGradient: 'linear-gradient(90deg, transparent, rgba(255,159,67,0.5), transparent)',
    }
    if (surgeState === 'CYCLE_ACTIVE') return {
        border: '1px solid rgba(162,155,254,0.45)',
        boxShadow: '0 0 60px rgba(162,155,254,0.1), inset 0 0 50px rgba(162,155,254,0.04)',
        accentGradient: 'linear-gradient(90deg, transparent, rgba(162,155,254,0.5), transparent)',
    }
    if (isShutdown) return {
        border: '1px solid rgba(255,71,87,0.35)',
        boxShadow: '0 0 60px rgba(255,71,87,0.07), inset 0 0 40px rgba(255,71,87,0.03)',
        accentGradient: 'linear-gradient(90deg, transparent, rgba(255,71,87,0.4), transparent)',
    }
    return {
        border: '1px solid rgba(255,255,255,0.07)',
        boxShadow: '0 1px 40px rgba(0,0,0,0.4)',
        accentGradient: 'linear-gradient(90deg, transparent, rgba(0,229,255,0.12), transparent)',
    }
}

interface DeviceCardProps {
    device: DiscoveredDevice
    metricKey: MetricConfig['key']
    thresholdCV: number
}

export function DeviceCard({ device, metricKey, thresholdCV }: DeviceCardProps) {
    const history = useRealStream(device.id)
    const latest = history[history.length - 1]
    const metric = METRICS.find((m) => m.key === metricKey) as MetricConfig
    const isShutdown = latest?.state === 'SHUTDOWN'
    const surgeState = latest?.surgeState ?? 'INACTIVE'

    const [surgeOpen, setSurgeOpen] = useState(false)

    const cardSt = surgeCardStyle(surgeState, isShutdown)

    return (
        <div
            className="card"
            style={{ border: cardSt.border, boxShadow: cardSt.boxShadow }}
        >
            <div className="card-accent-line" style={{ background: cardSt.accentGradient }} />

            <div className="card-header">
                <div>
                    <div className="card-device-id">{device.id}</div>
                    <div className="card-device-name">{device.label}</div>
                </div>
                <div className="card-header-actions">
                    {isShutdown && (latest?.stableCycle ?? 0) > 1 && (
                        <RestartCountdown
                            stableCycle={latest!.stableCycle}
                            stableCyclesRequired={latest!.stableCyclesRequired}
                        />
                    )}
                    <DecisionPill state={latest?.state} ready={!!latest} />
                    <SurgeToggle
                        surgeState={surgeState}
                        open={surgeOpen}
                        onClick={() => setSurgeOpen((o) => !o)}
                    />
                </div>
            </div>

            <div className="stats-row">
                <StatBadge label="Mean" value={latest?.mean.toFixed(1)} unit="V" color="#00e5ff" />
                <StatBadge label="Std Dev" value={latest?.std.toFixed(2)} unit="" color="#ff9f43" />
                <StatBadge label="CV" value={latest?.cv.toFixed(2)} unit="%" color="#ff4757" alert={(latest?.cv ?? 0) > thresholdCV} />
                <StatBadge label="Variance" value={latest?.variance.toFixed(2)} unit="" color="#a29bfe" />
            </div>

            <div className="chart-area">
                {history.length < 2 ? <EmptyChart /> : (
                    <ResponsiveContainer width="100%" height="100%">
                        <LineChart data={history} margin={{ top: 6, right: 8, bottom: 0, left: -8 }}>
                            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.035)" vertical={false} />
                            <XAxis
                                interval={3}
                                tickFormatter={(v) => new Date(v).toLocaleTimeString('pt-BR', { minute: '2-digit', second: '2-digit' })}
                                dataKey="evaluatedAt"
                                tick={{ fill: '#aaa9a9', fontSize: 10, fontFamily: "'IBM Plex Mono', monospace" }}
                                tickLine={false} axisLine={false}
                            />
                            <YAxis
                                tick={{ fill: '#aaa9a9', fontSize: 10, fontFamily: "'IBM Plex Mono', monospace" }}
                                tickLine={false} axisLine={false} width={54}
                            />
                            {metric.threshold !== null && (
                                <ReferenceLine
                                    y={metric.threshold}
                                    stroke="rgba(255,71,87,0.45)"
                                    strokeDasharray="5 4"
                                    label={{
                                        value: `CV ${metric.threshold}%`,
                                        fill: '#ff475799', fontSize: 9,
                                        fontFamily: "'IBM Plex Mono', monospace",
                                        position: 'insideTopRight',
                                    }}
                                />
                            )}
                            <Tooltip content={<CustomTooltip unit={metric.unit} />} />
                            <Line
                                type="monotone"
                                dataKey={metricKey}
                                stroke={metric.color}
                                strokeWidth={2}
                                dot={false}
                                activeDot={{ r: 5, fill: metric.color, strokeWidth: 0, opacity: 0.9 }}
                                isAnimationActive={false}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                )}
            </div>

            <div style={{
                overflow: 'hidden',
                maxHeight: surgeOpen ? '200px' : '0px',
                opacity: surgeOpen ? 1 : 0,
                transition: 'max-height 0.35s cubic-bezier(0.4,0,0.2,1), opacity 0.25s ease',
            }}>
                <SurgePanel deviceId={device.id} streamSurgeState={surgeState} />
            </div>

            {latest && (
                <div className="card-footer">
                    <span>LAST {history.length} WINDOWS</span>
                    <span style={{ color: '#aaa9a9' }}>
                        {latest.evaluatedAt
                            ? new Date(latest.evaluatedAt).toLocaleTimeString()
                            : `TICK #${latest.t}`}
                    </span>
                </div>
            )}
        </div>
    )
}