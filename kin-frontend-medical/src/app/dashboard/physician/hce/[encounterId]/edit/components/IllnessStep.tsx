'use client';

import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useEffect, useCallback } from 'react';
import { ChevronDown, ChevronUp, AlertCircle, Save, Clock, Gauge } from 'lucide-react';
import type { FC } from 'react';
import { illnessStepSchema, type IllnessStepFormData, validateOnsetDatetime } from '@/lib/hce/schemas/illnessStep.schema';
import { useAnamnesis, useUpdateAnamnesis } from '@/lib/hce/hooks/useAnamnesis';

const SYSTEMS = [
  { key: 'general', label: 'General' },
  { key: 'cardiovascular', label: 'Cardiovascular' },
  { key: 'respiratory', label: 'Respiratorio' },
  { key: 'digestive', label: 'Digestivo' },
  { key: 'genitourinary', label: 'Genitourinario' },
  { key: 'musculoskeletal', label: 'Musculoesquelético' },
  { key: 'neurological', label: 'Neurológico' },
  { key: 'endocrine', label: 'Endocrino' },
  { key: 'hematological', label: 'Hematológico' },
  { key: 'dermatological', label: 'Dermatológico' },
  { key: 'psychiatric', label: 'Psiquiátrico' },
  { key: 'ophthalmological', label: 'Oftalmológico' },
  { key: 'otorhinolaryngological', label: 'Otorrinolaringológico' },
  { key: 'other', label: 'Otro' },
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

interface SystemReviewItemProps {
  key: string;
  label: string;
  checked: boolean;
  notes: string;
  onChange: (checked: boolean, notes: string) => void;
}

function SystemReviewItem({ key: systemKey, label, checked, notes, onChange }: SystemReviewItemProps) {
  const [localNotes, setLocalNotes] = useState(notes);
  const [showNotes, setShowNotes] = useState(false);

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newChecked = e.target.checked;
    if (!newChecked) {
      setLocalNotes('');
      setShowNotes(false);
    } else {
      setShowNotes(true);
    }
    onChange(newChecked, newChecked ? localNotes : '');
  };

  const handleNotesChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setLocalNotes(e.target.value);
    onChange(checked, e.target.value);
  };

  return (
    <div className="flex items-start gap-3">
      <input
        type="checkbox"
        id={`system-${systemKey}`}
        checked={checked}
        onChange={handleCheckboxChange}
        className="mt-1 h-4 w-4 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500 cursor-pointer"
        aria-label={label}
      />
      <div className="flex-1 min-w-0">
        <label htmlFor={`system-${systemKey}`} className="font-medium text-neutral-700">
          {label}
        </label>
        {showNotes && (
          <textarea
            value={localNotes}
            onChange={handleNotesChange}
            placeholder={`Notas sobre ${label.toLowerCase()}...`}
            rows={2}
            className="mt-1 w-full px-3 py-2 text-sm border border-neutral-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
            aria-label={`Notas de ${label}`}
          />
        )}
      </div>
    </div>
  );
}

interface SeveritySliderProps {
  value: number | undefined;
  onChange: (value: number) => void;
  disabled?: boolean;
}

function SeveritySlider({ value, onChange, disabled }: SeveritySliderProps) {
  const displayValue = value ?? 5;
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <label className="text-sm font-medium text-neutral-700">Severidad (1-10)</label>
        <span className="text-lg font-bold text-medical-600">{displayValue}</span>
      </div>
      <input
        type="range"
        min={1}
        max={10}
        value={displayValue}
        onChange={(e) => onChange(parseInt(e.target.value, 10))}
        disabled={disabled}
        className="w-full h-2 bg-neutral-200 rounded-lg appearance-none cursor-pointer accent-medical-600 disabled:opacity-40 disabled:cursor-not-allowed"
        aria-label="Severidad auto-reportada"
      />
      <div className="flex justify-between text-xs text-neutral-400">
        <span>Leve (1)</span>
        <span>Moderado (5)</span>
        <span>Severo (10)</span>
      </div>
    </div>
  );
}

