import { medicalApi } from '@/services/api';
import type { EncounterFormData, EncounterUpdateData } from '@/lib/hce/schemas/encounter.schema';
import type { PatientIdentificationFormData } from '@/lib/hce/schemas/encounter.schema';
import type { AnamnesisFormData } from '@/lib/hce/schemas/anamnesis.schema';
import type { PhysicalExamFormData } from '@/lib/hce/schemas/physicalExam.schema';
import type { DiagnosisFormData, TreatmentPlanFormData, MedicalOrderFormData } from '@/lib/hce/schemas/encounter.schema';
import type { HistoryFormData } from '@/lib/hce/schemas/history.schema';

export interface EncounterResponse {
  id: string;
  patientId: string;
  encounterType: string;
  chiefComplaint: string;
  status: string;
  startedAt: string;
  closedAt: string | null;
}

export interface PatientIdentificationResponse {
  id: string;
  patientId: string;
  documentType: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: string;
  bloodType: string | null;
  rhFactor: string | null;
  eps: string | null;
  regime: string | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  city: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelation: string | null;
  guardianDocumentType: string | null;
  guardianDocumentNumber: string | null;
  guardianName: string | null;
}

export interface AnamnesisResponse {
  id: string;
  encounterId: string;
  onsetDatetime: string | null;
  evolutionDescription: string | null;
  aggravatingFactors: string | null;
  alleviatingFactors: string | null;
  associatedSymptoms: string | null;
  severitySelfReported: number | null;
  systemsReview: Record<string, string | null> | null;
  previousEpisodes: string | null;
  previousTreatments: string | null;
  functionalImpact: string | null;
}

export interface PhysicalExamResponse {
  id: string;
  encounterId: string;
  bpSystolic: number | null;
  bpDiastolic: number | null;
  heartRate: number | null;
  respiratoryRate: number | null;
  temperature: number | null;
  spo2: number | null;
  weightKg: number | null;
  heightCm: number | null;
  bmi: number | null;
  glasgowScore: number | null;
  painScale: number | null;
  headNeck: string | null;
  cardiovascular: string | null;
  respiratory: string | null;
  abdominal: string | null;
  neurological: string | null;
  musculoskeletal: string | null;
  skin: string | null;
  genitourinary: string | null;
  psychiatric: string | null;
}

export interface DiagnosisResponse {
  id: string;
  encounterId: string;
  cie10Code: string;
  cie10Description: string | null;
  type: string;
  certainty: string;
  isPrincipal: boolean;
}

export interface TreatmentPlanResponse {
  id: string;
  encounterId: string;
  conduct: string | null;
  objectives: string | null;
  followUpInstructions: string | null;
  prognosis: string | null;
  referredTo: string | null;
  referralReason: string | null;
}

export interface MedicalOrderResponse {
  id: string;
  encounterId: string;
  orderType: string;
  description: string;
  frequency: string | null;
  duration: string | null;
  priority: string | null;
  notes: string | null;
}

export interface HistoryResponse {
  allergies: Array<{ id: string; allergen: string; reaction: string | null; severity: string | null }>;
  surgeries: Array<{ id: string; procedure: string; date: string; complications: string | null }>;
  medications: Array<{ id: string; name: string; dosage: string | null; isActive: boolean }>;
  vaccines: Array<{ id: string; vaccine: string; date: string }>;
  familyHistory: Array<{ id: string; relative: string; condition: string }>;
  toxicHabits: Array<{ id: string; substance: string; isActive: boolean }>;
  gynecoObstetric: Record<string, unknown> | null;
}

