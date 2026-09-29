"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

/**
 * Providers de cliente para toda la app (App Router). El QueryClient se crea
 * una sola vez por sesión de render (useState) para no perder la caché entre
 * re-renders. Necesario para los hooks de React Query del wizard HCE
 * (useEncounter, useAnamnesis, ...).
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
