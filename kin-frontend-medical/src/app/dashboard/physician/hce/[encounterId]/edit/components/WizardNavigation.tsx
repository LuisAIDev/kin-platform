'use client';

import { ChevronLeft, ChevronRight, CheckCircle } from 'lucide-react';
import type { FC } from 'react';
import type { WizardStepId } from './wizardSteps';

interface WizardNavigationProps {
  currentStep: WizardStepId;
  totalSteps: number;
  currentStepIndex: number;
  onPrevious: () => void;
  onNext: () => void;
  onFinish: () => void;
  isSubmitting: boolean;
  canProceed: boolean;
  isLastStep: boolean;
  showFinish?: boolean;
  className?: string;
}

export const WizardNavigation: FC<WizardNavigationProps> = ({
  currentStep,
  totalSteps,
  currentStepIndex,
  onPrevious,
  onNext,
  onFinish,
  isSubmitting,
  canProceed,
  isLastStep,
  showFinish = true,
  className,
}) => {
  const isFirstStep = currentStepIndex === 0;

  return (
    <div
      className={`flex items-center justify-between gap-4 pt-6 border-t border-neutral-200 ${className || ''}`}
      role="navigation"
      aria-label="Navegación del asistente"
    >
      <button
        type="button"
        onClick={onPrevious}
        disabled={isFirstStep || isSubmitting}
        className="flex items-center gap-2 rounded-lg border border-neutral-300 px-4 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        aria-label="Paso anterior"
      >
        <ChevronLeft className="w-4 h-4" aria-hidden="true" />
        Anterior
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-3">
        <span className="text-sm text-neutral-500 hidden sm:block">
          Paso {currentStepIndex + 1} de {totalSteps}
        </span>

        {!isLastStep ? (
          <button
            type="button"
            onClick={onNext}
            disabled={!canProceed || isSubmitting}
            className="flex items-center gap-2 rounded-lg bg-medical-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-medical-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            aria-label="Paso siguiente"
          >
            Siguiente
            <ChevronRight className="w-4 h-4" aria-hidden="true" />
          </button>
        ) : showFinish ? (
          <button
            type="button"
            onClick={onFinish}
            disabled={!canProceed || isSubmitting}
            className="flex items-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            aria-label="Finalizar y cerrar consulta"
          >
            <CheckCircle className="w-4 h-4" aria-hidden="true" />
            Cerrar consulta
          </button>
        ) : null}
      </div>
    </div>
  );
};