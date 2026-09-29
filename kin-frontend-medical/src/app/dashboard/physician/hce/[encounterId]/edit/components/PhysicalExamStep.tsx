'use client';

import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useEffect, useCallback } from 'react';
import { Heart, Activity, Thermometer, Weight, Ruler, Brain, AlertCircle, Save, Loader2, Gauge, ChevronUp, ChevronDown, Clock } from 'lucide-react';
import type { FC } from 'react';
import { physicalExamSchema, type PhysicalExamFormData, calculateBMI } from '@/lib/hce/schemas/physicalExamStep.schema';
import { usePhysicalExam, useUpdatePhysicalExam } from '@/lib/hce/hooks/usePhysicalExam';

const VITAL_FIELDS = [
  { key: 'bpSystolic', label: 'PAS (mmHg)', min: 50, max: 300, icon: Heart, step: 1 },
  { key: 'bpDiastolic', label: 'PAD (mmHg)', min: 30, max: 200, icon: Heart, step: 1 },
  { key: 'heartRate', label: 'FC (lpm)', min: 30, max: 250, icon: Activity, step: 1 },
  { key: 'respiratoryRate', label: 'FR (rpm)', min: 5, max: 80, icon: Activity, step: 1 },
  { key: 'temperature', label: 'Temp (°C)', min: 30, max: 45, icon: Thermometer, step: 0.1 },
  { key: 'spo2', label: 'SpO2 (%)', min: 50, max: 100, icon: Gauge, step: 1 },
  { key: 'weightKg', label: 'Peso (kg)', min: 0.1, max: 500, icon: Weight, step: 0.1 },
  { key: 'heightCm', label: 'Talla (cm)', min: 20, max: 250, icon: Ruler, step: 1 },
  { key: 'glasgowScore', label: 'Glasgow', min: 3, max: 15, icon: Brain, step: 1 },
  { key: 'painScale', label: 'Dolor (0-10)', min: 0, max: 10, icon: AlertCircle, step: 1 },
] as const;

