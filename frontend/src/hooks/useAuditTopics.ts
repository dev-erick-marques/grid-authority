import { useState, useEffect } from 'react'

export interface HcsTopics {
    network: string
    publicKeyTopicId: string | null
    decisionTopicId: string | null
    surgeTopicId: string | null
}

export function useAuditTopics(): HcsTopics | null {
    const [topics, setTopics] = useState<HcsTopics | null>(null)

    useEffect(() => {
        fetch('/api/audit/topics')
            .then((r) => r.json() as Promise<HcsTopics>)
            .then(setTopics)
            .catch(console.error)
    }, [])

    return topics
}