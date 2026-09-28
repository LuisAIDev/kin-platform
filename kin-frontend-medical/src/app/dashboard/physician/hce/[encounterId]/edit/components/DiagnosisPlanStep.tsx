'use client';

import { useForm, useFieldArray, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useCallback, useEffect } from 'react';
import {
  Plus, Trash2, X, ChevronDown, ChevronUp, Radio,
  Pill, Scissors, FlaskConical, Image, Utensils, HeartPulse, MoreHorizontal,
  Save, Loader2, AlertCircle, Flag, ClipboardList,
  BookOpen, Stethoscope, Syringe,
} from 'lucide-react';
import type { FC } from 'react';
import {
  diagnosisSchema, treatmentPlanSchema, medicalOrderSchema, diagnosisPlanSchema,
  diagnosisTypeSchema, certaintySchema, conductSchema, prognosisSchema,
  orderTypeSchema, orderPrioritySchema,
  getDiagnosisTypeLabel, getCertaintyLabel, getConductLabel, getPrognosisLabel,
  getOrderTypeLabel, getOrderPriorityLabel,
  type DiagnosisFormData, type TreatmentPlanFormData, type MedicalOrderFormData,
} from '@/lib/hce/schemas/diagnosisPlanStep.schema';
import { useDiagnoses, useTreatmentPlan, useMedicalOrders } from '@/lib/hce/hooks';

const DIAGNOSIS_TYPES = [
  { value: 'PRINCIPAL', label: 'Principal' },
  { value: 'SECUNDARIO', label: 'Secundario' },
  { value: 'COMORBILIDAD', label: 'Comorbilidad' },
  { value: 'COMPLICACION', label: 'Complicación' },
  { value: 'INGRESO', label: 'Al Ingreso' },
  { value: 'EGRESO', label: 'Al Egreso' },
] as const;

const CERTAINTY_TYPES = [
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'PRESUMPTIVE', label: 'Presuntivo' },
  { value: 'RULED_OUT', label: 'Descartado' },
  { value: 'WORKING', label: 'En Estudio' },
] as const;

const CONDUCT_TYPES = [
  { value: 'OBSERVATION', label: 'Observación' },
  { value: 'OUTPATIENT_TREATMENT', label: 'Tratamiento Ambulatorio' },
  { value: 'REFERRAL', label: 'Referencia' },
  { value: 'HOSPITALIZATION', label: 'Hospitalización' },
  { value: 'SURGERY', label: 'Cirugía' },
  { value: 'PALLIATIVE', label: 'Paliativo' },
  { value: 'REHABILITATION', label: 'Rehabilitación' },
] as const;

const PROGNOSIS_TYPES = [
  { value: 'EXCELLENT', label: 'Excelente' },
  { value: 'GOOD', label: 'Bueno' },
  { value: 'FAIR', label: 'Regular' },
  { value: 'POOR', label: 'Malo' },
  { value: 'GUARDED', label: 'Reservado' },
  { value: 'UNKNOWN', label: 'Desconocido' },
] as const;

const ORDER_TYPES = [
  { value: 'MEDICATION', label: 'Medicamento', icon: Pill },
  { value: 'PROCEDURE', label: 'Procedimiento', icon: Scissors },
  { value: 'LAB_EXAM', label: 'Examen Lab', icon: FlaskConical },
  { value: 'IMAGING', label: 'Imagenología', icon: Image },
  { value: 'DIET', label: 'Dieta', icon: Utensils },
  { value: 'NURSING_CARE', label: 'Cuidados Enf.', icon: HeartPulse },
  { value: 'OTHER', label: 'Otro', icon: MoreHorizontal },
] as const;

const ORDER_PRIORITIES = [
  { value: 'STAT', label: 'STAT', color: 'bg-red-100 text-red-700' },
  { value: 'URGENT', label: 'Urgente', color: 'bg-orange-100 text-orange-700' },
  { value: 'ROUTINE', label: 'Rutina', color: 'bg-blue-100 text-blue-700' },
  { value: 'SCHEDULED', label: 'Programado', color: 'bg-green-100 text-green-700' },
] as const;

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

interface TherapeuticGoalsFieldArrayProps {
  form: ReturnType<typeof useForm<any>>;
}

function TherapeuticGoalsFieldArray({ form }: TherapeuticGoalsFieldArrayProps) {
  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: 'therapeuticGoals',
  });

  return (
    <div className="space-y-2">
      {fields.map((field, index) => (
        <div key={field.id} className="flex items-center gap-2">
          <input
            {...form.register(`therapeuticGoals.${index}`)}
            placeholder={`Objetivo ${index + 1}`}
            className="flex-1 px-4 py-2 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
          />
          <button
            type="button"
            onClick={() => remove(index)}
            className="p-2 rounded-lg text-neutral-500 hover:bg-red-50 hover:text-red-600"
            aria-label="Eliminar objetivo"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      ))}
      {fields.length < 10 && (
        <button
          type="button"
          onClick={() => append('')}
          className="flex items-center gap-2 text-sm text-medical-600 hover:text-medical-700"
        >
          <Plus className="w-4 h-4" />
          Agregar objetivo
        </button>
      )}
    </div>
  );
}

