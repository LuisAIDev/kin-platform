'use client';

import { Check } from 'lucide-react';
import type { FC } from 'react';

interface StepperStepProps {
  number: number;
  label: string;
  isActive: boolean;
  isCompleted: boolean;
  isDisabled: boolean;
  onClick?: () => void;
}

export const StepperStep: FC<StepperStepProps> = ({
  number,
  label,
  isActive,
  isCompleted,
  isDisabled,
  onClick,
}) => {
  const baseClasses = `
    flex flex-col items-center gap-1.5 min-w-[80px] max-w-[100px]
    transition-all duration-200
    cursor-pointer select-none
  `;

  const circleClasses = `
    flex items-center justify-center w-8 h-8 rounded-full border-2 font-semibold text-sm
    transition-all duration-200
  `;

  const labelClasses = `
    text-xs font-medium text-center leading-tight
    transition-colors duration-200
  `;

  let circleClassName = circleClasses;
  let labelClassName = labelClasses;

  if (isCompleted) {
    circleClassName += ' bg-emerald-600 border-emerald-600 text-white';
    labelClassName += ' text-emerald-700';
  } else if (isActive) {
    circleClassName += ' bg-medical-600 border-medical-600 text-white ring-4 ring-medical-200';
    labelClassName += ' text-medical-700 font-semibold';
  } else if (isDisabled) {
    circleClassName += ' bg-neutral-100 border-neutral-300 text-neutral-400';
    labelClassName += ' text-neutral-400';
  } else {
    circleClassName += ' bg-neutral-100 border-neutral-300 text-neutral-500 hover:bg-neutral-200 hover:border-neutral-400';
    labelClassName += ' text-neutral-500 hover:text-neutral-700';
  }

  return (
    <button
      type="button"
      disabled={isDisabled}
      onClick={onClick}
      className={`${baseClasses} ${isDisabled ? 'opacity-60 cursor-not-allowed' : ''}`}
      aria-current={isActive ? 'step' : undefined}
      aria-label={`Paso ${number}: ${label} ${isCompleted ? '(completado)' : isActive ? '(actual)' : ''}`}
    >
      <span className={circleClassName}>
        {isCompleted ? (
          <Check className="w-4 h-4" aria-hidden="true" />
        ) : (
          number
        )}
      </span>
      <span className={labelClassName}>{label}</span>
    </button>
  );
};