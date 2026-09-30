'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { User, Calendar, Phone, Mail, MapPin, AlertCircle, Shield } from 'lucide-react';
import { useEffect } from 'react';
import type { FC } from 'react';
import { patientIdentificationSchema, type PatientIdentificationFormData } from '@/lib/hce/schemas/encounter.schema';
import { usePatientIdentification, useUpdatePatientIdentification } from '@/lib/hce/hooks/usePatientIdentification';

interface IdentificationStepProps {
  patientId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
}

export const IdentificationStep: FC<IdentificationStepProps> = ({
  patientId,
  onSave,
  isDirty,
  className,
}) => {
  const { data: identification, isLoading, error } = usePatientIdentification(patientId);
  const updateMutation = useUpdatePatientIdentification(patientId);

  const form = useForm<PatientIdentificationFormData>({
    resolver: zodResolver(patientIdentificationSchema),
    defaultValues: {
      patientId,
      documentType: 'CC',
      rhFactor: 'A_POS',
      regime: 'CONTRIBUTIVO',
    },
  });

  const onSubmit = async (data: PatientIdentificationFormData) => {
    try {
      await updateMutation.mutateAsync(data);
      onSave();
    } catch (err) {
      console.error('Error updating identification:', err);
    }
  };

  // `form.reset` dentro de un efecto (no en render) para evitar el bucle de
  // re-renders (React #301). Debe ir ANTES de los early-returns: si queda
  // después, en el render de carga no se ejecuta y luego sí -> React #310
  // ("Rendered fewer hooks than expected").
  useEffect(() => {
    if (!identification) return;
    form.reset({
      patientId: identification.patientId,
      documentType: identification.documentType,
      documentNumber: identification.documentNumber,
      firstName: identification.firstName,
      lastName: identification.lastName,
      birthDate: identification.birthDate?.split('T')[0] || '',
      rhFactor: identification.rhFactor || 'A_POS',
      eps: identification.eps || '',
      regime: identification.regime || 'CONTRIBUTIVO',
      phone: identification.phone || '',
      email: identification.email || '',
      address: identification.address || '',
      city: identification.city || '',
      emergencyContactName: identification.emergencyContactName || '',
      emergencyContactPhone: identification.emergencyContactPhone || '',
      emergencyContactRelation: identification.emergencyContactRelation || '',
      guardianDocumentType: identification.guardianDocumentType || '',
      guardianDocumentNumber: identification.guardianDocumentNumber || '',
      guardianName: identification.guardianName || '',
    } as any);
  }, [identification, form]);

  if (isLoading) {
    return (
      <div className={`space-y-6 ${className || ''}`} role="status" aria-label="Cargando identificación">
        <div className="animate-pulse space-y-4">
          <div className="h-10 bg-neutral-200 rounded w-1/4" />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="h-12 bg-neutral-200 rounded" />
            <div className="h-12 bg-neutral-200 rounded" />
          </div>
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
            <p className="text-red-800">Error al cargar la identificación</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className={`space-y-6 ${className || ''}`} noValidate>
      <div className="rounded-xl border border-neutral-200 bg-white p-6">
        <div className="flex items-center gap-2 mb-4">
          <Shield className="w-5 h-5 text-medical-600" aria-hidden="true" />
          <h3 className="text-lg font-semibold text-neutral-800">Identificación del Paciente</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tipo de Documento *
            </label>
            <select
              {...form.register('documentType')}
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            >
              <option value="CC">Cédula de Ciudadanía (CC)</option>
              <option value="TI">Tarjeta de Identidad (TI)</option>
              <option value="CE">Cédula de Extranjería (CE)</option>
              <option value="PP">Pasaporte (PP)</option>
              <option value="RC">Registro Civil (RC)</option>
            </select>
            {form.formState.errors.documentType && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.documentType.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Número de Documento *
            </label>
            <input
              {...form.register('documentNumber')}
              type="text"
              placeholder="1234567890"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.documentNumber && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.documentNumber.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Nombres *
            </label>
            <input
              {...form.register('firstName')}
              type="text"
              placeholder="Juan"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.firstName && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.firstName.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Apellidos *
            </label>
            <input
              {...form.register('lastName')}
              type="text"
              placeholder="Pérez Gómez"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.lastName && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.lastName.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Fecha de Nacimiento *
            </label>
            <input
              {...form.register('birthDate')}
              type="date"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.birthDate && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.birthDate.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Grupo Sanguíneo (RH)
            </label>
            <select
              {...form.register('rhFactor')}
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            >
              <option value="A_POS">A+</option>
              <option value="A_NEG">A-</option>
              <option value="B_POS">B+</option>
              <option value="B_NEG">B-</option>
              <option value="AB_POS">AB+</option>
              <option value="AB_NEG">AB-</option>
              <option value="O_POS">O+</option>
              <option value="O_NEG">O-</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              EPS
            </label>
            <input
              {...form.register('eps')}
              type="text"
              placeholder="EPS Sanitas, Sura, etc."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Régimen
            </label>
            <select
              {...form.register('regime')}
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            >
              <option value="CONTRIBUTIVO">Contributivo</option>
              <option value="SUBSIDIADO">Subsidiado</option>
              <option value="ESPECIAL">Especial</option>
              <option value="EXCEPCION">Excepción</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Teléfono
            </label>
            <input
              {...form.register('phone')}
              type="tel"
              placeholder="300 123 4567"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Email
            </label>
            <input
              {...form.register('email')}
              type="email"
              placeholder="paciente@email.com"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
            {form.formState.errors.email && (
              <p className="mt-1 text-sm text-red-600">{form.formState.errors.email.message}</p>
            )}
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Dirección
            </label>
            <input
              {...form.register('address')}
              type="text"
              placeholder="Calle 123 #45-67"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Ciudad
            </label>
            <input
              {...form.register('city')}
              type="text"
              placeholder="Bogotá"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-neutral-200 bg-white p-6">
        <div className="flex items-center gap-2 mb-4">
          <User className="w-5 h-5 text-medical-600" aria-hidden="true" />
          <h3 className="text-lg font-semibold text-neutral-800">Contacto de Emergencia</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Nombre
            </label>
            <input
              {...form.register('emergencyContactName')}
              type="text"
              placeholder="María González"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Teléfono
            </label>
            <input
              {...form.register('emergencyContactPhone')}
              type="tel"
              placeholder="300 987 6543"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Parentesco
            </label>
            <input
              {...form.register('emergencyContactRelation')}
              type="text"
              placeholder="Madre, Padre, Cónyuge, etc."
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-neutral-200 bg-white p-6">
        <div className="flex items-center gap-2 mb-4">
          <Shield className="w-5 h-5 text-medical-600" aria-hidden="true" />
          <h3 className="text-lg font-semibold text-neutral-800">Acudiente (si aplica - menor de edad)</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Tipo Doc. Acudiente
            </label>
            <select
              {...form.register('guardianDocumentType')}
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            >
              <option value="">Seleccionar</option>
              <option value="CC">CC</option>
              <option value="TI">TI</option>
              <option value="CE">CE</option>
              <option value="PP">PP</option>
              <option value="RC">RC</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              No. Doc. Acudiente
            </label>
            <input
              {...form.register('guardianDocumentNumber')}
              type="text"
              placeholder="1234567890"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Nombre Acudiente
            </label>
            <input
              {...form.register('guardianName')}
              type="text"
              placeholder="Carlos Pérez"
              className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500"
            />
          </div>
        </div>
      </div>

      <div className="flex items-center justify-end gap-3 pt-4">
        <button
          type="submit"
          disabled={updateMutation.isPending}
          className="flex items-center gap-2 rounded-lg bg-medical-600 px-6 py-3 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-40 transition-colors"
        >
          {updateMutation.isPending ? 'Guardando...' : 'Guardar Identificación'}
        </button>
      </div>
    </form>
  );
};