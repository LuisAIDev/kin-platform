import { Suspense } from 'react';
import { WizardLayout } from './layout';

export default function HCEWizardPage() {
  return (
    <Suspense fallback={<div className="p-8 text-center">Cargando asistente...</div>}>
      <WizardLayout />
    </Suspense>
  );
}
