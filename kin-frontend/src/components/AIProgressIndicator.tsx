"use client";

import { useState, useEffect } from "react";

interface AIProgressIndicatorProps {
  visible: boolean;
  onComplete?: () => void;
}

export default function AIProgressIndicator({ visible, onComplete }: AIProgressIndicatorProps) {
  const [percentage, setPercentage] = useState(0);
  const [active, setActive] = useState(false);

  useEffect(() => {
    if (!visible) {
      setActive(false);
      return;
    }
    setActive(true);
    let progress: NodeJS.Timeout;

    if (visible && active) {
      let width = 0;
      const interval = setInterval(() => {
        width += 2;
        setPercentage(width);
        if (width >= 100) {
          clearInterval(interval);
          setPercentage(100);
          if (onComplete) {
            onComplete();
          }
        }
      }, 30);
    }

    return () => {
      clearInterval(interval);
    };
  }, [visible, active, onComplete]);

  if (!active) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black/30" onClick={() => {}} />
      <div className="relative z-10 w-full max-w-md mx-4 rounded-2xl border border-neutral-200 bg-white p-8 shadow-xl text-center">
        <div className="mb-4">
          <svg
            width="48"
            height="48"
            viewBox="0 0 48 48"
            className="mx-auto mb-3 text-primary"
          >
            <circle
              cx="24"
              cy="24"
              r="18"
              fill="none"
              stroke="currentColor"
              strokeWidth="5"
              strokeLinecap="round"
            />
            <path
              d="M13.3 2.6A12.95 12.95 0 0 1 12 8l-6.7 6.7"
              stroke="currentColor"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <line
              x1="12"
              y1="3"
              x2="12"
              y2="15"
              stroke="currentColor"
              strokeWidth="3"
              strokeLinecap="round"
            />
          </svg>
        </div>
        <p className="text-sm text-neutral-500 mb-2">Procesando solicitud de IA</p>
        <div className="bg-neutral-200 rounded-full h-2.5 w-full overflow-hidden">
          <div
            className="bg-primary h-2.5 rounded-full transition-all duration-500 w-[0%]"
            style={{ width: `${percentage}%` }}
          ></div>
        </div>
        <p className="mt-2 text-xs text-neutral-500">{percentage}%</p>
      </div>
    </div>
  );
}