interface DiagnosisPlanStepProps {
  encounterId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
}

export const DiagnosisPlanStep: FC<DiagnosisPlanStepProps> = ({
  encounterId,
  onSave,
  isDirty,
  className,
}) => {
  const { data: diagnoses, isLoading: diagLoading, refetch: refetchDiag } = useDiagnoses(encounterId);
  const { data: treatmentPlan, isLoading: planLoading } = useTreatmentPlan(encounterId);
  const { data: orders, isLoading: ordersLoading } = useMedicalOrders(encounterId);

  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  const [openDiagnosisModal, setOpenDiagnosisModal] = useState(false);
  const [editingDiagnosis, setEditingDiagnosis] = useState<any>(null);
  const [openOrderModal, setOpenOrderModal] = useState(false);
  const [editingOrder, setEditingOrder] = useState<any>(null);

  const diagnosisForm = useForm<any>({ resolver: zodResolver(diagnosisSchema), mode: 'onChange' });
  const planForm = useForm<any>({ resolver: zodResolver(treatmentPlanSchema), mode: 'onChange' });
  const orderForm = useForm<any>({ resolver: zodResolver(medicalOrderSchema), mode: 'onChange' });

  const principalDiagnosisId = useWatch({
    control: diagnosisForm.control,
    name: 'diagnoses',
  })?.find((d: any) => d.isPrincipal)?.id;

  const onDiagnosisSubmit = async (data: any) => {
    try {
      if (data.isPrincipal) {
        const currentDiagnoses = diagnosisForm.getValues('diagnoses') || [];
        const updated = currentDiagnoses.map((d: any) => ({ ...d, isPrincipal: d.id === data.id }));
        diagnosisForm.setValue('diagnoses', updated);
      }
      setLastSaved(new Date());
      onSave();
    } catch (err) {
      console.error('Error saving diagnosis:', err);
    }
  };

  const onPlanSubmit = async (data: any) => {
    try {
      setLastSaved(new Date());
      onSave();
    } catch (err) {
      console.error('Error saving treatment plan:', err);
    }
  };

  const onOrderSubmit = async (data: any) => {
    try {
      setLastSaved(new Date());
      onSave();
    } catch (err) {
      console.error('Error saving order:', err);
    }
  };

  const handleAutoSave = useCallback(() => {
    if (saveTimer) clearTimeout(saveTimer);
    setSaveTimer(setTimeout(() => {
      setLastSaved(new Date());
    }, 30000));
  }, []);

  useEffect(() => {
    const sub1 = diagnosisForm.watch(() => handleAutoSave());
    const sub2 = planForm.watch(() => handleAutoSave());
    const sub3 = orderForm.watch(() => handleAutoSave());
    return () => { sub1.unsubscribe(); sub2.unsubscribe(); sub3.unsubscribe(); };
  }, [diagnosisForm, planForm, orderForm, handleAutoSave]);

  const handleCloseDiagnosisModal = () => {
    setOpenDiagnosisModal(false);
    setEditingDiagnosis(null);
    diagnosisForm.reset({ encounterId, type: 'SECUNDARIO', certainty: 'WORKING', isPrincipal: false });
  };

  const handleCloseOrderModal = () => {
    setOpenOrderModal(false);
    setEditingOrder(null);
    orderForm.reset({ encounterId, orderType: 'MEDICATION', priority: 'ROUTINE' });
  };

  const setPrincipalDiagnosis = (id: string) => {
    const current = diagnosisForm.getValues('diagnoses') || [];
    const updated = current.map((d: any) => ({ ...d, isPrincipal: d.id === id }));
    diagnosisForm.setValue('diagnoses', updated);
    setLastSaved(new Date());
  };

  const deleteDiagnosis = (id: string) => {
    const current = diagnosisForm.getValues('diagnoses') || [];
    diagnosisForm.setValue('diagnoses', current.filter((d: any) => d.id !== id));
    setLastSaved(new Date());
  };

  const deleteOrder = (id: string) => {
    const current = orderForm.getValues('medicalOrders') || [];
    orderForm.setValue('medicalOrders', current.filter((o: any) => o.id !== id));
    setLastSaved(new Date());
  };

  const handleSaveAll = () => {
    setLastSaved(new Date());
    onSave();
  };

  if (false) {
    return null; // placeholder to satisfy TS
  }

  return (
    <div className={`space-y-6 ${className || ''}`}>
      {/* Diagnoses Section */}
      <CollapsibleSection title="Diagnósticos" defaultOpen icon={<ClipboardList className="w-5 h-5" />}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-md font-medium text-neutral-700">
            Diagnósticos
          </h3>
          <button
            type="button"
            onClick={() => { diagnosisForm.reset({ encounterId, type: 'SECUNDARIO', certainty: 'WORKING', isPrincipal: false }); setOpenDiagnosisModal(true); }}
            className="flex items-center gap-2 rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700"
          >
            <Plus className="w-4 h-4" /> Agregar Diagnóstico
          </button>
        </div>

        <div className="rounded-xl border border-neutral-200 bg-white divide-y divide-neutral-100">
          {/* Placeholder for diagnosis list */}
          <div className="p-4 text-center text-neutral-500">
            Lista de diagnósticos (placeholder)
          </div>
        </div>

        {/* Diagnosis Modal */}
        <DiagnosisModal
          form={diagnosisForm}
          onSubmit={() => {}}
          onClose={() => setOpenDiagnosisModal(false)}
          isSubmitting={false}
          editingDiagnosis={null}
          existingDiagnoses={[]}
        />
      </CollapsibleSection>

      {/* Treatment Plan Section */}
      <CollapsibleSection title="Plan de Manejo" defaultOpen icon={<BookOpen className="w-5 h-5" />}>
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Conducta *</label>
              <select
                {...planForm.register('conduct')}
                className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
              >
                {CONDUCT_TYPES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Pronóstico *</label>
              <select
                {...planForm.register('prognosis')}
                className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
              >
                {PROGNOSIS_TYPES.map((p) => (
                  <option key={p.value} value={p.value}>{p.label}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Objetivos Terapéuticos</label>
            <TherapeuticGoalsFieldArray form={planForm} />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Plan de Seguimiento</label>
            <textarea
              {...planForm.register('followupPlan')}
              rows={3}
              placeholder="Describa el plan de seguimiento..."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={2000}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Criterios de Re-evaluación</label>
            <textarea
              {...planForm.register('reevaluationCriteria')}
              rows={2}
              placeholder="Cuándo y por qué re-evaluar..."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={2000}
            />
          </div>
        </div>
      </CollapsibleSection>

      {/* Medical Orders Section */}
      <CollapsibleSection title="Órdenes Médicas" defaultOpen icon={<Stethoscope className="w-5 h-5" />}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-md font-medium text-neutral-700">Órdenes Médicas</h3>
          <button
            type="button"
            onClick={() => { orderForm.reset({ encounterId, orderType: 'MEDICATION', priority: 'ROUTINE' }); setOpenOrderModal(true); }}
            className="flex items-center gap-2 rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700"
          >
            <Plus className="w-4 h-4" /> Agregar Orden
          </button>
        </div>

        <div className="rounded-xl border border-neutral-200 bg-white divide-y divide-neutral-100">
          {/* Placeholder for orders list */}
          <div className="p-4 text-center text-neutral-500">
            Lista de órdenes (placeholder)
          </div>
        </div>

        {/* Order Modal */}
        <OrderModal
          form={orderForm}
          onSubmit={() => {}}
          onClose={() => setOpenOrderModal(false)}
          isSubmitting={false}
          editingOrder={null}
        />
      </CollapsibleSection>

      <div className="flex items-center justify-between pt-4">
        <div className="flex items-center gap-2 text-sm text-neutral-500">
          <span>Placeholder - Save functionality</span>
        </div>
        <button
          type="button"
          onClick={() => setLastSaved(new Date())}
          className="flex items-center gap-2 rounded-lg bg-medical-600 px-6 py-3 text-sm font-medium text-white hover:bg-medical-700"
        >
          <Save className="w-4 h-4" />
          Guardar Todo
        </button>
      </div>
    </div>
  );
};

// Placeholder modal components
function DiagnosisModal({ form, onSubmit, onClose, isSubmitting, editingDiagnosis, existingDiagnoses }: {
  form: ReturnType<typeof useForm<any>>;
  onSubmit: (data: any) => void;
  onClose: () => void;
  isSubmitting: boolean;
  editingDiagnosis: any;
  existingDiagnoses: any[];
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="diag-modal-title">
      <div className="w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-2xl bg-white shadow-xl">
        <div className="sticky top-0 flex items-center justify-between border-b border-neutral-200 bg-white p-4 rounded-t-2xl">
          <h2 id="diag-modal-title" className="text-lg font-semibold text-neutral-800">Nuevo Diagnóstico</h2>
          <button onClick={onClose} className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100" aria-label="Cerrar">
            <X className="w-5 h-5" />
          </button>
        </div>
        <form onSubmit={form.handleSubmit(onSubmit)} className="p-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Código CIE-10 *</label>
            <input {...form.register('cie10Code')} placeholder="K59.0, J18.9..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Descripción CIE-10</label>
            <input {...form.register('cie10Description')} placeholder="Constipación, Neumonía..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Tipo</label>
              <select {...form.register('type')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
                {DIAGNOSIS_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Certeza</label>
              <select {...form.register('certainty')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
                {CERTAINTY_TYPES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <input {...form.register('isPrincipal')} type="checkbox" id="isPrincipal" className="h-4 w-4 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500" />
            <label htmlFor="isPrincipal" className="text-sm text-neutral-700">Diagnóstico Principal (solo uno)</label>
          </div>
          <div className="sticky bottom-0 border-t border-neutral-200 bg-white p-4 rounded-b-2xl flex justify-end gap-3">
            <button type="button" onClick={onClose} className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50">
              <X className="w-4 h-4" /> Cancelar
            </button>
            <button type="submit" disabled={false} className="flex items-center gap-2 rounded-lg bg-medical-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-medical-700">
              Guardar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function OrderModal({ form, onSubmit, onClose, isSubmitting, editingOrder }: {
  form: ReturnType<typeof useForm<any>>;
  onSubmit: (data: any) => void;
  onClose: () => void;
  isSubmitting: boolean;
  editingOrder: any;
}) {
  const orderType = form.watch('orderType');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="order-modal-title">
      <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white shadow-xl">
        <div className="sticky top-0 flex items-center justify-between border-b border-neutral-200 bg-white p-4 rounded-t-2xl">
          <h2 id="order-modal-title" className="text-lg font-semibold text-neutral-800">Nueva Orden Médica</h2>
          <button onClick={onClose} className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100" aria-label="Cerrar">
            <X className="w-5 h-5" />
          </button>
        </div>
        <form onSubmit={form.handleSubmit(onSubmit)} className="p-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Tipo de Orden *</label>
            <div className="flex flex-wrap gap-2">
              {ORDER_TYPES.map((t) => (
                <label key={t.value} className={`flex items-center gap-2 px-3 py-2 rounded-lg border-2 cursor-pointer transition ${form.watch('orderType') === t.value ? 'border-medical-600 bg-medical-50' : 'border-neutral-200 hover:border-medical-300'}`}>
                  <input type="radio" value={t.value} {...form.register('orderType')} className="h-4 w-4 text-medical-600 focus:ring-medical-500" />
                  <t.icon className="w-4 h-4 text-medical-600" />
                  <span className="text-sm font-medium">{t.label}</span>
                </label>
              ))}
            </div>
          </div>

          {['MEDICATION'].includes(orderType) && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Medicamento *</label>
                <input {...form.register('drugName')} placeholder="Paracetamol, Ibuprofeno..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Dosis</label>
                <input {...form.register('dose')} placeholder="500" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Unidad</label>
                <input {...form.register('doseUnit')} placeholder="mg, ml, UI..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Vía</label>
                <select {...form.register('route')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
                  <option value="">Seleccionar</option>
                  <option value="ORAL">Oral</option>
                  <option value="IV">Intravenosa</option>
                  <option value="IM">Intramuscular</option>
                  <option value="SC">Subcutánea</option>
                  <option value="TOPICAL">Tópica</option>
                  <option value="INHALATION">Inhalación</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Frecuencia</label>
                <input {...form.register('frequency')} placeholder="Cada 8 horas" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
              </div>
              <div>
                <label className="block text-sm font-medium text-neutral-700 mb-1">Días</label>
                <input {...form.register('durationDays', { valueAsNumber: true })} type="number" min={1} max={365} placeholder="7" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
              </div>
            </div>
          )}

          {['PROCEDURE', 'LAB_EXAM', 'IMAGING'].includes(orderType) && (
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Código CUPS *</label>
              <input {...form.register('cupsCode')} placeholder="890201, 900301..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Instrucciones</label>
            <textarea {...form.register('instructions')} rows={3} placeholder="Instrucciones para el paciente o personal..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none" maxLength={1000} />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Prioridad</label>
              <select {...form.register('priority')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
                {ORDER_PRIORITIES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </div>
          </div>

          <div className="sticky bottom-0 border-t border-neutral-200 bg-white p-4 rounded-b-2xl flex justify-end gap-3">
            <button type="button" onClick={onClose} className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50">
              <X className="w-4 h-4" /> Cancelar
            </button>
            <button type="submit" disabled={false} className="flex items-center gap-2 rounded-lg bg-medical-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-medical-700">
              Guardar
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}