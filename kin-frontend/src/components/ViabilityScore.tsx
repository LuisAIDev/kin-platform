"use client";

import { useEffect, useState } from "react";

type Props = {
  score: number;
};

function getColor(score: number): string {
  if (score >= 70) return "#22c55e";
  if (score >= 40) return "#f59e0b";
  return "#ef4444";
}

function getLabel(score: number): string {
  if (score >= 70) return "Alto";
  if (score >= 40) return "Medio";
  return "Bajo";
}

export default function ViabilityScore({ score }: Props) {
  const radius = 52;
  const circumference = 2 * Math.PI * radius;
  const target = circumference - (score / 100) * circumference;
  const [offset, setOffset] = useState(circumference);
  const color = getColor(score);

  useEffect(() => {
    const timer = setTimeout(() => setOffset(target), 80);
    return () => clearTimeout(timer);
  }, [target]);

  return (
    <div className="flex flex-col items-center my-4">
      <svg width="150" height="150" viewBox="0 0 150 150">
        <circle
          cx="75" cy="75" r={radius}
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="10"
        />
        <circle
          cx="75" cy="75" r={radius}
          fill="none"
          stroke={color}
          strokeWidth="10"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform="rotate(-90 75 75)"
          style={{ transition: "stroke-dashoffset 1.4s cubic-bezier(0.4, 0, 0.2, 1)" }}
        />
        <text
          x="75" y="68"
          textAnchor="middle"
          dominantBaseline="central"
          fill={color}
          style={{ fontSize: 32, fontWeight: 700, fontFamily: "inherit" }}
        >
          {score}
        </text>
        <text
          x="75" y="94"
          textAnchor="middle"
          dominantBaseline="central"
          fill="#a3a3a3"
          style={{ fontSize: 13, fontWeight: 500, fontFamily: "inherit" }}
        >
          / 100
        </text>
      </svg>
      <span
        className="mt-1 text-xs font-semibold tracking-wide uppercase"
        style={{ color }}
      >
        {getLabel(score)}
      </span>
    </div>
  );
}
