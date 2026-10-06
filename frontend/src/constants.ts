export interface MetricConfig {
  key: 'mean' | 'cv' | 'trend' | 'riskScore' | 'forecast'
  label: string
  unit: string
  color: string
  threshold: number | null
}
export const METRICS: MetricConfig[] = [
 { key:'mean', label:'Mean', unit:'V', color:'#00e5ff', threshold:null },
 { key:'cv', label:'CV', unit:'%', color:'#ff4757', threshold:null },
 { key:'trend', label:'Trend', unit:'V/s', color:'#ff9f43', threshold:null },
 { key:'riskScore', label:'Risk', unit:'%', color:'#a29bfe', threshold:null },
 { key:'forecast', label:'Forecast', unit:'V', color:'#2ed573', threshold:null },
]
