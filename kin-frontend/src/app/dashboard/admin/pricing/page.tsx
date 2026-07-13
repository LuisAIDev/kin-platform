"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth";
import { pricingService, PricingPlan } from "@/services/pricing";

export default function AdminPricingPage() {
  const router = useRouter();
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState<string | null>(null);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [edits, setEdits] = useState<Record<string, Partial<PricingPlan>>>({});

  useEffect(() => {
    const user = authService.getUser();
    if (!user || user.role !== "ADMIN") {
      router.replace("/dashboard");
      return;
    }

    pricingService
      .getAll()
      .then(setPlans)
      .catch(() => setMessage({ type: "error", text: "Error al cargar los planes" }))
      .finally(() => setLoading(false));
  }, [router]);

  const handleChange = (id: string, field: string, value: string | number | boolean | string[]) => {
    setEdits((prev) => ({
      ...prev,
      [id]: { ...prev[id], [field]: value },
    }));
  };

  const handleSave = async (plan: PricingPlan) => {
    setSaving(plan.id);
    setMessage(null);
    const edit = edits[plan.id];
    if (!edit) return;

    try {
      const updated = await pricingService.update(plan.id, {
        name: (edit.name ?? plan.name) as string,
        price: (edit.price ?? plan.price) as number,
        currency: (edit.currency ?? plan.currency) as string,
        billingPeriod: (edit.billingPeriod ?? plan.billingPeriod) as string,
        features: (edit.features ?? plan.features) as string[],
        isPopular: (edit.isPopular ?? plan.isPopular) as boolean,
        displayOrder: (edit.displayOrder ?? plan.displayOrder) as number,
      });
      setPlans((prev) => prev.map((p) => (p.id === plan.id ? updated : p)));
      setEdits((prev) => {
        const next = { ...prev };
        delete next[plan.id];
        return next;
      });
      setMessage({ type: "success", text: "Plan actualizado correctamente" });
    } catch {
      setMessage({ type: "error", text: "Error al actualizar el plan" });
    } finally {
      setSaving(null);
    }
  };

  const getEditValue = (plan: PricingPlan, field: keyof PricingPlan) => {
    return edits[plan.id]?.[field] ?? plan[field];
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh] text-neutral-500">
        Cargando planes...
      </div>
    );
  }

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-neutral-900">Administrar Precios</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Edita los planes de precios que se muestran en la landing page.
        </p>
      </div>

      {message && (
        <div
          className={`mb-6 rounded-lg px-4 py-3 text-sm font-medium ${
            message.type === "success"
              ? "bg-green-50 text-green-700 border border-green-200"
              : "bg-red-50 text-red-700 border border-red-200"
          }`}
        >
          {message.text}
        </div>
      )}

      <div className="space-y-8">
        {plans.map((plan) => {
          const isSaving = saving === plan.id;
          return (
            <div
              key={plan.id}
              className="rounded-xl border border-neutral-200 bg-white p-6 shadow-sm"
            >
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                    Nombre
                  </label>
                  <input
                    type="text"
                    value={getEditValue(plan, "name") as string}
                    onChange={(e) => handleChange(plan.id, "name", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                    Precio
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={getEditValue(plan, "price") as number}
                    onChange={(e) => handleChange(plan.id, "price", parseFloat(e.target.value) || 0)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                    Moneda
                  </label>
                  <input
                    type="text"
                    maxLength={3}
                    value={getEditValue(plan, "currency") as string}
                    onChange={(e) => handleChange(plan.id, "currency", e.target.value.toUpperCase())}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                    Período de facturación
                  </label>
                  <input
                    type="text"
                    value={getEditValue(plan, "billingPeriod") as string}
                    onChange={(e) => handleChange(plan.id, "billingPeriod", e.target.value)}
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                    Orden
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={getEditValue(plan, "displayOrder") as number}
                    onChange={(e) =>
                      handleChange(plan.id, "displayOrder", parseInt(e.target.value) || 0)
                    }
                    className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                  />
                </div>

                <div className="flex items-center gap-3 pt-6">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={getEditValue(plan, "isPopular") as boolean}
                      onChange={(e) => handleChange(plan.id, "isPopular", e.target.checked)}
                      className="h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-primary-500"
                    />
                    <span className="text-sm text-neutral-700">Plan popular</span>
                  </label>
                </div>
              </div>

              <div className="mt-4">
                <label className="block text-xs font-semibold text-neutral-500 uppercase tracking-wider mb-1">
                  Características (una por línea)
                </label>
                <textarea
                  rows={4}
                  value={(getEditValue(plan, "features") as string[]).join("\n")}
                  onChange={(e) =>
                    handleChange(
                      plan.id,
                      "features",
                      e.target.value.split("\n").filter((f) => f.trim())
                    )
                  }
                  className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 min-h-11"
                />
              </div>

              <div className="mt-4 flex justify-end">
                <button
                  onClick={() => handleSave(plan)}
                  disabled={isSaving || !edits[plan.id]}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-500 disabled:opacity-50 disabled:cursor-not-allowed transition min-h-11"
                >
                  {isSaving ? "Guardando..." : "Guardar cambios"}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
