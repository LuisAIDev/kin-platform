import type { MetadataRoute } from "next";

const SITE_URL = "https://www.kin-platform-medical.com";

type RouteConfig = {
  path: string;
  changeFrequency: MetadataRoute.Sitemap[number]["changeFrequency"];
  priority: number;
};

const PUBLIC_ROUTES: RouteConfig[] = [
  { path: "/", changeFrequency: "weekly", priority: 1 },
  { path: "/register/salud/medico", changeFrequency: "monthly", priority: 0.9 },
  { path: "/register/salud/paciente", changeFrequency: "monthly", priority: 0.8 },
  { path: "/register", changeFrequency: "monthly", priority: 0.7 },
  { path: "/login", changeFrequency: "yearly", priority: 0.3 },
];

export default function sitemap(): MetadataRoute.Sitemap {
  const lastModified = new Date();

  return PUBLIC_ROUTES.map((route) => ({
    url: `${SITE_URL}${route.path}`,
    lastModified,
    changeFrequency: route.changeFrequency,
    priority: route.priority,
  }));
}
