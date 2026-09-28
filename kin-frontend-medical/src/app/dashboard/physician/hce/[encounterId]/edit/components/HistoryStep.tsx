'use client';

import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState, useCallback, useEffect } from 'react';
import {
  Plus, Trash2, X, ChevronDown, ChevronUp,
  AlertTriangle, Scissors, Pill, Syringe, Users, Skull, Baby,
  Save, Loader2, AlertCircle,
} from 'lucide-react';
import type { FC } from 'react';
import {
  allergySchema, surgerySchema, medicationSchema, vaccineSchema,
  familyHistorySchema, toxicHabitSchema, gynecoObstetricSchema,
  historyTypeSchema, getHistoryTypeLabel, getHistoryTypeIcon,
  createHistoryItemSchema, type HistoryType, type HistoryItemFormData,
} from '@/lib/hce/schemas/historyStep.schema';
import { usePatientHistory, useCreateHistoryItem, useDeleteHistoryItem } from '@/lib/hce/hooks/useHistory';

const ICON_COMPONENTS: Record<string, React.ComponentType<{ className?: string }>> = {
  AlertTriangle, Scissors, Pill, Syringe, Users, Skull, Baby,
};

interface HistoryTabProps {
  type: HistoryType;
  patientId: string;
  isActive: boolean;
  onSave: () => void;
}

