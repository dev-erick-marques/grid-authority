export interface AuditTopics {
  enabled: boolean
}
export function useAuditTopics(): AuditTopics { return { enabled: false } }
