'use client';

import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useCallback, useEffect } from 'react';
import {
  CheckCircle2, XCircle, AlertCircle, Save, Loader2,
  ClipboardCheck, FileText, ArrowLeft, Shield,
  Stethoscope, ClipboardList, BookOpen, Pill, CheckCircle,
  ChevronUp, ChevronDown, X,
} from 'lucide-react';
import type { FC } from 'react';
import { useRouter } from 'next/navigation';
import {
  closingStepSchema,
  type ClosingStepFormData,
  validateClosingRequirements,
  getMissingRequirements,
} from '@/lib/hce/schemas/closingStep.schema';

interface CollapsibleSectionProps {
  title: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
  icon?: React.ReactNode;
  className?: string;
}

function CollapsibleSection({ title, children, defaultOpen = false, icon, className }: CollapsibleSectionProps) {
  const [isOpen, setIsOpen] = useState(defaultOpen);
  return (
    <div className={`rounded-xl border border-neutral-200 bg-white overflow-hidden ${className || ''}`}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between gap-3 p-4 hover:bg-neutral-50 transition-colors"
        aria-expanded={isOpen}
      >
        <div className="flex items-center gap-2">
          {icon && <span className="text-medical-600">{icon}</span>}
          <h3 className="text-lg font-medium text-neutral-800">{title}</h3>
        </div>
        {isOpen ? <ChevronUp className="w-5 h-5 text-neutral-500" /> : <ChevronDown className="w-5 h-5 text-neutral-500" />}
      </button>
      <div className={`overflow-hidden transition-all duration-200 ${isOpen ? 'max-h-[2000px] opacity-100' : 'max-h-0 opacity-0'}`}>
        <div className="px-4 pb-4 border-t border-neutral-100">{children}</div>
      </div>
    </div>
  );
}

interface ClosingStepProps {
  encounterId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
  patientData?: {
    fullName: string;
    documentNumber: string;
    eps: string;
  };
  encounterData?: {
    chiefComplaint: string;
    encounterType: string;
  };
  anamnesisData?: {
    evolutionDescription: string;
    severitySelfReported: number;
  };
  historyCount?: Record<string, number>;
  physicalExamData?: {
    bpSystolic: number | null;
    bpDiastolic: number | null;
    heartRate: number | null;
    temperature: number | null;
    spo2: number | null;
    weightKg: number | null;
    heightCm: number | null;
    bmi: number | null;
  };
  diagnosisData?: {
    principal: { cie10Code: string; cie10Description: string } | null;
    secondary: Array<{ cie10Code: string; cie10Description: string }>;
  };
  treatmentPlanData?: {
    conduct: string;
    followupPlan: string;
  };
  ordersCount?: number;
  canClose?: boolean;
  onCloseEncounter: () => Promise<void>;
}

