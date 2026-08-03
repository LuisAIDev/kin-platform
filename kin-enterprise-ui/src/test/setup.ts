import "@testing-library/jest-dom/vitest";

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
