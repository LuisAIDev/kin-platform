import TriageForm from "@/components/triage/TriageForm";

export const metadata = {
  title: "Triaje Digital | KIN Health",
  description:
    "Orientación informativa de posibles condiciones a partir de tus síntomas. No sustituye el diagnóstico médico profesional.",
};

export default function PatientTriagePage() {
  return <TriageForm />;
}
