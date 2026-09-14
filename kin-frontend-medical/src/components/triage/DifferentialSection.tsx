"use client";

import { useState } from "react";
import {
  differentialService,
  type DifferentialResponse,
} from "@/services/differential";

const SEVERITY_COLORS: Record<string, string> = {
  LEVE: "bg-emerald-100 text-emerald-800",
  MODERADO: "bg-amber-100 text-amber-800",
  GRAVE: "bg-red-100 text-red-800",
};

const URGENCY_COLORS: Record<string, string> = {
  BAJA: "bg-slate-100 text-slate-700",
  MEDIA: "bg-blue-100 text-blue-800",
  ALTA: "bg-orange-100 text-orange-800",
};

const RISK_FACTOR_OPTIONS = [
  "fumador",
  "ex-fumador",
  "diabetes",
  "hipertension",
  "obesidad",
  "dislipidemia",
  "enfermedad-cardiovascular-previa",
  "trombosis-previa",
  "anticoagulantes",
  "sedentarismo",
  "epoc",
  "asma",
  "apnea-sueno",
  "inmunodepresion",
  "vih",
  "cancer-activo",
  "hospitalizacion-reciente",
  "contacto-enfermos",
  "viaje-reciente",
  "enfermedad-renal-cronica",
  "enfermedad-hepatica",
  "embarazo",
  "postparto-lactancia",
  "anticonceptivos",
  "terapia-hormonal",
  "menopausia",
  "alcohol",
  "drogas-ilicitas",
  "estres-cronico",
  "dieta-inadecuada",
  "falta-sueno",
  "antecedentes-familiares-cv",
  "antecedentes-familiares-cancer",
  "antecedentes-familiares-diabetes",
  "edad-avanzada",
  "edad-pediatrica",
  "alergias-conocidas",
  "cirugia-reciente",
] as const;

const RISK_FACTOR_LABELS: Record<string, string> = {
  "fumador": "Fumador",
  "ex-fumador": "Ex-fumador",
  "diabetes": "Diabetes",
  "hipertension": "Hipertensión",
  "obesidad": "Obesidad",
  "dislipidemia": "Dislipidemia",
  "enfermedad-cardiovascular-previa": "Enfermedad cardiovascular previa",
  "trombosis-previa": "Trombosis previa",
  "anticoagulantes": "Anticoagulantes",
  "sedentarismo": "Sedentarismo",
  "epoc": "EPOC",
  "asma": "Asma",
  "apnea-sueno": "Apnea del sueño",
  "inmunodepresion": "Inmunodepresión",
  "vih": "VIH",
  "cancer-activo": "Cáncer activo",
  "hospitalizacion-reciente": "Hospitalización reciente",
  "contacto-enfermos": "Contacto con enfermos",
  "viaje-reciente": "Viaje reciente",
  "enfermedad-renal-cronica": "Enfermedad renal crónica",
  "enfermedad-hepatica": "Enfermedad hepática",
  "embarazo": "Embarazo",
  "postparto-lactancia": "Postparto / lactancia",
  "anticonceptivos": "Anticonceptivos",
  "terapia-hormonal": "Terapia hormonal",
  "menopausia": "Menopausia",
  "alcohol": "Alcohol",
  "drogas-ilicitas": "Drogas ilícitas",
  "estres-cronico": "Estrés crónico",
  "dieta-inadecuada": "Dieta inadecuada",
  "falta-sueno": "Falta de sueño",
  "antecedentes-familiares-cv": "Antecedentes familiares CV",
  "antecedentes-familiares-cancer": "Antecedentes familiares cáncer",
  "antecedentes-familiares-diabetes": "Antecedentes familiares diabetes",
  "edad-avanzada": "Edad avanzada",
  "edad-pediatrica": "Edad pediátrica",
  "alergias-conocidas": "Alergias conocidas",
  "cirugia-reciente": "Cirugía reciente",
};

