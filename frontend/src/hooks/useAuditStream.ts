import { useState, useEffect } from 'react'
export type AuditEntryType = 'DECISION' | 'AUDIT' | 'ERROR'
export interface AuditEntry {
 id:number; type:AuditEntryType; eventType?:string; deviceId?:string; action?:string;
 policyVersion?:number; riskScore?:number; confidence?:number; timeToThreshold?:number;
 observabilityScore?:number; metricsHash?:string; commandHash?:string; ts:number;
}
let _seq=0
export function useAuditStream(max=80):AuditEntry[]{
 const [log,setLog]=useState<AuditEntry[]>([])
 useEffect(()=>{
  const source=new EventSource('/api/audit/stream')
  source.addEventListener('audit',(e:MessageEvent<string>)=>{
   const raw=JSON.parse(e.data); const entry:AuditEntry={id:++_seq,...raw}
   setLog(prev=>[entry,...prev].slice(0,max))
  })
  source.onerror=()=>source.close(); return()=>source.close()
 },[max]); return log
}
