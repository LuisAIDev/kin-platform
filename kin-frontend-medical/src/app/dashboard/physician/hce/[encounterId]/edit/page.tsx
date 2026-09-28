import { Suspense } from 'react';
import { WizardLayout } from './layout';

export default function HCEWizardPage({
  params,
}: {
  params: Promise<{ encounterId: string }>;
}) {
  return (
    <Suspense fallback={<div className="p-8 text-center">Cargando asistente...</div>}>
      <WizardLayout params={params} />
    </Suspense>
  );
}