const SYSTEM_FIELDS = [
  { key: 'generalAppearance', label: 'Aspecto General' },
  { key: 'headNeck', label: 'Cabeza y Cuello' },
  { key: 'cardiovascular', label: 'Cardiovascular' },
  { key: 'respiratory', label: 'Respiratorio' },
  { key: 'abdominal', label: 'Abdominal' },
  { key: 'neurological', label: 'Neurológico' },
  { key: 'musculoskeletal', label: 'Musculoesquelético' },
  { key: 'skin', label: 'Piel y Anexos' },
  { key: 'genitourinary', label: 'Genitourinario' },
  { key: 'psychiatric', label: 'Psiquiátrico' },
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

function VitalInput({ key: vitalKey, label, min, max, icon: Icon, step, value, onChange, error, disabled }: {
  key: string;
  label: string;
  min: number;
  max: number;
  icon: React.ComponentType<{ className?: string }>;
  step: number;
  value: number | undefined;
  onChange: (val: number | undefined) => void;
  error?: string;
  disabled?: boolean;
}) {
  const displayValue = value ?? '';
  return (
    <div className="space-y-1">
      <label className="flex items-center gap-1.5 text-sm font-medium text-neutral-700">
        <Icon className="w-4 h-4 text-medical-600" />
        {label}
      </label>
      <input
        type="number"
        value={displayValue}
        onChange={(e) => onChange(e.target.value === '' ? undefined : parseFloat(e.target.value))}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
        className={`w-full px-4 py-3 rounded-lg border focus:outline-none focus:ring-2 transition-colors ${
          error
            ? 'border-red-500 focus:ring-red-500'
            : 'border-neutral-300 focus:ring-medical-500'
        }`}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${vitalKey}-error` : undefined}
      />
      {error && (
        <p id={`${vitalKey}-error`} className="text-sm text-red-600" role="alert">{error}</p>
      )}
    </div>
  );
}

function SystemTextarea({ key: systemKey, label, value, onChange }: {
  key: string;
  label: string;
  value: string | undefined;
  onChange: (val: string | undefined) => void;
}) {
  return (
    <div className="space-y-1">
      <label className="block text-sm font-medium text-neutral-700">{label}</label>
      <textarea
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value || undefined)}
        rows={3}
        placeholder={`Hallazgos en ${label.toLowerCase()}...`}
        className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
        maxLength={1000}
      />
      <p className="text-xs text-neutral-500 text-right">
        {(value ?? '').length} / 1000
      </p>
    </div>
  );
}

interface PhysicalExamStepProps {
  encounterId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
}

export const PhysicalExamStep: FC<PhysicalExamStepProps> = ({
  encounterId,
  onSave,
  isDirty,
  className,
}) => {
  const { data: physicalExam, isLoading, error } = usePhysicalExam(encounterId);
  const updateMutation = useUpdatePhysicalExam(encounterId);

  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  const form = useForm<PhysicalExamFormData>({
    resolver: zodResolver(physicalExamSchema),
    defaultValues: {
      encounterId,
    },
    mode: 'onChange',
  });

  const weightKg = useWatch({ control: form.control, name: 'weightKg' });
  const heightCm = useWatch({ control: form.control, name: 'heightCm' });
  const bmi = calculateBMI(weightKg, heightCm);

  const onSubmit = async (data: PhysicalExamFormData) => {
    try {
      await updateMutation.mutateAsync(data);
      setLastSaved(new Date());
      onSave();
    } catch (err) {
      console.error('Error updating physical exam:', err);
    }
  };

  const handleAutoSave = useCallback((data: PhysicalExamFormData) => {
    if (saveTimer) clearTimeout(saveTimer);
    setSaveTimer(setTimeout(() => {
      updateMutation.mutate(data);
      setLastSaved(new Date());
    }, 30000));
  }, [updateMutation]);

  useEffect(() => {
    const subscription = form.watch((data) => {
      handleAutoSave(data as PhysicalExamFormData);
    });
    return () => subscription.unsubscribe();
  }, [form, handleAutoSave]);

  useEffect(() => {
    if (physicalExam) {
      const initialData = {
        encounterId: physicalExam.encounterId,
        bpSystolic: physicalExam.bpSystolic ?? undefined,
        bpDiastolic: physicalExam.bpDiastolic ?? undefined,
        heartRate: physicalExam.heartRate ?? undefined,
        respiratoryRate: physicalExam.respiratoryRate ?? undefined,
        temperature: physicalExam.temperature ?? undefined,
        spo2: physicalExam.spo2 ?? undefined,
        weightKg: physicalExam.weightKg ?? undefined,
        heightCm: physicalExam.heightCm ?? undefined,
        glasgowScore: physicalExam.glasgowScore ?? undefined,
        painScale: physicalExam.painScale ?? undefined,
        headNeck: physicalExam.headNeck ?? '',
        cardiovascular: physicalExam.cardiovascular ?? '',
        respiratory: physicalExam.respiratory ?? '',
        abdominal: physicalExam.abdominal ?? '',
        neurological: physicalExam.neurological ?? '',
        musculoskeletal: physicalExam.musculoskeletal ?? '',
        skin: physicalExam.skin ?? '',
        genitourinary: physicalExam.genitourinary ?? '',
        psychiatric: physicalExam.psychiatric ?? '',
      };
      form.reset(initialData);
    }
  }, [physicalExam, form]);

  if (isLoading) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="status" aria-label="Cargando examen físico">
        <div className="animate-pulse space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="h-16 bg-neutral-200 rounded" />
            <div className="h-16 bg-neutral-200 rounded" />
            <div className="h-16 bg-neutral-200 rounded" />
          </div>
          <div className="h-32 bg-neutral-200 rounded" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="alert">
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-600" aria-hidden="true" />
            <p className="text-red-800">Error al cargar el examen físico</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className={`space-y-6 ${className || ''}`} noValidate>
      <CollapsibleSection title="Signos Vitales" defaultOpen icon={<Heart className="w-5 h-5" />}>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {VITAL_FIELDS.map((vital) => (
            <VitalInput
              key={vital.key}
              label={vital.label}
              min={vital.min}
              max={vital.max}
              icon={vital.icon}
              step={vital.step}
              value={form.watch(vital.key)}
              onChange={(val) => form.setValue(vital.key, val, { shouldValidate: true })}
              error={form.formState.errors[vital.key]?.message}
            />
          ))}
          <div className="lg:col-span-5 space-y-1">
            <label className="flex items-center gap-1.5 text-sm font-medium text-neutral-700">
              <Gauge className="w-4 h-4 text-medical-600" />
              IMC (calculado)
            </label>
            <div className="w-full px-4 py-3 rounded-lg bg-neutral-50 border border-neutral-200 text-lg font-semibold text-medical-700 flex items-center justify-center">
              {bmi !== undefined ? `${bmi} kg/m²` : '—'}
            </div>
            <p className="text-xs text-neutral-500">Se calcula automáticamente: Peso / (Talla/100)²</p>
          </div>
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Examen por Sistemas" defaultOpen>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {SYSTEM_FIELDS.map((system) => (
            <SystemTextarea
              key={system.key}
              label={system.label}
              value={form.watch(system.key)}
              onChange={(val) => form.setValue(system.key, val, { shouldValidate: true })}
            />
          ))}
        </div>
      </CollapsibleSection>

      <div className="flex items-center justify-between pt-4">
        <div className="flex items-center gap-2 text-sm text-neutral-500">
          {lastSaved && (
            <>
              <Save className="w-4 h-4" aria-hidden="true" />
              <span>Último guardado: {lastSaved.toLocaleTimeString()}</span>
            </>
          )}
          {!lastSaved && isDirty && (
            <>
              <Clock className="w-4 h-4" aria-hidden="true" />
              <span className="text-amber-600">Cambios sin guardar</span>
            </>
          )}
        </div>

        <button
          type="submit"
          disabled={updateMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-medical-600 px-6 py-3 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-40 transition-colors"
        >
          {updateMutation.isPending ? 'Guardando...' : 'Guardar Examen Físico'}
        </button>
      </div>
    </form>
  );
};