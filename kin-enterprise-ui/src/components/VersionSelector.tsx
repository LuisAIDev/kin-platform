import type { EnterpriseVersion } from "../types/enterprise";

interface VersionSelectorProps {
  /** Versiones disponibles (ascendentes). */
  versions: EnterpriseVersion[];
  /** Versión seleccionada. */
  selected: number;
  /** Callback al cambiar la versión. */
  onSelect: (version: number) => void;
}

/** Selector de versión del proyecto empresarial. */
export function VersionSelector({
  versions,
  selected,
  onSelect,
}: VersionSelectorProps) {
  return (
    <label htmlFor="version-select">
      <span className="card-title">Versión</span>
      <select
        id="version-select"
        className="version-select"
        value={selected}
        onChange={(event) => onSelect(Number(event.target.value))}
      >
        {versions.map((version) => (
          <option key={version.version} value={version.version}>
            v{version.version} · {version.status}
          </option>
        ))}
      </select>
    </label>
  );
}