interface IllnessStepProps {
  encounterId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
}

export const IllnessStep: FC<IllnessStepProps> = ({
  encounterId,
  onSave,
  isDirty,
  className,
}) => {
  const { data: anamnesis, isLoading, error } = useAnamnesis(encounterId);
  const updateMutation = useUpdateAnamnesis(encounterId);

  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  const form = useForm<IllnessStepFormData>({
    resolver: zodResolver(illnessStepSchema),
    defaultValues: {
      encounterId,
      onsetDatetime: new Date().toISOString().slice(0, 16),
      severitySelfReported: 5,
    },
    mode: 'onChange',
  });

  const watchSystemsReview = form.watch('systemsReview');

  const onSubmit = async (data: IllnessStepFormData) => {
    try {
      await updateMutation.mutateAsync(data);
      setLastSaved(new Date());
      onSave();
    } catch (err) {
      console.error('Error updating anamnesis:', err);
    }
  };

  const handleAutoSave = useCallback((data: IllnessStepFormData) => {
    if (saveTimer) clearTimeout(saveTimer);
    setSaveTimer(setTimeout(() => {
      updateMutation.mutate(data);
      setLastSaved(new Date());
    }, 30000));
  }, [updateMutation]);

  useEffect(() => {
    const subscription = form.watch((data) => {
      handleAutoSave(data);
    });
    return () => subscription.unsubscribe();
  }, [form, handleAutoSave]);

  useEffect(() => {
    if (anamnesis) {
      const initialData = {
        encounterId: anamnesis.encounterId,
        onsetDatetime: anamnesis.onsetDatetime?.slice(0, 16) || new Date().toISOString().slice(0, 16),
        evolutionDescription: anamnesis.evolutionDescription || '',
        aggravatingFactors: anamnesis.aggravatingFactors || '',
        alleviatingFactors: anamnesis.alleviatingFactors || '',
        associatedSymptoms: anamnesis.associatedSymptoms || '',
        severitySelfReported: anamnesis.severitySelfReported ?? 5,
        systemsReview: anamnesis.systemsReview || {},
        previousEpisodes: anamnesis.previousEpisodes,
        previousTreatments: anamnesis.previousTreatments || '',
        functionalImpact: anamnesis.functionalImpact || '',
      };
      form.reset(initialData);
    }
  }, [anamnesis, form]);

  if (isLoading) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="status" aria-label="Cargando enfermedad actual">
        <div className="animate-pulse space-y-4">
          <div className="h-12 bg-neutral-200 rounded" />
          <div className="h-12 bg-neutral-200 rounded" />
          <div className="h-12 bg-neutral-200 rounded" />
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
            <p className="text-red-800">Error al cargar la enfermedad actual</p>
          </div>
        </div>
      </div>
    );
  }

  const onsetValidation = validateOnsetDatetime(form.watch('onsetDatetime') || '');

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className={`space-y-6 ${className || ''}`} noValidate>
      <CollapsibleSection title="Datos de Inicio" defaultOpen icon={<Clock className="w-5 h-5" />}>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Fecha y hora de inicio *
            </label>
            <input
              {...form.register('onsetDatetime')}
              type="datetime-local"
              max={new Date().toISOString().slice(0, 16)}
              className={`w-full px-4 py-3 rounded-lg border focus:outline-none focus:ring-2 focus:ring-medical-500 transition-colors ${
                onsetValidation.valid === false ? 'border-red-500 focus:ring-red-500' : 'border-neutral-300 focus:ring-medical-500'
              }`}
            />
            {!onsetValidation.valid && (
              <p className="mt-1 text-sm text-red-600">{onsetValidation.message}</p>
            )}
            {form.formState.errors.onsetDatetime && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.onsetDatetime.message}</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Episodios previos
            </label>
            <input
              {...form.register('previousEpisodes', { valueAsNumber: true })}
              type="number"
              min={0}
              placeholder="0"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.previousEpisodes && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.previousEpisodes.message}</p>
            )}
          </div>
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Descripción de la Evolución" defaultOpen>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Descripción de la evolución *
            </label>
            <textarea
              {...form.register('evolutionDescription')}
              rows={4}
              placeholder="Describe cómo ha evolucionado el cuadro clínico desde su inicio..."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={2000}
            />
            {form.formState.errors.evolutionDescription && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.evolutionDescription.message}</p>
            )}
            <p className="text-sm text-neutral-500 text-right">
              {form.watch('evolutionDescription')?.length || 0} / 2000 caracteres
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Factores agravantes
              </label>
              <textarea
                {...form.register('aggravatingFactors')}
                rows={3}
                placeholder="¿Qué empeora los síntomas? (ej. movimiento, alimentos, estrés)"
                className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
                maxLength={1000}
              />
              <p className="text-sm text-neutral-500 text-right">
                {form.watch('aggravatingFactors')?.length || 0} / 1000 caracteres
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">
                Factores aliviantes
              </label>
              <textarea
                {...form.register('alleviatingFactors')}
                rows={3}
                placeholder="¿Qué mejora los síntomas? (ej. reposo, medicamentos, posición)"
                className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
                maxLength={1000}
              />
              <p className="text-sm text-neutral-500 text-right">
                {form.watch('alleviatingFactors')?.length || 0} / 1000 caracteres
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Síntomas asociados
            </label>
            <textarea
              {...form.register('associatedSymptoms')}
              rows={3}
              placeholder="Otros síntomas que acompañan al principal (ej. náuseas, fiebre, mareo)"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={1000}
            />
            <p className="text-sm text-neutral-500 text-right">
              {form.watch('associatedSymptoms')?.length || 0} / 1000 caracteres
            </p>
          </div>
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Severidad Auto-reportada" defaultOpen icon={<Gauge className="w-5 h-5" />}>
        <SeveritySlider
          value={form.watch('severitySelfReported')}
          onChange={(val) => form.setValue('severitySelfReported', val, { shouldValidate: true })}
        />
        {form.formState.errors.severitySelfReported && (
          <p className="mt-1 text-sm text-red-600">{form.formState.errors.severitySelfReported.message}</p>
        )}
      </CollapsibleSection>

      <CollapsibleSection title="Revisión por Sistemas (14 sistemas)" defaultOpen>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
          {SYSTEMS.map((system) => (
            <SystemReviewItem
              key={system.key}
              label={system.label}
              checked={watchSystemsReview?.[system.key]?.checked ?? false}
              notes={watchSystemsReview?.[system.key]?.notes ?? ''}
              onChange={(checked, notes) => {
                form.setValue(`systemsReview.${system.key}`, { checked, notes }, { shouldValidate: true });
              }}
            />
          ))}
        </div>
      </CollapsibleSection>

      <CollapsibleSection title="Antecedentes y Impacto" defaultOpen>
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tratamientos previos
            </label>
            <textarea
              {...form.register('previousTreatments')}
              rows={3}
              placeholder="¿Qué tratamientos ha probado? (medicamentos, terapias, cirugías)"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={1000}
            />
            <p className="text-sm text-neutral-500 text-right">
              {form.watch('previousTreatments')?.length || 0} / 1000 caracteres
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Impacto funcional
            </label>
            <textarea
              {...form.register('functionalImpact')}
              rows={3}
              placeholder="¿Cómo afecta esto tu vida diaria? (trabajo, sueño, actividades, estado de ánimo)"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={1000}
            />
            <p className="text-sm text-neutral-500 text-right">
              {form.watch('functionalImpact')?.length || 0} / 1000 caracteres
            </p>
          </div>
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
          {updateMutation.isPending ? 'Guardando...' : 'Guardar Enfermedad Actual'}
        </button>
      </div>
    </form>
  );
};