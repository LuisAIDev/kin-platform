// Barrel de schemas HCE.
//
// Los schemas "base" (contrato de API/servicio) se re-exportan completos. Los
// schemas de "step" (asistente clínico) reutilizan nombres de los base pero con
// forma distinta; re-exportarlos con `export *` producía colisiones (TS2308).
// Por eso aquí se re-exportan solo sus símbolos únicos; los componentes importan
// el resto directamente desde su archivo de step correspondiente.
export * from './encounter.schema';
export * from './anamnesis.schema';
export * from './physicalExam.schema';
export * from './history.schema';

// illnessStep (sistemas/illness). `systemsReviewSchema`/`SystemsReviewData`
// colisionan con anamnesis, por lo que no se re-exportan aquí.
export {
  systemsReviewItemSchema,
  illnessStepSchema,
  validateOnsetDatetime,
} from './illnessStep.schema';
export type { SystemsReviewItem, IllnessStepFormData } from './illnessStep.schema';

// historyStep. Los `*Schema`/`*FormData` de ítems colisionan con history.
export {
  historyTypeSchema,
  historyItemSchema,
  createHistoryItemSchema,
  getHistoryTypeLabel,
  getHistoryTypeIcon,
} from './historyStep.schema';
export type { HistoryType, HistoryItemFormData } from './historyStep.schema';

// physicalExamStep. `physicalExamSchema`/`PhysicalExamFormData`/`calculateBMI`
// colisionan con physicalExam.
export { validateVitals } from './physicalExamStep.schema';

// diagnosisPlanStep. `diagnosisSchema`/`treatmentPlanSchema`/`medicalOrderSchema`
// y sus enums colisionan con encounter.
export {
  cie10CodeSchema,
  conductSchema,
  prognosisSchema,
  orderTypeSchema,
  orderPrioritySchema,
  diagnosisPlanSchema,
  getDiagnosisTypeLabel,
  getCertaintyLabel,
  getConductLabel,
  getPrognosisLabel,
  getOrderTypeLabel,
  getOrderPriorityLabel,
} from './diagnosisPlanStep.schema';
export type { DiagnosisPlanFormData } from './diagnosisPlanStep.schema';

// closingStep.
export {
  closingStepSchema,
  validateClosingRequirements,
  getMissingRequirements,
} from './closingStep.schema';
export type { ClosingStepFormData } from './closingStep.schema';
