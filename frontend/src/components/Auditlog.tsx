import { useAuditStream } from '../hooks/useAuditStream'
import type { AuditEntry, AuditEntryType } from '../hooks/useAuditStream'

const TYPE_META: Record<AuditEntryType, {
    color: string
    bg: string
    border: string
    label: string
    detail: (entry: AuditEntry) => string
}> = {
    DECISION: {
        color: '#00e5ff', bg: 'rgba(0,229,255,0.06)', border: 'rgba(0,229,255,0.18)', label: 'SIGNED',
        detail: (e) => `${e.action ?? '?'} · ${e.deviceId ?? '?'}`,
    },
    AUDIT: {
        color: '#2ed573', bg: 'rgba(46,213,115,0.06)', border: 'rgba(46,213,115,0.18)', label: 'AUDIT',
        detail: (e) => `${e.action ?? e.eventType ?? '?'} · ${e.deviceId ?? '?'}`,
    },
    ERROR: {
        color: '#ff4757', bg: 'rgba(255,71,87,0.07)', border: 'rgba(255,71,87,0.22)', label: 'ERR',
        detail: (e) => `${e.eventType ?? '?'} · ${e.deviceId ?? '?'} · ${e.error?.slice(0, 60) ?? '?'}`,
    },
}

function fmt(ts: number) {
    return new Date(ts).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function AuditRow({ entry }: { entry: AuditEntry }) {
    const meta = TYPE_META[entry.type]
    if (!meta) return null

    return (
        <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '7px 12px',
            borderRadius: 8,
            background: meta.bg,
            border: `1px solid ${meta.border}`,
            fontFamily: "'IBM Plex Mono', monospace",
            fontSize: 11,
            lineHeight: 1.4,
            animation: 'fade-in 0.2s ease',
        }}>
            <span style={{
                minWidth: 36,
                textAlign: 'center',
                fontSize: 9,
                fontWeight: 700,
                letterSpacing: '0.12em',
                color: meta.color,
                background: `${meta.color}18`,
                border: `1px solid ${meta.color}40`,
                borderRadius: 4,
                padding: '2px 5px',
                flexShrink: 0,
            }}>
                {meta.label}
            </span>

            <span style={{ flex: 1, color: '#c8c8c8', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {meta.detail(entry)}
            </span>

            <span style={{ color: '#444', fontSize: 10, whiteSpace: 'nowrap' }}>
                {fmt(entry.ts)}
            </span>
        </div>
    )
}


export function AuditLog() {
    const entries = useAuditStream()

    return (
        <section style={{ marginTop: 48 }}>

            <div style={{
                display: 'flex',
                alignItems: 'flex-end',
                justifyContent: 'space-between',
                marginBottom: 14,
                flexWrap: 'wrap',
                gap: 16,
            }}>

                <div>
                    <p style={{
                        fontFamily: "'IBM Plex Mono', monospace",
                        fontSize: 10,
                        letterSpacing: '0.28em',
                        color: 'rgba(0,229,255,0.45)',
                        textTransform: 'uppercase',
                        marginBottom: 4,
                    }}>
                        Cryptographic Audit
                    </p>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
                        <h2 style={{ fontSize: 18, fontWeight: 800, letterSpacing: '-0.02em', color: '#f2f2f2', margin: 0 }}>
                            Live Event Log
                        </h2>
                    </div>
                </div>
                <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
                    {(Object.entries(TYPE_META) as [AuditEntryType, typeof TYPE_META[AuditEntryType]][]).map(([, m]) => (
                        <div key={m.label} style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
                            <span style={{ width: 6, height: 6, borderRadius: '50%', background: m.color, flexShrink: 0 }} />
                            <span style={{ fontFamily: "'IBM Plex Mono', monospace", fontSize: 10, color: '#8f8f8f', letterSpacing: '0.1em' }}>
                                {m.label}
                            </span>
                        </div>
                    ))}
                </div>
            </div>

            <div style={{
                background: 'rgba(255,255,255,0.018)',
                border: '1px solid rgba(255,255,255,0.07)',
                borderRadius: 14,
                padding: '12px 14px',
                display: 'flex',
                flexDirection: 'column',
                gap: 6,
                maxHeight: 360,
                overflowY: 'auto',
            }}>
                {entries.length === 0 ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '32px 0',
                        fontFamily: "'IBM Plex Mono', monospace",
                        fontSize: 11,
                        color: '#2a2a2a',
                        letterSpacing: '0.12em',
                    }}>
                        waiting for events…
                    </div>
                ) : (
                    entries.map((e) => <AuditRow key={e.id} entry={e} />)
                )}
            </div>
        </section>
    )
}