export type WizardStepId =
  | 'identification'
  | 'motive'
  | 'illness'
  | 'history'
  | 'physicalExam'
  | 'diagnosisPlan'
  | 'closing';

export interface WizardStep {
  id: WizardStepId;
  label: string;
  description: string;
  icon: string; // lucide icon name
  required: boolean;
}

export const WIZARD_STEPS: WizardStep[] = [
  {
    id: 'identification',
    label: 'Identificación',
    description: 'Datos del paciente',
    icon: 'User',
    required: true,
  },
  {
    id: 'motive',
    label: 'Motivo',
    description: 'Motivo de consulta',
    icon: 'MessageSquare',
    required: true,
  },
  {
    id: 'illness',
    label: 'Enf. Actual',
    description: 'Enfermedad actual',
    icon: 'Activity',
    required: false,
  },
  {
    id: 'history',
    label: 'Antecedentes',
    description: 'Historia clínica',
    icon: 'BookOpen',
    required: false,
  },
  {
    id: 'physicalExam',
    label: 'Examen Físico',
    description: 'Signos vitales y sistemas',
    icon: 'Stethoscope',
    required: false,
  },
  {
    id: 'diagnosisPlan',
    label: 'Dx y Plan',
    description: 'Diagnóstico y manejo',
    icon: 'ClipboardList',
    required: true,
  },
  {
    id: 'closing',
    label: 'Cierre',
    description: 'Finalizar consulta',
    icon: 'CheckCircle',
    required: true,
  },
];

export const STEP_ORDER: WizardStepId[] = WIZARD_STEPS.map((s) => s.id);