"use client";

const FEEDBACK_URL = process.env.NEXT_PUBLIC_FEEDBACK_URL;

export default function FeedbackButton({ label = "Enviar feedback" }: { label?: string }) {
  const href = FEEDBACK_URL && FEEDBACK_URL.length > 0 ? FEEDBACK_URL : "mailto:soporte@kin-platform.com?subject=Feedback%20piloto%20KIN%20Health";

  return (
    <a
      href={href}
      target={FEEDBACK_URL && FEEDBACK_URL.length > 0 ? "_blank" : undefined}
      rel="noopener noreferrer"
      className="inline-flex items-center gap-1.5 rounded-lg border border-neutral-200 bg-white px-3 py-1.5 text-sm font-medium text-neutral-700 shadow-sm hover:border-primary-300 hover:text-primary-700"
    >
      <span aria-hidden="true">💬</span>
      {label}
    </a>
  );
}
