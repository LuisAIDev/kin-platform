"use client";

import { useEffect, useState } from "react";
import { useDebounce } from "../../hooks/useDebounce";

type InvimaSuggestion = {
  cumCode: string | null;
  commercialName: string;
  genericName: string | null;
  pharmaceuticalForm: string | null;
  concentration: string | null;
};

export function InvimaAutocomplete({
  value,
  onChange,
  placeholder = "Buscar medicamento por nombre comercial o genérico…",
}: {
  value: string;
  onChange: (cumCode: string | null, commercialName: string) => void;
  placeholder?: string;
}) {
  const [query, setQuery] = useState(value);
  const [suggestions, setSuggestions] = useState<InvimaSuggestion[]>([]);
  const [showList, setShowList] = useState(false);
  const [loading, setLoading] = useState(false);

  const debounced = useDebounce(query, 300);

  useEffect(() => {
    if (debounced.length < 2) {
      setSuggestions([]);
      return;
    }
    setLoading(true);
    fetch(`/api/v1/catalogs/invima/search?q=${encodeURIComponent(debounced)}&limit=15`, {
      credentials: "include",
    })
      .then((r) => (r.ok ? r.json() : []))
      .then(setSuggestions)
      .catch(() => setSuggestions([]))
      .finally(() => setLoading(false));
  }, [debounced]);

  function select(s: InvimaSuggestion) {
    setQuery(s.commercialName);
    onChange(s.cumCode, s.commercialName);
    setShowList(false);
  }

  return (
    <div className="relative">
      <input
        type="text"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setShowList(true);
        }}
        onFocus={() => setShowList(true)}
        placeholder={placeholder}
        className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-neutral-900 focus:border-medical-500 focus:ring-2 focus:ring-medical-100"
      />
      {loading && <span className="absolute right-3 top-2 text-xs text-neutral-400">…</span>}
      {showList && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 max-h-80 w-full overflow-y-auto rounded-md border border-neutral-200 bg-white shadow-lg">
          {suggestions.map((s, idx) => (
            <li
              key={`${s.cumCode ?? s.commercialName}-${idx}`}
              onClick={() => select(s)}
              className="cursor-pointer px-4 py-2 hover:bg-neutral-100"
            >
              <div className="text-sm font-medium text-neutral-800">{s.commercialName}</div>
              <div className="text-xs text-neutral-500">
                {[s.genericName, s.concentration, s.pharmaceuticalForm].filter(Boolean).join(" · ")}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
