'use client';

import { StepperStep } from '@/components/ui/Stepper';
import { WizardStep, WIZARD_STEPS, type WizardStepId } from './wizardSteps';

interface WizardStepperProps {
  currentStep: WizardStepId;
  completedSteps: WizardStepId[];
  onStepClick: (stepId: WizardStepId) => void;
  className?: string;
}

export function WizardStepper({
  currentStep,
  completedSteps,
  onStepClick,
  className,
}: WizardStepperProps) {
  return (
    <nav
      className={`flex items-center justify-between gap-2 overflow-x-auto pb-4 ${className || ''}`}
      aria-label="Progreso del asistente HCE"
    >
      {WIZARD_STEPS.map((step: WizardStep, index) => {
        const isCompleted = completedSteps.includes(step.id);
        const isCurrent = currentStep === step.id;
        const isClickable = isCompleted || isCurrent;

        return (
          <StepperStep
            key={step.id}
            number={index + 1}
            label={step.label}
            isActive={isCurrent}
            isCompleted={isCompleted}
            isDisabled={!isClickable}
            onClick={isClickable ? () => onStepClick(step.id) : undefined}
          />
        );
      })}
    </nav>
  );
}