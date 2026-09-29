'use client';

import { useState, useEffect, useCallback } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { Loader2, Save, XCircle } from 'lucide-react';
import { WizardStepper } from './components/WizardStepper';
import { WizardNavigation } from './components/WizardNavigation';
import { IdentificationStep } from './components/IdentificationStep';
import { MotiveStep } from './components/MotiveStep';
import { WIZARD_STEPS, type WizardStepId, STEP_ORDER } from './components/wizardSteps';
import { useEncounter } from '@/lib/hce/hooks/useEncounter';
import type { EncounterFormData } from '@/lib/hce/schemas/encounter.schema';

const STEP_COMPONENTS: Record<WizardStepId, React.ComponentType<any>> = {
  identification: IdentificationStep,
  motive: MotiveStep,
  illness: () => <div className="p-6 text-center text-neutral-500">Paso 3 - Enfermedad Actual (pendiente)</div>,
  history: () => <div className="p-6 text-center text-neutral-500">Paso 4 - Antecedentes (pendiente)</div>,
  physicalExam: () => <div className="p-6 text-center text-neutral-500">Paso 5 - Examen Físico (pendiente)</div>,
  diagnosisPlan: () => <div className="p-6 text-center text-neutral-500">Paso 6 - Diagnóstico y Plan (pendiente)</div>,
  closing: () => <div className="p-6 text-center text-neutral-500">Paso 7 - Cierre (pendiente)</div>,
};

export function WizardLayout() {
  const router = useRouter();
  const { encounterId } = useParams<{ encounterId: string }>();
  const resolvedEncounterId = encounterId;

  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [completedSteps, setCompletedSteps] = useState<WizardStepId[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

  const currentStepId = STEP_ORDER[currentStepIndex];
  const isLastStep = currentStepIndex === STEP_ORDER.length - 1;

  const { data: encounter, isLoading: encounterLoading } = useEncounter(resolvedEncounterId);

  const canProceed = (stepId: WizardStepId) => {
    const step = WIZARD_STEPS.find((s) => s.id === stepId);
    if (!step?.required) return true;
    return completedSteps.includes(stepId);
  };

  const handleStepClick = useCallback((stepId: WizardStepId) => {
    const targetIndex = STEP_ORDER.indexOf(stepId);
    if (targetIndex !== -1 && (targetIndex <= currentStepIndex || canProceed(stepId))) {
      setCurrentStepIndex(targetIndex);
    }
  }, [currentStepIndex]);

  const handleSave = useCallback(() => {
    setCompletedSteps((prev) => {
      if (!prev.includes(currentStepId)) {
        return [...prev, currentStepId];
      }
      return prev;
    });
    setHasUnsavedChanges(false);
  }, [currentStepId]);

  const handleNext = useCallback(() => {
    if (currentStepIndex < STEP_ORDER.length - 1) {
      setCurrentStepIndex((prev) => prev + 1);
    }
  }, []);

  const handlePrevious = useCallback(() => {
    if (currentStepIndex > 0) {
      setCurrentStepIndex((prev) => prev - 1);
    }
  }, []);

  const handleFinish = useCallback(async () => {
    setIsSaving(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 500));
      router.push(`/dashboard/physician/hce/${resolvedEncounterId}`);
    } finally {
      setIsSaving(false);
    }
  }, [resolvedEncounterId, router]);

  const StepComponent = STEP_COMPONENTS[currentStepId];

  if (encounterLoading) {
    return (
      <div className="min-h-screen bg-neutral-50 flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-medical-600" aria-hidden="true" />
      </div>
    );
  }

  if (!encounter) {
    return (
      <div className="min-h-screen bg-neutral-50 flex items-center justify-center">
        <div className="text-center">
          <XCircle className="w-12 h-12 text-red-500 mx-auto mb-4" aria-hidden="true" />
          <h2 className="text-xl font-semibold text-neutral-800 mb-2">Encuentro no encontrado</h2>
          <button
            onClick={() => router.push('/dashboard/physician')}
            className="text-medical-600 hover:text-medical-700 font-medium"
          >
            Volver al portal médico
          </button>
        </div>
      </div>
    );
  }

  const handleFormChange = useCallback(() => {
    setHasUnsavedChanges(true);
  }, []);

  return (
    <div className="min-h-screen bg-neutral-50">
      <header className="sticky top-0 z-40 bg-white border-b border-neutral-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-4">
              <button
                onClick={() => router.back()}
                className="p-2 rounded-lg text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 transition"
                aria-label="Volver"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              <div>
                <h1 className="text-xl font-bold text-neutral-800">Historia Clínica Electrónica</h1>
                <p className="text-sm text-neutral-500">Encuentro: {encounter.id.slice(0, 8)}...</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              {hasUnsavedChanges && (
                <span className="flex items-center gap-1.5 text-xs text-amber-600 bg-amber-50 px-3 py-1 rounded-full">
                  <Save className="w-3 h-3" aria-hidden="true" />
                  Cambios sin guardar
                </span>
              )}
              <Save className="w-5 h-5 text-neutral-300" aria-hidden="true" />
            </div>
          </div>
        </div>

        <div className="px-4 pb-4 border-b border-neutral-200">
          <WizardStepper
            currentStep={currentStepId}
            completedSteps={completedSteps}
            onStepClick={handleStepClick}
          />
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <StepComponent
          encounterId={resolvedEncounterId}
          onSave={handleSave}
        />

        <WizardNavigation
          currentStep={currentStepId}
          totalSteps={STEP_ORDER.length}
          currentStepIndex={currentStepIndex}
          onPrevious={handlePrevious}
          onNext={handleNext}
          onFinish={handleFinish}
          isSubmitting={isSaving}
          canProceed={canProceed(currentStepId)}
          isLastStep={isLastStep}
        />
      </main>
    </div>
  );
}

// Layout de ruta: pass-through. El contenido real vive en `page.tsx`, que
// renderiza `WizardLayout`. Se declara el default requerido por Next sin
// duplicar el render.
export default function EncounterEditLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}