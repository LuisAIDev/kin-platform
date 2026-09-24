"use client";

import { useEffect, useState } from "react";
import { useDebounce } from "../../hooks/useDebounce";

type CupsSuggestion = {
  cupsCode: string;
  cupsDescription: string;
  cupsCategory: string;
};

export function CupsAutocomplete({
  value,
  onChange,
  placeholder = "Buscar CUPS por código o descripción…",
}: {
  value: string;
  onChange: (code: string, description: string) => void;
  placeholder?: string;
}) {
  const [query, setQuery] = useState(value);
  const [suggestions, setSuggestions] = useState<CupsSuggestion[]>([]);
  const [showList, setShowList] = useState(false);
  const [loading, setLoading] = useState(false);

  const debounced = useDebounce(query, 300);

  useEffect(() => {
    if (debounced.length < 2) {
      setSuggestions([]);
      return;
    }
    setLoading(true);
    fetch(`/api/v1/catalogs/cups/search?q=${encodeURIComponent(debounced)}&limit=15`, {
      credentials: "include",
    })
      .then((r) => (r.ok ? r.json() : []))
      .then(setSuggestions)
      .catch(() => setSuggestions([]))
      .finally(() => setLoading(false));
  }, [debounced]);

  function select(s: CupsSuggestion) {
    setQuery(`${s.cupsCode} - ${s.cupsDescription}`);
    onChange(s.cupsCode, s.cupsDescription);
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
          {suggestions.map((s) => (
            <li
              key={s.cupsCode}
              onClick={() => select(s)}
              className="cursor-pointer px-4 py-2 hover:bg-neutral-100"
            >
              <div className="font-mono text-sm text-medical-600">{s.cupsCode}</div>
              <div className="text-sm text-neutral-800">{s.cupsDescription}</div>
              <div className="text-xs text-neutral-500">{s.cupsCategory}</div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
