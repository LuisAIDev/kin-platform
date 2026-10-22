import { api } from "./api";

export const investmentMemoService = {
  get: (projectId: string) =>
    api.getText(`/projects/${encodeURIComponent(projectId)}/investment-memo`),
};