const RISK_FACTOR_CATEGORIES: Record<string, string> = {
  "fumador": "cardiometabolicos",
  "ex-fumador": "cardiometabolicos",
  "diabetes": "cardiometabolicos",
  "hipertension": "cardiometabolicos",
  "obesidad": "cardiometabolicos",
  "dislipidemia": "cardiometabolicos",
  "enfermedad-cardiovascular-previa": "cardiometabolicos",
  "trombosis-previa": "cardiometabolicos",
  "anticoagulantes": "cardiometabolicos",
  "sedentarismo": "cardiometabolicos",
  "epoc": "respiratorios",
  "asma": "respiratorios",
  "apnea-sueno": "respiratorios",
  "inmunodepresion": "inmunologicos",
  "vih": "inmunologicos",
  "cancer-activo": "inmunologicos",
  "hospitalizacion-reciente": "inmunologicos",
  "contacto-enfermos": "inmunologicos",
  "viaje-reciente": "inmunologicos",
  "enfermedad-renal-cronica": "renales-hepaticos",
  "enfermedad-hepatica": "renales-hepaticos",
  "embarazo": "hormonales-reproductivos",
  "postparto-lactancia": "hormonales-reproductivos",
  "anticonceptivos": "hormonales-reproductivos",
  "terapia-hormonal": "hormonales-reproductivos",
  "menopausia": "hormonales-reproductivos",
  "alcohol": "estilo-vida",
  "drogas-ilicitas": "estilo-vida",
  "estres-cronico": "estilo-vida",
  "dieta-inadecuada": "estilo-vida",
  "falta-sueno": "estilo-vida",
  "antecedentes-familiares-cv": "familiares",
  "antecedentes-familiares-cancer": "familiares",
  "antecedentes-familiares-diabetes": "familiares",
  "edad-avanzada": "otros",
  "edad-pediatrica": "otros",
  "alergias-conocidas": "otros",
  "cirugia-reciente": "otros",
};

const CATEGORY_LABELS: Record<string, string> = {
  cardiometabolicos: "Cardiometabólicos",
  respiratorios: "Respiratorios",
  inmunologicos: "Inmunológicos e infecciosos",
  "renales-hepaticos": "Renales y hepáticos",
  "hormonales-reproductivos": "Hormonales y reproductivos",
  "estilo-vida": "Estilo de vida",
  familiares: "Familiares",
  otros: "Otros",
};

const CATEGORY_ORDER = [
  "cardiometabolicos",
  "respiratorios",
  "inmunologicos",
  "renales-hepaticos",
  "hormonales-reproductivos",
  "estilo-vida",
  "familiares",
  "otros",
] as const;

