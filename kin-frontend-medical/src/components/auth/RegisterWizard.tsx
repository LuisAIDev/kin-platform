'use client';

import { CheckCircle2, Circle } from 'lucide-react';

interface Step {
  id: number;
  title: string;
  description?: string;
}

interface RegisterWizardProps {
  steps: Step[];
  currentStep: number;
  children: React.ReactNode;
}

export function RegisterWizard({ steps, currentStep, children }: RegisterWizardProps) {
  return (
    <div className="w-full">
      <div className="flex items-center justify-center mb-8">
        {steps.map((step, index) => (
          <div key={step.id} className="flex items-center">
            <div className="flex flex-col items-center">
              <div
                className={`w-10 h-10 rounded-full flex items-center justify-center transition-all duration-300 ${
                  currentStep > step.id
                    ? 'bg-medical-600 text-white shadow-md shadow-medical-600/30'
                    : currentStep === step.id
                    ? 'bg-medical-100 text-medical-600 border-2 border-medical-600'
                    : 'bg-neutral-100 text-neutral-400 border-2 border-neutral-200'
                }`}
              >
                {currentStep > step.id ? (
                  <CheckCircle2 className="w-5 h-5" />
                ) : (
                  <span className="font-semibold text-sm">{step.id}</span>
                )}
              </div>
              <div className="mt-2 text-center max-w-[100px]">
                <p
                  className={`text-xs font-medium leading-tight ${
                    currentStep >= step.id ? 'text-medical-600' : 'text-neutral-400'
                  }`}
                >
                  {step.title}
                </p>
              </div>
            </div>
            {index < steps.length - 1 && (
              <div
                className={`w-16 h-0.5 mx-2 rounded-full transition-all duration-300 ${
                  currentStep > step.id ? 'bg-medical-600' : 'bg-neutral-200'
                }`}
              />
            )}
          </div>
        ))}
      </div>
      <div className="bg-white rounded-2xl shadow-xl border border-neutral-200 p-8 sm:p-10">
        {children}
      </div>
    </div>
  );
}