export const ClosingStep: FC<ClosingStepProps> = ({
  encounterId,
  onSave,
  isDirty,
  className,
  patientData,
  encounterData,
  anamnesisData,
  historyCount,
  physicalExamData,
  diagnosisData,
  treatmentPlanData,
  ordersCount = 0,
  canClose = false,
  onCloseEncounter,
}) => {
  const router = useRouter();
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [saveTimer, setSaveTimer] = useState<ReturnType<typeof setTimeout> | null>(null);
  const [isClosing, setIsClosing] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const form = useForm<ClosingStepFormData>({
    resolver: zodResolver(closingStepSchema),
    defaultValues: {
      encounterId,
      hasPrincipalDiagnosis: false,
      hasTreatmentPlan: false,
      isEncounterOpen: true,
      confirmation: false,
    },
  });

  const handleAutoSave = useCallback(() => {
    if (saveTimer) clearTimeout(saveTimer);
    setSaveTimer(setTimeout(() => {
      setLastSaved(new Date());
    }, 30000));
  }, []);

  useEffect(() => {
    const subscription = form.watch(() => handleAutoSave());
    return () => subscription.unsubscribe();
  }, [form, handleAutoSave]);

  useEffect(() => {
    form.setValue('hasPrincipalDiagnosis', !!diagnosisData?.principal, { shouldValidate: true });
    form.setValue('hasTreatmentPlan', !!treatmentPlanData?.conduct, { shouldValidate: true });
    form.setValue('isEncounterOpen', true, { shouldValidate: true });
  }, [diagnosisData?.principal, treatmentPlanData?.conduct, form]);

  const validation = validateClosingRequirements(
    !!diagnosisData?.principal,
    !!treatmentPlanData?.conduct,
    true
  );

  const missingRequirements = getMissingRequirements(
    !!diagnosisData?.principal,
    !!treatmentPlanData?.conduct,
    true
  );

  const onSubmit = async (data: ClosingStepFormData) => {
    if (!validation.valid) {
      return;
    }
    setIsClosing(true);
    try {
      await onCloseEncounter();
      setLastSaved(new Date());
      router.push(`/dashboard/physician/hce/${encounterId}`);
    } catch (err) {
      console.error('Error closing encounter:', err);
    } finally {
      setIsClosing(false);
      setShowConfirmModal(false);
    }
  };

  const handleConfirmClose = () => {
    setShowConfirmModal(true);
  };

  const handleCancelClose = () => {
    setShowConfirmModal(false);
  };

  if (!canClose) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="status">
        <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center">
          <Shield className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-neutral-800 mb-2">Completa los pasos anteriores</h3>
          <p className="text-neutral-500">Debes completar al menos los pasos obligatorios antes de cerrar la consulta.</p>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className={`space-y-6 ${className || ''}`} noValidate>
      {/* Validation Status */}
      <div className={`rounded-xl p-4 ${validation.valid ? 'bg-emerald-50 border border-emerald-200' : 'bg-red-50 border border-red-200'}`}>
        <div className="flex items-start gap-3">
          {validation.valid ? (
            <CheckCircle2 className="w-5 h-5 text-emerald-600 mt-0.5 flex-shrink-0" />
          ) : (
            <AlertCircle className="w-5 h-5 text-red-600 mt-0.5 flex-shrink-0" />
          )}
          <div className="flex-1">
            <h3 className={`${validation.valid ? 'text-emerald-800' : 'text-red-800'} font-semibold`}>
              {validation.valid ? 'Todos los requisitos cumplidos' : 'Faltan requisitos para cerrar'}
            </h3>
            {!validation.valid && (
              <ul className="mt-2 space-y-1 text-sm text-red-700">
                {missingRequirements.map((req, idx) => (
                  <li key={idx} className="flex items-center gap-2">
                    <XCircle className="w-4 h-4 flex-shrink-0" />
                    Falta {req}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>

      {/* Summary Sections */}
      <div className="space-y-4">
        {patientData && (
          <CollapsibleSection title="Identificación del Paciente" defaultOpen icon={<Shield className="w-5 h-5" />}>
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
              <div><dt className="text-neutral-500">Nombre</dt><dd className="font-medium">{patientData.fullName}</dd></div>
              <div><dt className="text-neutral-500">Documento</dt><dd className="font-medium">{patientData.documentNumber}</dd></div>
              <div><dt className="text-neutral-500">EPS</dt><dd className="font-medium">{patientData.eps || '—'}</dd></div>
            </dl>
          </CollapsibleSection>
        )}

        {encounterData && (
          <CollapsibleSection title="Motivo de Consulta" defaultOpen icon={<Stethoscope className="w-5 h-5" />}>
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
              <div><dt className="text-neutral-500">Motivo</dt><dd className="font-medium">{encounterData.chiefComplaint}</dd></div>
              <div><dt className="text-neutral-500">Tipo</dt><dd className="font-medium">{encounterData.encounterType}</dd></div>
            </dl>
          </CollapsibleSection>
        )}

        {anamnesisData && (
          <CollapsibleSection title="Enfermedad Actual" defaultOpen icon={<ClipboardList className="w-5 h-5" />}>
            <dl className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
              <div className="md:col-span-2"><dt className="text-neutral-500">Evolución</dt><dd className="font-medium">{anamnesisData.evolutionDescription || '—'}</dd></div>
              <div><dt className="text-neutral-500">Severidad (1-10)</dt><dd className="font-medium">{anamnesisData.severitySelfReported || '—'}</dd></div>
            </dl>
          </CollapsibleSection>
        )}

        {historyCount && (
          <CollapsibleSection title="Antecedentes" defaultOpen icon={<BookOpen className="w-5 h-5" />}>
            <dl className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
              {Object.entries(historyCount).map(([type, count]) => (
                <div key={type} className="col-span-1"><dt className="text-neutral-500 capitalize">{type}</dt><dd className="font-medium text-medical-600">{count}</dd></div>
              ))}
            </dl>
          </CollapsibleSection>
        )}

        {physicalExamData && (
          <CollapsibleSection title="Examen Físico - Signos Vitales" defaultOpen icon={<Shield className="w-5 h-5" />}>
            <dl className="grid grid-cols-1 md:grid-cols-3 gap-3 text-sm">
              <div><dt className="text-neutral-500">PA</dt><dd className="font-medium">{physicalExamData.bpSystolic && physicalExamData.bpDiastolic ? `${physicalExamData.bpSystolic}/${physicalExamData.bpDiastolic} mmHg` : '—'}</dd></div>
              <div><dt className="text-neutral-500">FC</dt><dd className="font-medium">{physicalExamData.heartRate ? `${physicalExamData.heartRate} lpm` : '—'}</dd></div>
              <div><dt className="text-neutral-500">FR</dt><dd className="font-medium">{physicalExamData.temperature ? `${physicalExamData.temperature}°C` : '—'}</dd></div>
              <div><dt className="text-neutral-500">SpO2</dt><dd className="font-medium">{physicalExamData.spo2 ? `${physicalExamData.spo2}%` : '—'}</dd></div>
              <div><dt className="text-neutral-500">Peso</dt><dd className="font-medium">{physicalExamData.weightKg ? `${physicalExamData.weightKg} kg` : '—'}</dd></div>
              <div><dt className="text-neutral-500">IMC</dt><dd className="font-medium">{physicalExamData.bmi ? `${physicalExamData.bmi} kg/m²` : '—'}</dd></div>
            </dl>
          </CollapsibleSection>
        )}

        {diagnosisData && (
          <CollapsibleSection title="Diagnósticos" defaultOpen icon={<ClipboardCheck className="w-5 h-5" />}>
            <dl className="space-y-2 text-sm">
              {diagnosisData.principal && (
                <div className="bg-amber-50 rounded-lg p-3 border border-amber-200">
                  <dt className="text-amber-700 font-medium flex items-center gap-2">★ Principal</dt>
                  <dd className="font-medium mt-1">{diagnosisData.principal.cie10Code} - {diagnosisData.principal.cie10Description}</dd>
                </div>
              )}
              {diagnosisData.secondary?.length && (
                <div>
                  <dt className="text-neutral-500 font-medium">Secundarios ({diagnosisData.secondary.length})</dt>
                  <dd className="mt-1 space-y-1">
                    {diagnosisData.secondary.map((d, i) => (
                      <div key={i} className="text-neutral-700">{d.cie10Code} - {d.cie10Description}</div>
                    ))}
                  </dd>
                </div>
              )}
            </dl>
          </CollapsibleSection>
        )}

        {treatmentPlanData && (
          <CollapsibleSection title="Plan de Manejo" defaultOpen icon={<BookOpen className="w-5 h-5" />}>
            <dl className="space-y-2 text-sm">
              <div><dt className="text-neutral-500">Conducta</dt><dd className="font-medium">{treatmentPlanData.conduct}</dd></div>
              <div><dt className="text-neutral-500">Seguimiento</dt><dd className="font-medium">{treatmentPlanData.followupPlan || '—'}</dd></div>
            </dl>
          </CollapsibleSection>
        )}

        {ordersCount > 0 && (
          <CollapsibleSection title="Órdenes Médicas" defaultOpen icon={<Pill className="w-5 h-5" />}>
            <p className="text-sm text-neutral-600">{ordersCount} {ordersCount === 1 ? 'orden registrada' : 'órdenes registradas'}</p>
          </CollapsibleSection>
        )}
      </div>

      {/* Confirmation */}
      <div className="rounded-xl border border-neutral-200 bg-white p-6">
        <div className="flex items-center gap-3 mb-4">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              {...form.register('confirmation')}
              type="checkbox"
              className="h-5 w-5 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500"
            />
            <span className="text-sm text-neutral-700">Confirmo que la información es correcta y deseo cerrar la consulta</span>
          </label>
        </div>
        {form.formState.errors.confirmation && (
          <p className="text-sm text-red-600 mb-4">{form.formState.errors.confirmation.message}</p>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between pt-4">
        <div className="flex items-center gap-2 text-sm text-neutral-500">
          {lastSaved && (
            <>
              <Save className="w-4 h-4" />
              <span>Último guardado: {lastSaved.toLocaleTimeString()}</span>
            </>
          )}
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => router.push(`/dashboard/physician/hce/${encounterId}/edit`)}
            disabled={isClosing}
            className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-40"
          >
            <ArrowLeft className="w-4 h-4" />
            Volver al Wizard
          </button>

          <button
            type="button"
            onClick={handleConfirmClose}
            disabled={!validation.valid || isClosing}
            className="flex items-center gap-2 rounded-lg bg-red-600 px-6 py-3 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-40"
          >
            <Shield className="w-4 h-4" />
            Cerrar y Firmar Consulta
          </button>
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="close-modal-title">
          <div className="w-full max-w-md rounded-2xl bg-white shadow-xl">
            <div className="flex items-center justify-between border-b border-neutral-200 bg-white p-4 rounded-t-2xl">
              <h2 id="close-modal-title" className="text-lg font-semibold text-neutral-800">Confirmar Cierre de Consulta</h2>
              <button onClick={handleCancelClose} className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100" aria-label="Cerrar">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="flex items-center gap-3 text-red-600 bg-red-50 rounded-lg p-4">
                <AlertCircle className="w-6 h-6 flex-shrink-0" />
                <div>
                  <p className="font-semibold text-red-800">Esta acción es irreversible</p>
                  <p className="text-sm text-red-700 mt-1">El encuentro se marcará como CERRADO y no podrá editarse.</p>
                </div>
              </div>
              <div className="space-y-2 text-sm">
                <p><span className="font-medium">Paciente:</span> {patientData?.fullName}</p>
                <p><span className="font-medium">Motivo:</span> {encounterData?.chiefComplaint}</p>
                <p><span className="font-medium">Diagnóstico Principal:</span> {diagnosisData?.principal?.cie10Code} - {diagnosisData?.principal?.cie10Description}</p>
              </div>
            </div>
            <div className="border-t border-neutral-200 p-4 rounded-b-2xl flex justify-end gap-3">
              <button
                onClick={handleCancelClose}
                disabled={isClosing}
                className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-40"
              >
                <X className="w-4 h-4" />
                Cancelar
              </button>

              <button
                onClick={() => onSubmit({} as ClosingStepFormData)}
                disabled={isClosing}
                className="flex items-center gap-2 rounded-lg bg-red-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-40"
              >
                {isClosing ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Cerrando...
                  </>
                ) : (
                  <>
                    <Shield className="w-4 h-4" />
                    Confirmar y Firmar
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </form>
  );
};