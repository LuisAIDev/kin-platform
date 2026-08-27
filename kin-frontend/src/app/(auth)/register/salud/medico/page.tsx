import PhysicianRegisterForm from "@/components/auth/PhysicianRegisterForm";
import RegisterShell from "@/components/auth/RegisterShell";

export default function RegisterMedicoPage() {
  return (
    <RegisterShell
      title="Registro de médico"
      subtitle="Tu cuenta quedará pendiente de verificación por un administrador."
    >
      <PhysicianRegisterForm />
    </RegisterShell>
  );
}
