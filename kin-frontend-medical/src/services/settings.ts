export type Theme = "light" | "dark" | "system";
export type Language = "es" | "en";
export type AiProvider = "auto" | "deepseek" | "openai";
export type AiLength = "short" | "balanced" | "long";

export interface UserSettings {
  displayName: string;
  theme: Theme;
  language: Language;
  aiLevel: "FLASH" | "PRO";
  aiProvider: AiProvider;
  temperature: number;
  aiLength: AiLength;
  notificationsEnabled: boolean;
}

export const DEFAULT_SETTINGS: UserSettings = {
  displayName: "",
  theme: "system",
  language: "es",
  aiLevel: "FLASH",
  aiProvider: "auto",
  temperature: 0.7,
  aiLength: "balanced",
  notificationsEnabled: true,
};

const STORAGE_KEY = "kin_user_settings_v1";

function merge(base: UserSettings, raw: unknown): UserSettings {
  const value = (raw ?? {}) as Partial<UserSettings>;
  return {
    displayName: typeof value.displayName === "string" ? value.displayName : base.displayName,
    theme: value.theme === "light" || value.theme === "dark" || value.theme === "system"
      ? value.theme : base.theme,
    language: value.language === "es" || value.language === "en" ? value.language : base.language,
    aiLevel: value.aiLevel === "FLASH" || value.aiLevel === "PRO" ? value.aiLevel : base.aiLevel,
    aiProvider: value.aiProvider === "auto" || value.aiProvider === "deepseek"
      || value.aiProvider === "openai" ? value.aiProvider : base.aiProvider,
    temperature: typeof value.temperature === "number"
      ? Math.min(1, Math.max(0, value.temperature)) : base.temperature,
    aiLength: value.aiLength === "short" || value.aiLength === "balanced"
      || value.aiLength === "long" ? value.aiLength : base.aiLength,
    notificationsEnabled: typeof value.notificationsEnabled === "boolean"
      ? value.notificationsEnabled : base.notificationsEnabled,
  };
}

export const settingsService = {
  load(): UserSettings {
    if (typeof window === "undefined") return DEFAULT_SETTINGS;
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      return merge(DEFAULT_SETTINGS, raw ? JSON.parse(raw) : null);
    } catch {
      return DEFAULT_SETTINGS;
    }
  },

  save(settings: UserSettings): void {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch {
      // almacenamiento no disponible
    }
  },
};
