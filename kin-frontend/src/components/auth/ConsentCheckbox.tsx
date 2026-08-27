"use client";

/**
 * Checkbox de consentimiento para el tratamiento de datos de salud
 * (requisito legal del auto-registro de la vertical Salud).
 */
export default function ConsentCheckbox({
  checked,
  onChange,
}: {
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label className="flex items-start gap-2 text-xs text-neutral-600 cursor-pointer">
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        required
        className="mt-0.5 h-4 w-4 shrink-0 rounded border-neutral-300 accent-primary-600"
      />
      <span>
        Acepto el tratamiento de mis datos de salud con fines de atención y
        triaje, conforme a la política de privacidad de KIN. Autorizo el acceso
        de mi médico asignado a esta información.
      </span>
    </label>
  );
}
