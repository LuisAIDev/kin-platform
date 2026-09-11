'use client';

import { useState, useEffect } from 'react';
import { CheckCircle2, XCircle } from 'lucide-react';

interface ValidatedInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  validation?: (value: string) => { valid: boolean; message?: string };
}

export function ValidatedInput({ label, validation, className, ...props }: ValidatedInputProps) {
  const [value, setValue] = useState(props.value as string || '');
  const [validationResult, setValidationResult] = useState<{ valid: boolean; message?: string } | null>(null);

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
          {...props}
          value={value}
          onChange={(e) => {
            setValue(e.target.value);
            props.onChange?.(e);
          }}
          className={`w-full px-4 py-3 pr-12 rounded-lg border border-neutral-300 text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:ring-2 transition-colors disabled:bg-neutral-100 disabled:cursor-not-allowed ${
            validationResult?.valid === true
              ? 'border-green-500 focus:ring-green-500'
              : validationResult?.valid === false
              ? 'border-red-500 focus:ring-red-500'
              : 'focus:ring-medical-500'
          } ${className || ''}`}
        />
        {validationResult && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            {validationResult.valid ? (
              <CheckCircle2 className="h-5 w-5 text-green-500" />
            ) : (
              <XCircle className="h-5 w-5 text-red-500" />
            )}
          </div>
        )}
      </div>
      {validationResult?.message && (
        <p className={`mt-1 text-sm ${validationResult.valid ? 'text-green-600' : 'text-red-600'}`}>
          {validationResult.message}
        </p>
      )}
    </div>
  );
}
