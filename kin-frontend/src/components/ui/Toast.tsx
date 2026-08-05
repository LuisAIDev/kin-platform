"use client";

export type ToastType = "success" | "error" | "info";

export type ToastItem = {
  id: string;
  type: ToastType;
  message: string;
};

export default function Toast({ toast }: { toast: ToastItem }) {
  const styles: Record<ToastType, string> = {
    success: "bg-emerald-600 text-white",
    error: "bg-red-600 text-white",
    info: "bg-neutral-800 text-white",
  };
  const role = toast.type === "error" ? "alert" : "status";

  return (
    <div
      role={role}
      aria-live={toast.type === "error" ? "assertive" : "polite"}
      className={`pointer-events-auto flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium shadow-lg ${styles[toast.type]}`}
    >
      <span aria-hidden="true">
        {toast.type === "success" ? "✓" : toast.type === "error" ? "✕" : "ℹ"}
      </span>
      {toast.message}
    </div>
  );
}
