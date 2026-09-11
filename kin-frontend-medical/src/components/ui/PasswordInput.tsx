'use client';

import { useState, useEffect } from 'react';
import { Eye, EyeOff } from 'lucide-react';

interface ValidationResult {
  valid: boolean;
  message?: string;
}

interface PasswordInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  validation?: (value: string) => ValidationResult;
}

export function PasswordInput({ label, className, validation, ...props }: PasswordInputProps) {
  const [showPassword, setShowPassword] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const value = props.value as string || '';

  useEffect(() => {
    if (validation && value) {
      setValidationResult(validation(value));
    }
  }, [value, validation]);

  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-neutral-700 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative">
        <input
          type={showPassword ? 'text' : 'password'}
          className={`w-full px-4 py-3 pr-12 rounded-lg border border-neutral-300 text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-medical-500 focus:border-transparent transition-colors disabled:bg-neutral-100 disabled:cursor-not-allowed ${className || ''}`}
          {...props}
        />
        <button
          type="button"
          onClick={() => setShowPassword(!showPassword)}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-600 transition-colors focus:outline-none focus:text-medical-600"
          aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          tabIndex={-1}
        >
          {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
        </button>
      </div>
      {validationResult?.message && (
        <p className={`mt-1 text-sm ${validationResult.valid ? 'text-green-600' : 'text-red-600'}`}>
          {validationResult.message}
        </p>
      )}
    </div>
  );
}
