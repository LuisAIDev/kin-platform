import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ClosingStep } from './ClosingStep';
import { Shield, AlertCircle, CheckCircle2, XCircle, Shield as ShieldIcon } from 'lucide-react';

const mockOnCloseEncounter = vi.fn().mockResolvedValue(undefined);
const mockRouterPush = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockRouterPush }),
}));

const defaultProps = {
  encounterId: '123e4567-e89b-12d3-a456-426614174000',
  onSave: vi.fn(),
  isDirty: false,
  canClose: true,
  onCloseEncounter: mockOnCloseEncounter,
  patientData: {
    fullName: 'Juan Pérez',
    documentNumber: '1234567890',
    eps: 'Sanitas',
  },
  encounterData: {
    chiefComplaint: 'Dolor abdominal',
    encounterType: 'OUTPATIENT',
  },
  diagnosisData: {
    principal: {
      cie10Code: 'K59.0',
      cie10Description: 'Constipación',
    },
    secondary: [],
  },
  treatmentPlanData: {
    conduct: 'OUTPATIENT_TREATMENT',
    followupPlan: 'Control en 7 días',
  },
  physicalExamData: {
    bpSystolic: 120,
    bpDiastolic: 80,
    heartRate: 72,
    temperature: 36.5,
    spo2: 98,
    weightKg: 70,
    heightCm: 175,
    bmi: 22.9,
  },
  ordersCount: 2,
  historyCount: {
    allergies: 1,
    medications: 2,
  },
};

beforeEach(() => {
  vi.clearAllMocks();
  mockOnCloseEncounter.mockResolvedValue(undefined);
  mockRouterPush.mockReset();
});

describe('ClosingStep', () => {
  it('renders validation success when all requirements met', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('Todos los requisitos cumplidos')).toBeInTheDocument();
  });

  it('renders validation errors when missing principal diagnosis', () => {
    render(<ClosingStep {...defaultProps} diagnosisData={{ principal: null, secondary: [] }} />);
    expect(screen.getByText('Faltan requisitos para cerrar')).toBeInTheDocument();
    expect(screen.getByText('Falta diagnóstico PRINCIPAL')).toBeInTheDocument();
  });

  it('renders validation errors when missing treatment plan', () => {
    render(<ClosingStep {...defaultProps} treatmentPlanData={undefined} />);
    expect(screen.getByText('Faltan requisitos para cerrar')).toBeInTheDocument();
    expect(screen.getByText('Falta plan de manejo')).toBeInTheDocument();
  });

  it('shows patient identification section', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('Juan Pérez')).toBeInTheDocument();
    expect(screen.getByText('1234567890')).toBeInTheDocument();
    expect(screen.getByText('Sanitas')).toBeInTheDocument();
  });

  it('shows encounter motive section', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('Dolor abdominal')).toBeInTheDocument();
    expect(screen.getByText('OUTPATIENT')).toBeInTheDocument();
  });

  it('shows principal diagnosis with star icon', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('★ Principal')).toBeInTheDocument();
    expect(screen.getByText('K59.0 - Constipación')).toBeInTheDocument();
  });

  it('shows treatment plan section', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('OUTPATIENT_TREATMENT')).toBeInTheDocument();
    expect(screen.getByText('Control en 7 días')).toBeInTheDocument();
  });

  it('shows physical exam vital signs', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('120/80 mmHg')).toBeInTheDocument();
    expect(screen.getByText('72 lpm')).toBeInTheDocument();
    expect(screen.getByText('36.5°C')).toBeInTheDocument();
    expect(screen.getByText('98%')).toBeInTheDocument();
  });

  it('shows orders count', () => {
    render(<ClosingStep {...defaultProps} />);
    expect(screen.getByText('2 órdenes registradas')).toBeInTheDocument();
  });

  it('disables close button when validation fails', () => {
    render(<ClosingStep {...defaultProps} diagnosisData={{ principal: null, secondary: [] }} />);
    const closeButton = screen.getByRole('button', { name: /cerrar y firmar consulta/i });
    expect(closeButton).toBeDisabled();
  });

  it('enables close button when validation passes', () => {
    render(<ClosingStep {...defaultProps} />);
    const closeButton = screen.getByRole('button', { name: /cerrar y firmar consulta/i });
    expect(closeButton).not.toBeDisabled();
  });

  it('opens confirmation modal when clicking close button', async () => {
    render(<ClosingStep {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: /cerrar y firmar consulta/i }));
    await waitFor(() => {
      expect(screen.getByText('Confirmar Cierre de Consulta')).toBeInTheDocument();
    });
  });

  it('calls onCloseEncounter when confirming close', async () => {
    render(<ClosingStep {...defaultProps} />);
    fireEvent.click(screen.getByRole('button', { name: /cerrar y firmar consulta/i }));
    await waitFor(() => {
      fireEvent.click(screen.getByRole('button', { name: /confirmar y firmar/i }));
    });
    await waitFor(() => {
      expect(mockOnCloseEncounter).toHaveBeenCalledTimes(1);
    });
  });

  it('shows warning when cannot close', () => {
    render(<ClosingStep {...defaultProps} canClose={false} />);
    expect(screen.getByText('Completa los pasos anteriores')).toBeInTheDocument();
  });
});