import type { MetadataRoute } from "next";

const SITE_URL = "https://www.kin-platform.com";

type RouteConfig = {
  path: string;
  changeFrequency: MetadataRoute.Sitemap[number]["changeFrequency"];
  priority: number;
};

const PUBLIC_ROUTES: RouteConfig[] = [
  { path: "/", changeFrequency: "weekly", priority: 1 },
  { path: "/sobre-kin", changeFrequency: "monthly", priority: 0.8 },
  { path: "/arquitectura", changeFrequency: "monthly", priority: 0.8 },
  { path: "/register", changeFrequency: "monthly", priority: 0.6 },
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
