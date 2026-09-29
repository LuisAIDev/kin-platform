import nextCoreWebVitals from "eslint-config-next/core-web-vitals";

// Migración a flat config (ESLint 9). `eslint-config-next` v16 ya exporta un
// flat config nativo en `eslint-config-next/core-web-vitals`, por lo que se
// reutiliza el preset histórico del proyecto sin FlatCompat (que rompía con
// "Converting circular structure to JSON").
const config = [
  ...nextCoreWebVitals,
  {
    rules: {
      "react/no-unescaped-entities": "off",
      // Reglas NUEVAS de eslint-plugin-react-hooks v7 (no existían antes del
      // upgrade a ESLint 9). Se marcan como warning para no romper el gate con
      // deuda preexistente en ~20 archivos; refactor pendiente en TD-FE-LINT-1.
      "react-hooks/set-state-in-effect": "warn",
      "react-hooks/immutability": "warn",
    },
  },
  {
    ignores: ["node_modules/**", ".next/**", "dist/**", "coverage/**", ".probe-clean/**"],
  },
];

export default config;
