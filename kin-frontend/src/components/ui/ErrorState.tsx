"use client";

type Props = {
  message: string;
  onRetry?: () => void;
};

export default function ErrorState({ message, onRetry }: Props) {
  return (
    <div role="alert" className="flex flex-col items-center justify-center py-16 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-2xl" aria-hidden="true">
        ⚠️
      </div>
      <p className="mt-4 text-sm font-medium text-neutral-900">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="mt-6 rounded-lg border border-neutral-300 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50 transition min-h-11"
        >
          Reintentar
        </button>
      )}
    </div>
  );
}
