"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { projectsService } from "@/services/projects";

const CATEGORIES = [
  { value: "PERSONAL", label: "Personal" },
  { value: "EMPRENDIMIENTO", label: "Emprendimiento" },
  { value: "EMPRESARIAL", label: "Empresarial" },
  { value: "SOCIAL", label: "Social" },
] as const;

export default function NewProjectPage() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (saving) return;

    setError("");
    setSaving(true);

    try {
      const project = await projectsService.create({
        title: title.trim(),
        description: description.trim() || undefined,
        category,
      });
      router.push(`/dashboard/projects/${project.id}`);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-12">
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-lg flex flex-col gap-5"
      >
        <div>
          <h1 className="text-2xl font-bold">Nuevo proyecto</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Cuéntanos sobre tu idea y KIN te guiará para estructurarla.
          </p>
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">
            {error}
          </p>
        )}

        <div className="flex flex-col gap-1.5">
          <label htmlFor="title" className="text-sm font-medium">
            Título
          </label>
          <input
            id="title"
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Ej: App de gestión financiera"
            required
            maxLength={255}
            className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-neutral-900"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="description" className="text-sm font-medium">
            Descripción
          </label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe brevemente tu proyecto..."
            rows={4}
            maxLength={5000}
            className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-neutral-900 resize-none"
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="category" className="text-sm font-medium">
            Categoría
          </label>
          <select
            id="category"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            required
            className="rounded-lg border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-neutral-900 bg-white"
          >
            <option value="" disabled>
              Selecciona una categoría
            </option>
            {CATEGORIES.map((cat) => (
              <option key={cat.value} value={cat.value}>
                {cat.label}
              </option>
            ))}
          </select>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={() => router.back()}
            className="rounded-lg border border-neutral-300 px-5 py-2.5 text-sm font-medium text-neutral-700 hover:bg-neutral-100 transition"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={saving || !title.trim() || !category}
            className="rounded-lg bg-neutral-900 px-6 py-2.5 text-sm font-medium text-white hover:bg-neutral-800 transition disabled:opacity-50"
          >
            {saving ? "Creando..." : "Crear proyecto"}
          </button>
        </div>
      </form>
    </main>
  );
}
