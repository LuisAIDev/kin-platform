'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { MessageSquare, AlertCircle } from 'lucide-react';
import type { FC } from 'react';
import { encounterSchema, encounterTypeSchema, type EncounterFormData } from '@/lib/hce/schemas/encounter.schema';
import { useEncounter, useUpdateEncounter } from '@/lib/hce/hooks/useEncounter';

interface MotiveStepProps {
  encounterId: string;
  onSave: () => void;
  className?: string;
}

export const MotiveStep: FC<MotiveStepProps> = ({
  encounterId,
  onSave,
  className,
}) => {
  const { data: encounter, isLoading, error } = useEncounter(encounterId);
  const updateMutation = useUpdateEncounter(encounterId);

  const form = useForm<EncounterFormData>({
    resolver: zodResolver(encounterSchema),
    defaultValues: {
      patientId: '',
      encounterType: 'OUTPATIENT',
      chiefComplaint: '',
    },
  });

  const onSubmit = async (data: EncounterFormData) => {
    try {
      await updateMutation.mutateAsync(data);
      onSave();
    } catch (err) {
      console.error('Error updating encounter:', err);
    }
  };

  if (isLoading) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="status" aria-label="Cargando encuentro">
        <div className="animate-pulse space-y-4">
          <div className="h-10 bg-neutral-200 rounded w-1/3" />
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
            <p className="text-red-800">Error al cargar el encuentro</p>
          </div>
        </div>
      </div>
    );
  }

  const initialData: EncounterFormData = encounter ? {
    patientId: encounter.patientId,
    encounterType: encounter.encounterType as EncounterFormData['encounterType'],
    chiefComplaint: encounter.chiefComplaint || '',
  } : {
    patientId: '',
    encounterType: 'OUTPATIENT',
    chiefComplaint: '',
  };

  form.reset(initialData);

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className={`space-y-6 ${className || ''}`} noValidate>
      <div className="rounded-xl border border-neutral-200 bg-white p-6">
        <div className="flex items-center gap-2 mb-4">
          <MessageSquare className="w-5 h-5 text-medical-600" aria-hidden="true" />
          <h3 className="text-lg font-semibold text-neutral-800">Motivo de Consulta</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tipo de Encuentro *
            </label>
            <select
              {...form.register('encounterType')}
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            >
              <option value="OUTPATIENT">Consulta Externa</option>
              <option value="INPATIENT">Hospitalización</option>
              <option value="EMERGENCY">Urgencias</option>
              <option value="TELEMEDICINE">Telemedicina</option>
              <option value="HOME_CARE">Atención Domiciliaria</option>
              <option value="DAY_SURGERY">Cirugía Ambulatoria</option>
            </select>
            {form.formState.errors.encounterType && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.encounterType.message}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Motivo Principal de Consulta *
            </label>
            <textarea
              {...form.register('chiefComplaint')}
              rows={4}
              placeholder="Describa el motivo principal que trae al paciente a consulta..."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
              maxLength={500}
            />
            {form.formState.errors.chiefComplaint && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.chiefComplaint.message}</p>
            )}
            <p className="mt-1 text-sm text-neutral-500 text-right">
              {form.watch('chiefComplaint')?.length || 0} / 500 caracteres
            </p>
          </div>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3 pt-4">
        <button
          type="submit"
          disabled={updateMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-medical-600 px-6 py-3 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-40 transition-colors"
        >
          {updateMutation.isPending ? 'Guardando...' : 'Guardar Motivo'}
        </button>
      </div>
    </form>
  );
};