function HistoryTab({ type, patientId, isActive, onSave }: HistoryTabProps) {
  const { data: history, isLoading, error, refetch } = usePatientHistory(patientId);
  const createMutation = useCreateHistoryItem(patientId);
  const deleteMutation = useDeleteHistoryItem();

  const [openModal, setOpenModal] = useState(false);
  const [editingItem, setEditingItem] = useState<HistoryItemFormData | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const filteredHistory = history?.filter((item: any) => item.type === type) || [];

  const ItemSchema = createHistoryItemSchema(type);
  const defaultValues = getDefaultValuesForType(type);

  const form = useForm({
    resolver: zodResolver(ItemSchema),
    defaultValues,
    mode: 'onChange',
  });

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: 'items',
  });

  const onSubmit = async (data: any) => {
    try {
      const itemData = {
        ...data,
        patientId,
        type,
      };
      if (editingItem?.id) {
        // For edit, we'd need an update endpoint
        // For now, just create new
      }
      await createMutation.mutateAsync(itemData);
      form.reset(defaultValues);
      setOpenModal(false);
      setEditingItem(null);
      refetch();
      onSave();
    } catch (err) {
      console.error('Error creating history item:', err);
    }
  };

  const handleEdit = (item: any) => {
    setEditingItem(item);
    form.reset({ ...defaultValues, ...item.data });
    setOpenModal(true);
  };

  const handleDelete = async (id: string) => {
    if (deleteConfirmId === id) {
      try {
        await deleteMutation.mutateAsync(id);
        refetch();
        onSave();
        setDeleteConfirmId(null);
      } catch (err) {
        console.error('Error deleting history item:', err);
      }
    } else {
      setDeleteConfirmId(id);
    }
  };

  const handleCloseModal = () => {
    setOpenModal(false);
    setEditingItem(null);
    form.reset(defaultValues);
  };

  if (isLoading) {
    return (
      <div className="space-y-4" role="status" aria-label={`Cargando ${getHistoryTypeLabel(type)}`}>
        <div className="animate-pulse space-y-3">
          <div className="h-16 bg-neutral-200 rounded" />
          <div className="h-16 bg-neutral-200 rounded" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-4" role="alert">
        <div className="rounded-lg bg-red-50 border border-red-200 p-4">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-600" />
            <p className="text-red-800">Error al cargar {getHistoryTypeLabel(type).toLowerCase()}</p>
          </div>
        </div>
      </div>
    );
  }

  const IconComponent = ICON_COMPONENTS[getHistoryTypeIcon(type)] || AlertTriangle;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-lg font-medium text-neutral-800">
          <IconComponent className="w-5 h-5 text-medical-600" />
          {getHistoryTypeLabel(type)}
        </h3>
        <button
          type="button"
          onClick={() => { setEditingItem(null); form.reset(defaultValues); setOpenModal(true); }}
          className="flex items-center gap-2 rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          Agregar
        </button>
      </div>

      {filteredHistory.length === 0 ? (
        <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center text-sm text-neutral-500">
          No hay {getHistoryTypeLabel(type).toLowerCase()} registrados.
        </div>
      ) : (
        <div className="rounded-xl border border-neutral-200 bg-white divide-y divide-neutral-100">
          {filteredHistory.map((item: any) => (
            <div key={item.id} className="flex items-center justify-between p-4 hover:bg-neutral-50 transition-colors">
              <div className="flex-1 min-w-0">
                <p className="font-medium text-neutral-800 truncate">
                  {formatItemDisplay(item, type)}
                </p>
                <p className="text-sm text-neutral-500 truncate">
                  {item.createdAt ? `Registrado: ${new Date(item.createdAt).toLocaleDateString()}` : ''}
                </p>
              </div>
              <div className="flex items-center gap-2 ml-4">
                <button
                  type="button"
                  onClick={() => handleEdit(item)}
                  className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-medical-600 transition-colors"
                  aria-label="Editar"
                >
                  <ChevronDown className="w-5 h-5" />
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(item.id)}
                  className={`p-2 rounded-lg transition-colors ${
                    deleteConfirmId === item.id
                      ? 'bg-red-50 text-red-600'
                      : 'text-neutral-500 hover:bg-neutral-100 hover:text-red-600'
                  }`}
                  aria-label={deleteConfirmId === item.id ? 'Confirmar eliminación' : 'Eliminar'}
                >
                  {deleteConfirmId === item.id ? (
                    <X className="w-5 h-5" />
                  ) : (
                    <Trash2 className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {openModal && (
        <HistoryModal
          type={type}
          form={form}
          onSubmit={onSubmit}
          onClose={handleCloseModal}
          isSubmitting={createMutation.isPending}
          editingItem={editingItem}
          schema={ItemSchema}
        />
      )}
    </div>
  );
}

function formatItemDisplay(item: any, type: HistoryType): string {
  const data = item.data || item;
  switch (type) {
    case 'ALLERGY': return `${data.allergen} (${data.severity})`;
    case 'SURGERY': return `${data.procedure} - ${data.date}`;
    case 'MEDICATION': return `${data.name} ${data.dosage ? `- ${data.dosage}` : ''}`;
    case 'VACCINE': return `${data.name} - ${data.date}`;
    case 'FAMILY_HISTORY': return `${data.relationship}: ${data.condition}`;
    case 'TOXICOLOGICAL': return `${data.substance} (${data.isActive ? 'Activo' : 'Inactivo'})`;
    case 'GYNECO_OBSTETRIC': return `G: ${data.gravida} P: ${data.para} A: ${data.abortions}`;
    default: return 'Item';
  }
}

function getDefaultValuesForType(type: HistoryType) {
  switch (type) {
    case 'ALLERGY':
      return { patientId: '', allergen: '', severity: 'MILD', status: 'ACTIVE' };
    case 'SURGERY':
      return { patientId: '', procedure: '', date: new Date().toISOString().split('T')[0] };
    case 'MEDICATION':
      return { patientId: '', name: '', isActive: true };
    case 'VACCINE':
      return { patientId: '', name: '', date: new Date().toISOString().split('T')[0] };
    case 'FAMILY_HISTORY':
      return { patientId: '', relationship: 'FATHER', condition: '' };
    case 'TOXICOLOGICAL':
      return { patientId: '', substance: 'TOBACCO', isActive: true };
    case 'GYNECO_OBSTETRIC':
      return { patientId: '', gravida: 0, para: 0, abortions: 0, livingChildren: 0 };
    default:
      return { patientId: '' };
  }
}

interface HistoryModalProps {
  type: HistoryType;
  form: ReturnType<typeof useForm>;
  onSubmit: (data: any) => void;
  onClose: () => void;
  isSubmitting: boolean;
  editingItem: any;
  schema: z.ZodTypeAny;
}

function HistoryModal({ type, form, onSubmit, onClose, isSubmitting, editingItem, schema }: HistoryModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white shadow-xl">
        <div className="sticky top-0 flex items-center justify-between border-b border-neutral-200 bg-white p-4 rounded-t-2xl">
          <h2 id="modal-title" className="text-lg font-semibold text-neutral-800">
            {editingItem ? 'Editar' : 'Nuevo'} {getHistoryTypeLabel(type).slice(0, -1)}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 transition-colors"
            aria-label="Cerrar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={form.handleSubmit(onSubmit)} className="p-4 space-y-4">
          {renderFormFields(type, form, schema)}

          <div className="sticky bottom-0 border-t border-neutral-200 bg-white p-4 rounded-b-2xl flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition-colors"
            >
              <X className="w-4 h-4" />
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex items-center gap-2 rounded-lg bg-medical-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-40 transition-colors"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Guardando...
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  {editingItem ? 'Actualizar' : 'Guardar'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

function renderFormFields(type: HistoryType, form: ReturnType<typeof useForm>, schema: z.ZodTypeAny) {
  const commonFields = (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="md:col-span-2">
        <label className="block text-sm font-medium text-neutral-700 mb-1">Notas</label>
        <textarea
          {...form.register('notes')}
          rows={3}
          placeholder="Notas adicionales..."
          className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500 resize-none"
        />
      </div>
    </div>
  );

  switch (type) {
    case 'ALLERGY':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Alérgeno *</label>
            <input {...form.register('allergen')} placeholder="Penicilina, polen, látex..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Reacción</label>
            <input {...form.register('reaction')} placeholder="Erupción, edema, anafilaxia..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Severidad</label>
            <select {...form.register('severity')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
              <option value="MILD">Leve</option>
              <option value="MODERATE">Moderada</option>
              <option value="SEVERE">Grave</option>
              <option value="ANAPHYLAXIS">Anafilaxia</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha de inicio</label>
            <input {...form.register('onsetDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          {commonFields}
        </div>
      );

    case 'SURGERY':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Procedimiento *</label>
            <input {...form.register('procedure')} placeholder="Apendicectomía, cesárea..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha *</label>
            <input {...form.register('date')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Hospital</label>
            <input {...form.register('hospital')} placeholder="Hospital Central..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Complicaciones</label>
            <input {...form.register('complications')} placeholder="Infección, sangrado..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          {commonFields}
        </div>
      );

    case 'MEDICATION':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Nombre *</label>
            <input {...form.register('name')} placeholder="Losartán, Metformina..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Dosis</label>
            <input {...form.register('dosage')} placeholder="50mg, 10ml..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Frecuencia</label>
            <input {...form.register('frequency')} placeholder="Cada 8 horas, 1 vez al día..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
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
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha inicio</label>
            <input {...form.register('startDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha fin</label>
            <input {...form.register('endDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Indicación</label>
            <input {...form.register('indication')} placeholder="Hipertensión, Diabetes..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div className="flex items-center gap-2">
            <input {...form.register('isActive')} type="checkbox" className="h-4 w-4 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500" />
            <label className="text-sm text-neutral-700">Activo</label>
          </div>
          {commonFields}
        </div>
      );

    case 'VACCINE':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Vacuna *</label>
            <input {...form.register('name')} placeholder="COVID-19, Influenza, Hepatitis B..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha *</label>
            <input {...form.register('date')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Dosis</label>
            <input {...form.register('dose')} placeholder="1ra dosis, refuerzo..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Lote</label>
            <input {...form.register('batch')} placeholder="Lote AB12345..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Próxima dosis</label>
            <input {...form.register('nextDoseDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          {commonFields}
        </div>
      );

    case 'FAMILY_HISTORY':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Familiar *</label>
            <select {...form.register('relationship')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
              <option value="FATHER">Padre</option>
              <option value="MOTHER">Madre</option>
              <option value="BROTHER">Hermano</option>
              <option value="SISTER">Hermana</option>
              <option value="SON">Hijo</option>
              <option value="DAUGHTER">Hija</option>
              <option value="GRANDFATHER_PATERNAL">Abuelo paterno</option>
              <option value="GRANDMOTHER_PATERNAL">Abuela paterna</option>
              <option value="GRANDFATHER_MATERNAL">Abuelo materno</option>
              <option value="GRANDMOTHER_MATERNAL">Abuela materna</option>
              <option value="UNCLE">Tío</option>
              <option value="AUNT">Tía</option>
              <option value="COUSIN">Primo/a</option>
              <option value="OTHER">Otro</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Condición *</label>
            <input {...form.register('condition')} placeholder="Diabetes, Hipertensión, Cáncer..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Edad de inicio</label>
            <input {...form.register('ageOfOnset', { valueAsNumber: true })} type="number" min={0} max={120} placeholder="55" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <input {...form.register('isDeceased')} type="checkbox" className="h-4 w-4 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500" />
              <label className="text-sm text-neutral-700">Fallecido</label>
            </div>
            <div>
              <label className="block text-sm font-medium text-neutral-700 mb-1">Edad al fallecer</label>
              <input {...form.register('ageAtDeath', { valueAsNumber: true })} type="number" min={0} max={120} placeholder="72" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
            </div>
          </div>
          {commonFields}
        </div>
      );

    case 'TOXICOLOGICAL':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Sustancia *</label>
            <select {...form.register('substance')} className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500">
              <option value="TOBACCO">Tabaco</option>
              <option value="ALCOHOL">Alcohol</option>
              <option value="CANNABIS">Cannabis</option>
              <option value="COCAINE">Cocaína</option>
              <option value="OPIOIDS">Opioides</option>
              <option value="OTHER">Otras</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Frecuencia</label>
            <input {...form.register('frequency')} placeholder="Diario, ocasional, fin de semana..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Cantidad</label>
            <input {...form.register('quantity')} placeholder="10 cigarrillos, 2 copas..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha inicio</label>
            <input {...form.register('startDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Fecha fin</label>
            <input {...form.register('endDate')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div className="flex items-center gap-2">
            <input {...form.register('isActive')} type="checkbox" className="h-4 w-4 rounded border-neutral-300 text-medical-600 focus:ring-2 focus:ring-medical-500" />
            <label className="text-sm text-neutral-700">Activo</label>
          </div>
          {commonFields}
        </div>
      );

    case 'GYNECO_OBSTETRIC':
      return (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Gestas (G)</label>
            <input {...form.register('gravida', { valueAsNumber: true })} type="number" min={0} placeholder="3" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Partos (P)</label>
            <input {...form.register('para', { valueAsNumber: true })} type="number" min={0} placeholder="2" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Abortos</label>
            <input {...form.register('abortions', { valueAsNumber: true })} type="number" min={0} placeholder="0" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Cesáreas</label>
            <input {...form.register('cesareans', { valueAsNumber: true })} type="number" min={0} placeholder="1" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Hijos vivos</label>
            <input {...form.register('livingChildren', { valueAsNumber: true })} type="number" min={0} placeholder="2" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Última menstruación</label>
            <input {...form.register('lastMenstrualPeriod')} type="date" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Edad menarquía</label>
            <input {...form.register('menarcheAge', { valueAsNumber: true })} type="number" min={8} max={30} placeholder="12" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Edad menopausia</label>
            <input {...form.register('menopauseAge', { valueAsNumber: true })} type="number" min={35} max={65} placeholder="50" className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Método anticonceptivo</label>
            <input {...form.register('contraceptiveMethod')} placeholder="DIU, pastillas, condón..." className="w-full px-4 py-3 rounded-lg border border-neutral-300 focus:outline-none focus:ring-2 focus:ring-medical-500" />
          </div>
          {commonFields}
        </div>
      );

    default:
      return <div>{commonFields}</div>;
  }
}

const HISTORY_TYPES: HistoryType[] = [
  'ALLERGY', 'SURGERY', 'MEDICATION', 'VACCINE',
  'FAMILY_HISTORY', 'TOXICOLOGICAL', 'GYNECO_OBSTETRIC',
];

interface HistoryStepProps {
  encounterId: string;
  patientId: string;
  onSave: () => void;
  isDirty: boolean;
  className?: string;
}

export const HistoryStep: FC<HistoryStepProps> = ({
  patientId,
  onSave,
  isDirty,
  className,
}) => {
  const [activeTab, setActiveTab] = useState<HistoryType>('ALLERGY');

  return (
    <div className={`space-y-6 ${className || ''}`}>
      <div className="flex items-center gap-2 mb-4">
        <h2 className="text-xl font-semibold text-neutral-800">Historia Clínica</h2>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-2 border-b border-neutral-200">
        {HISTORY_TYPES.map((type) => (
          <button
            key={type}
            type="button"
            onClick={() => setActiveTab(type)}
            className={`flex items-center gap-2 whitespace-nowrap px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              activeTab === type
                ? 'bg-medical-600 text-white'
                : 'text-neutral-600 hover:bg-neutral-100 hover:text-medical-700'
            }`}
            role="tab"
            aria-selected={activeTab === type}
            aria-controls={`history-tabpanel-${type}`}
          >
            <span>{getHistoryTypeLabel(type)}</span>
          </button>
        ))}
      </div>

      <div id={`history-tabpanel-${activeTab}`} role="tabpanel" aria-labelledby={`history-tab-${activeTab}`}>
        <HistoryTab
          type={activeTab}
          patientId={patientId}
          isActive={true}
          onSave={onSave}
        />
      </div>
    </div>
  );
};