export const hceApi = {
  // Encounter CRUD
  getEncounter: (encounterId: string) =>
    medicalApi.get<EncounterResponse>(`/hce/encounters/${encounterId}`),

  updateEncounter: (encounterId: string, data: EncounterUpdateData) =>
    medicalApi.put<EncounterResponse>(`/hce/encounters/${encounterId}`, data),

  closeEncounter: (encounterId: string) =>
    medicalApi.post<EncounterResponse>(`/hce/encounters/${encounterId}/close`, {}),

  // Patient Identification
  getPatientIdentification: (patientId: string) =>
    medicalApi.get<PatientIdentificationResponse>(`/hce/patients/${patientId}/identification`),

  updatePatientIdentification: (patientId: string, data: PatientIdentificationFormData) =>
    medicalApi.put<PatientIdentificationResponse>(`/hce/patients/${patientId}/identification`, data),

  // Anamnesis
  getAnamnesis: (encounterId: string) =>
    medicalApi.get<AnamnesisResponse>(`/hce/encounters/${encounterId}/anamnesis`),

  updateAnamnesis: (encounterId: string, data: AnamnesisFormData) =>
    medicalApi.put<AnamnesisResponse>(`/hce/encounters/${encounterId}/anamnesis`, data),

  // Physical Exam
  getPhysicalExam: (encounterId: string) =>
    medicalApi.get<PhysicalExamResponse>(`/hce/encounters/${encounterId}/physical-exam`),

  updatePhysicalExam: (encounterId: string, data: PhysicalExamFormData) =>
    medicalApi.put<PhysicalExamResponse>(`/hce/encounters/${encounterId}/physical-exam`, data),

  // Diagnoses
  getDiagnoses: (encounterId: string) =>
    medicalApi.get<DiagnosisResponse[]>(`/hce/encounters/${encounterId}/diagnoses`),

  createDiagnosis: (encounterId: string, data: DiagnosisFormData) =>
    medicalApi.post<DiagnosisResponse>(`/hce/encounters/${encounterId}/diagnoses`, data),

  updateDiagnosis: (diagnosisId: string, data: Partial<DiagnosisFormData>) =>
    medicalApi.put<DiagnosisResponse>(`/hce/diagnoses/${diagnosisId}`, data),

  deleteDiagnosis: (diagnosisId: string) =>
    medicalApi.delete(`/hce/diagnoses/${diagnosisId}`),

  setPrincipalDiagnosis: (diagnosisId: string) =>
    medicalApi.put<DiagnosisResponse>(`/hce/diagnoses/${diagnosisId}/principal`, {}),

  // Treatment Plan
  getTreatmentPlan: (encounterId: string) =>
    medicalApi.get<TreatmentPlanResponse>(`/hce/encounters/${encounterId}/treatment-plan`),

  createTreatmentPlan: (encounterId: string, data: TreatmentPlanFormData) =>
    medicalApi.post<TreatmentPlanResponse>(`/hce/encounters/${encounterId}/treatment-plan`, data),

  updateTreatmentPlan: (encounterId: string, data: TreatmentPlanFormData) =>
    medicalApi.put<TreatmentPlanResponse>(`/hce/encounters/${encounterId}/treatment-plan`, data),

  // Medical Orders
  getMedicalOrders: (encounterId: string) =>
    medicalApi.get<MedicalOrderResponse[]>(`/hce/encounters/${encounterId}/orders`),

  createMedicalOrder: (encounterId: string, data: MedicalOrderFormData) =>
    medicalApi.post<MedicalOrderResponse>(`/hce/encounters/${encounterId}/orders`, data),

  updateMedicalOrder: (orderId: string, data: Partial<MedicalOrderFormData>) =>
    medicalApi.put<MedicalOrderResponse>(`/hce/orders/${orderId}`, data),

  deleteMedicalOrder: (orderId: string) =>
    medicalApi.delete(`/hce/orders/${orderId}`),

  // Clinical History
  getPatientHistory: (patientId: string) =>
    medicalApi.get<HistoryResponse>(`/hce/patients/${patientId}/history`),

  createHistoryItem: (patientId: string, data: HistoryFormData) =>
    medicalApi.post<HistoryResponse>(`/hce/patients/${patientId}/history`, data),

  updateHistoryItem: (historyId: string, data: Partial<HistoryFormData>) =>
    medicalApi.put<HistoryResponse>(`/hce/history/${historyId}`, data),

  deleteHistoryItem: (historyId: string) =>
    medicalApi.delete(`/hce/history/${historyId}`),
};

export type {
  EncounterFormData,
  EncounterUpdateData,
  PatientIdentificationFormData,
  AnamnesisFormData,
  PhysicalExamFormData,
  DiagnosisFormData,
  TreatmentPlanFormData,
  MedicalOrderFormData,
  HistoryFormData,
};