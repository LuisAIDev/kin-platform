import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// Con globals:false, Testing Library no registra su limpieza automática;
// se limpia el DOM tras cada test para evitar fugas entre casos.
afterEach(() => {
  cleanup();
});

// jsdom no implementa URL.createObjectURL/revokeObjectURL; se aportan stubs
// para los tests de descarga (blobs).
if (typeof URL.createObjectURL !== "function") {
  Object.defineProperty(URL, "createObjectURL", {
    value: () => "blob:mock",
    configurable: true,
  });
  Object.defineProperty(URL, "revokeObjectURL", {
    value: () => undefined,
    configurable: true,
  });
}
