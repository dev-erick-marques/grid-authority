export const THRESHOLD_CV = 12.5

export interface MetricConfig {
  key: 'mean' | 'std' | 'cv' | 'variance'
  label: string
  unit: string
  color: string
  threshold: number | null
}

export const METRICS: MetricConfig[] = [
  { key: 'mean',     label: 'Mean',    unit: 'V', color: '#00e5ff', threshold: null },
  { key: 'std',      label: 'Std Dev', unit: '',  color: '#ff9f43', threshold: null },
  { key: 'cv',       label: 'CV',      unit: '%', color: '#ff4757', threshold: THRESHOLD_CV },
  { key: 'variance', label: 'Variance',unit: '',  color: '#a29bfe', threshold: null },
]