export default function DifferentialSection({
  symptoms,
}: {
  symptoms: string[];
}) {
  const [riskFactors, setRiskFactors] = useState<Set<string>>(new Set());
  const [result, setResult] = useState<DifferentialResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const toggleRisk = (factor: string) => {
    setRiskFactors((prev) => {
      const next = new Set(prev);
      if (next.has(factor)) next.delete(factor);
      else next.add(factor);
      return next;
    });
  };

  const handleDifferential = async () => {
    setLoading(true);
    setError("");
    try {
      const response = await differentialService.analyze(symptoms, Array.from(riskFactors));
      setResult(response);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  if (symptoms.length === 0) return null;

  return (
    <section className="flex flex-col gap-4 border-t border-neutral-200 pt-6">
      <div>
        <h2 className="text-lg font-semibold">Diagnóstico diferencial</h2>
        <p className="text-sm text-neutral-700 mt-1">
          Análisis más elaborado de las posibles condiciones con factores de
          riesgo y pruebas sugeridas.
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-sm font-medium">Factores de riesgo (opcional)</p>
        <div className="flex flex-col gap-3">
          {CATEGORY_ORDER.map((categoryKey) => {
            const factorsInCategory = RISK_FACTOR_OPTIONS.filter(
              (f) => RISK_FACTOR_CATEGORIES[f] === categoryKey
            );
            if (factorsInCategory.length === 0) return null;
            return (
              <div key={categoryKey} className="flex flex-col gap-1.5">
                <p className="text-xs font-semibold text-neutral-600 uppercase tracking-wide">
                  {CATEGORY_LABELS[categoryKey]}
                </p>
                <div className="flex flex-wrap gap-2">
                  {factorsInCategory.map((factor) => {
                    const isSelected = riskFactors.has(factor);
                    return (
                      <button
                        key={factor}
                        type="button"
                        onClick={() => toggleRisk(factor)}
                        aria-pressed={isSelected}
                        className={`rounded-full border px-3 py-1.5 text-xs font-medium transition ${
                          isSelected
                            ? "bg-indigo-600 border-indigo-600 text-white"
                            : "border-neutral-300 text-neutral-700 hover:bg-indigo-50 hover:border-indigo-400"
                        }`}
                      >
                        {RISK_FACTOR_LABELS[factor] || factor}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={handleDifferential}
          disabled={loading}
          className="rounded-lg bg-indigo-600 px-6 py-2.5 text-sm font-medium text-white hover:bg-indigo-700 transition disabled:bg-indigo-300"
        >
          {loading ? "Analizando..." : "Analizar diagnóstico diferencial"}
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
      )}

      {result && <Results result={result} />}
    </section>
  );
}

function Results({ result }: { result: DifferentialResponse }) {
  if (result.items.length === 0) {
    return (
      <div className="rounded-lg border border-neutral-200 px-5 py-6 text-center text-sm text-neutral-700">
        No se pudo construir un diagnóstico diferencial con los síntomas
        seleccionados.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      {result.items.map((item) => (
        <article
          key={item.conditionId}
          className="rounded-xl border border-neutral-200 p-5 flex flex-col gap-3"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-base font-semibold">{item.condition}</h3>
              {item.description && (
                <p className="text-sm text-neutral-600 mt-0.5">{item.description}</p>
              )}
            </div>
            <span className="text-lg font-bold text-indigo-700 shrink-0">
              {Math.round(item.probability * 100)}%
            </span>
          </div>

          <div className="w-full h-2 rounded-full bg-neutral-100 overflow-hidden">
            <div
              className="h-full bg-indigo-600"
              style={{ width: `${Math.round(item.probability * 100)}%` }}
            />
          </div>

          <div className="flex flex-wrap gap-2 text-xs">
            <span className={`rounded-full px-2.5 py-1 font-medium ${SEVERITY_COLORS[item.severity]}`}>
              Severidad: {item.severity}
            </span>
            <span className={`rounded-full px-2.5 py-1 font-medium ${URGENCY_COLORS[item.urgency]}`}>
              Urgencia: {item.urgency}
            </span>
            <ExternalLinks condition={item.condition} />
          </div>

          {item.reasoning && (
            <p className="text-sm text-neutral-600">{item.reasoning}</p>
          )}

          {item.riskFactors.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <p className="text-xs font-semibold uppercase text-neutral-600">
                Factores de riesgo
              </p>
              <div className="flex flex-wrap gap-2">
                {item.riskFactors.map((rf) => (
                  <span
                    key={rf.factor}
                    className="rounded-full bg-indigo-50 text-indigo-700 px-2.5 py-1 text-xs font-medium"
                  >
                    {rf.factor}
                    {rf.description ? ` · ${rf.description}` : ""}
                  </span>
                ))}
              </div>
            </div>
          )}

          {item.recommendedTests.length > 0 && (
            <div className="flex flex-col gap-1.5">
              <p className="text-xs font-semibold uppercase text-neutral-600">
                Pruebas sugeridas
              </p>
              <ul className="flex flex-col gap-1">
                {item.recommendedTests.map((test) => (
                  <li key={test.test} className="text-sm text-neutral-600">
                    • {test.test}
                    {test.description ? ` — ${test.description}` : ""}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </article>
      ))}
      <p className="text-xs text-neutral-600">{result.disclaimer}</p>
    </div>
  );
}

/** Enlaces a información externa (fuentes médicas confiables) por condición. */
function ExternalLinks({ condition }: { condition: string }) {
  const encodedCondition = encodeURIComponent(condition);

  const links = [
    {
      label: "MedlinePlus",
      url: `https://vsearch.nlm.nih.gov/vivisimo/cgi-bin/query-meta?v%3Aproject=medlineplus&v%3Asources=medlineplus-bundle&query=${encodedCondition}`,
    },
    {
      label: "Mayo Clinic",
      url: `https://www.mayoclinic.org/es/search?query=${encodedCondition}`,
    },
    {
      label: "MSD Manuals",
      url: `https://www.msdmanuals.com/es/hogar/searchresults?query=${encodedCondition}`,
    },
    {
      label: "CDC",
      url: `https://www.cdc.gov/spanish/enfermedades/index.html`,
    },
    {
      label: "OMS",
      url: `https://www.who.int/es/health-topics`,
    },
    {
      label: "AAFP",
      url: `https://www.aafp.org/family-physician/patient-care/conditions-diseases.html`,
    },
  ];

  return (
    <div className="flex flex-wrap gap-2 mt-2">
      {links.map((link) => (
        <a
          key={link.label}
          href={link.url}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1 rounded-md bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-700 hover:bg-neutral-200 transition"
        >
          {link.label}
        </a>
      ))}
    </div>
  );
}
