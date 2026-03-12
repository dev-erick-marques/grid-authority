import { useState, useEffect } from 'react'

export type AuditEntryType = 'KMS_SIGNED' | 'HCS_ANCHORED' | 'HCS_ERROR'

export interface AuditEntry {
    id: number
    type: AuditEntryType
    eventType?: string
    deviceName?: string
    deviceId?: string
    action?: string
    keyId?: string
    topicId?: string
    sha256?: string
    error?: string
    ts: number
}

let _seq = 0

export function useAuditStream(max = 80): AuditEntry[] {
    const [log, setLog] = useState<AuditEntry[]>([])

    useEffect(() => {
        const source = new EventSource('/api/audit/stream')

        source.addEventListener('audit', (e: MessageEvent<string>) => {
            const raw = JSON.parse(e.data)
            const entry: AuditEntry = { id: ++_seq, ...raw }
            setLog((prev) => [entry, ...prev].slice(0, max))
        })

        source.onerror = () => source.close()
        return () => source.close()
    }, [max])

    return log
}