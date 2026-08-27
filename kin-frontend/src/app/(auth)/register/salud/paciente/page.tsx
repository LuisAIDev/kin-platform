import PatientRegisterForm from "@/components/auth/PatientRegisterForm";
import RegisterShell from "@/components/auth/RegisterShell";

export default function RegisterPacientePage() {
  return (
    <RegisterShell
      title="Registro de paciente"
      subtitle="Crea tu cuenta para acceder a la vertical de salud."
    >
      <PatientRegisterForm />
    </RegisterShell>
  );
}
