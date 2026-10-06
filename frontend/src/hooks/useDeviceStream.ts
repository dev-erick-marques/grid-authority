import { useState,useEffect,useRef } from 'react'
export type DeviceState='ACTIVE'
export type DeviceSurgeState='INACTIVE'|'SURGE_ACTIVE'|'SAG_ACTIVE'|'CYCLE_ACTIVE'
export interface ChartEntry {
 t:number; mean:number; std:number; cv:number; trend:number; riskScore:number; forecast:number;
 timeToThreshold:number|null; confidence:number; observabilityScore:number; baselineDeviation:number;
 state:DeviceState; surgeState:DeviceSurgeState; decision:string; evaluatedAt?:string
}
export interface DeviceMetricsDTO {
 deviceId:string; deviceName:string; mean:number; std:number; cv:number; state:DeviceState; surgeState:DeviceSurgeState;
 evaluatedAt:string; trend:number; riskScore:number; forecast:number;
 confidence:number; observabilityScore:number; baselineDeviation:number; timeToThreshold:number|null; action:string
}
function normalise(raw:DeviceMetricsDTO,index:number):ChartEntry{
 return {t:index,mean:+raw.mean.toFixed(2),std:+raw.std.toFixed(4),cv:+raw.cv.toFixed(4),trend:+raw.trend.toFixed(4),
  riskScore:+raw.riskScore.toFixed(4),forecast:+raw.forecast.toFixed(2),timeToThreshold:raw.timeToThreshold,
  confidence:+raw.confidence.toFixed(4),observabilityScore:+raw.observabilityScore.toFixed(4),
  baselineDeviation:+raw.baselineDeviation.toFixed(4),state:raw.state,surgeState:raw.surgeState??'INACTIVE',
  decision:raw.action??'OBSERVE',evaluatedAt:raw.evaluatedAt}
}
export function useRealStream(deviceId:string):ChartEntry[]{
 const [history,setHistory]=useState<ChartEntry[]>([]); const tickRef=useRef(0)
 useEffect(()=>{
  tickRef.current=0
  fetch(`/api/devices/${deviceId}/metrics`).then(r=>r.json() as Promise<DeviceMetricsDTO[]>).then(data=>{
   const n=data.slice(-30).map(i=>{tickRef.current++;return normalise(i,tickRef.current)});setHistory(n)
  }).catch(console.error)
  const source=new EventSource('/api/stream')
  source.addEventListener('metrics',(e:MessageEvent<string>)=>{const raw=JSON.parse(e.data) as DeviceMetricsDTO;if(raw.deviceId!==deviceId)return;
   tickRef.current++;setHistory(h=>[...h.slice(-29),normalise(raw,tickRef.current)])})
  source.onerror=()=>source.close(); return()=>source.close()
 },[deviceId]); return history
}
