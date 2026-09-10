/** Tipos de la API Enterprise consumidos por el dashboard integrado en kin-frontend. */

export type EnterpriseProgressState =
  | "REQUESTED"
  | "RUNNING"
  | "DOCUMENT_GENERATED"
  | "COMPLETED"
  | "FAILED";

export interface EnterpriseProgressEvent {
  projectId: string;
  version: number;
  state: EnterpriseProgressState;
  timestamp: string;
  documentType?: string | null;
  message?: string | null;
}

export interface EnterpriseDocument {
  id: string;
  type: string;
  size: number;
  createdAt: string;
  generatedBy: string;
  engineVersion: string;
  version: number;
  inputHash: string;
  renderFormat?: string | null;
  mimeType?: string | null;
  checksum?: string | null;
}

export interface EnterpriseVersion {
  version: number;
  status: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
  failedReason?: string | null;
  documentCount: number;
}

export interface EnterpriseScoreSection {
  overall?: number | null;
  grade?: string | null;
  confidence?: number | null;
  market?: number | null;
  innovation?: number | null;
  viability?: number | null;
  financial?: number | null;
  risk?: number | null;
  scalability?: number | null;
  team?: number | null;
  sustainability?: number | null;
}

export interface EnterpriseDashboard {
  projectId: string;
  version: number;
  status: string;
  progress: number;
  documentCount: number;
  versionsCount: number;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
  failedReason?: string | null;
  generationDurationMillis?: number | null;
  score?: EnterpriseScoreSection | null;
  documents: EnterpriseDocument[];
  versions: EnterpriseVersion[];
  statistics: Record<string, number>;
}

export const TERMINAL_STATES: readonly EnterpriseProgressState[] = [
  "COMPLETED",
  "FAILED",
];

export type InfoState =
  | "CONFIRMED"
  | "IMPORTED"
  | "CALCULATED"
  | "ESTIMATED"
  | "AI_SUGGESTED"
  | "PENDING"
  | "NOT_AVAILABLE";

export type InfoSourceType =
  | "USER_INPUT"
  | "IMPORTED_DOCUMENT"
  | "CALCULATED"
  | "ESTIMATED"
  | "AI_SUGGESTED";

export interface InfoValue {
  value: string | null;
  sourceType: InfoSourceType | null;
  state: InfoState;
  origin: string | null;
}

export interface ResolvedDimension {
  dimension: string;
  displayName: string;
  value: string | null;
  sourceType: InfoSourceType | null;
  state: InfoState;
  origin: string | null;
}

export interface InfoDocument {
  id: string;
  filename: string;
  mimeType: string;
  size: number;
  status: string;
  createdAt: string | null;
  summary: string | null;
  relevantFacts: string[];
}

export interface SupplementalInfo {
  financial: Record<string, InfoValue>;
  market: Record<string, InfoValue>;
  impact: Record<string, InfoValue>;
  risk: Record<string, InfoValue>;
  breakeven: InfoValue;
  breakevenMissing: string[];
  breakevenCalculable: boolean;
}

export interface EnterpriseInformation {
  resolvedDimensions: ResolvedDimension[];
  supplemental: SupplementalInfo;
  documents: InfoDocument[];
  conflicts: string[];
}
