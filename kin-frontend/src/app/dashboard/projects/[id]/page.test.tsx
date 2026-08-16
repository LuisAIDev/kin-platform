import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Suspense } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ProjectDetailPage from "./page";
import { chatService } from "@/services/chat";
import { projectsService } from "@/services/projects";
import { projectInfoService } from "@/services/projectInfo";
import type { Project } from "@/services/projects";
import { MAX_CHAT_MESSAGE_LENGTH } from "@/utils/chatLimits";

const mockPush = vi.fn();
const mockRouter = { push: mockPush };

vi.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
}));

vi.mock("@/services/projects", async () => {
  const actual =
    await vi.importActual<typeof import("@/services/projects")>("@/services/projects");
  return { ...actual, projectsService: { ...actual.projectsService, getById: vi.fn() } };
});

vi.mock("@/services/chat", async () => {
  const actual = await vi.importActual<typeof import("@/services/chat")>("@/services/chat");
  return {
    ...actual,
    chatService: {
      ...actual.chatService,
      getHistory: vi.fn(),
      sendMessageStream: vi.fn(),
    },
  };
});

vi.mock("@/services/projectInfo", async () => {
  const actual =
    await vi.importActual<typeof import("@/services/projectInfo")>("@/services/projectInfo");
  return {
    ...actual,
    projectInfoService: {
      ...actual.projectInfoService,
      listInfo: vi.fn(),
      listDocuments: vi.fn(),
      saveInfo: vi.fn(),
      uploadDocument: vi.fn(),
    },
  };
});

const project: Project = {
  id: "p1",
  userId: "u1",
  title: "Proyecto Test",
  description: "Descripción del proyecto",
  category: "TECH",
  categoryName: "Tecnología",
  categoryColor: null,
  status: "ACTIVE",
  viabilityScore: null,
  aiSummary: null,
  startedAt: null,
  completedAt: null,
  createdAt: "2026-08-01T00:00:00Z",
  updatedAt: "2026-08-01T00:00:00Z",
  progressPercentage: null,
};

async function renderPage() {
  await act(async () => {
    render(
      <Suspense fallback={null}>
        <ProjectDetailPage params={Promise.resolve({ id: "p1" })} />
      </Suspense>,
    );
  });
}

async function loadPage() {
  await renderPage();
  await screen.findByText("Proyecto Test");
}

describe("ProjectDetailPage — protección del chat (10.000 caracteres)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPush.mockReset();
    Element.prototype.scrollIntoView = vi.fn();
    localStorage.setItem("kin_token_v2", "tok");
    vi.mocked(projectsService.getById).mockResolvedValue(project);
    vi.mocked(chatService.getHistory).mockResolvedValue([]);
    vi.mocked(chatService.sendMessageStream).mockReturnValue(new AbortController());
    vi.mocked(projectInfoService.listInfo).mockResolvedValue([]);
    vi.mocked(projectInfoService.listDocuments).mockResolvedValue([]);
  });

  it("el campo de chat tiene maxLength igual al límite del backend", async () => {
    await loadPage();
    const input = screen.getByPlaceholderText("Escribe tu mensaje...");
    expect(input).toHaveAttribute("maxlength", String(MAX_CHAT_MESSAGE_LENGTH));
  });

  it("muestra un contador de caracteres", async () => {
    await loadPage();
    expect(screen.getByTestId("chat-char-counter")).toHaveTextContent("0 / 10.000");
  });

  it("mensaje normal → envía el mensaje", async () => {
    const user = userEvent.setup();
    await loadPage();

    await user.type(screen.getByPlaceholderText("Escribe tu mensaje..."), "hola KIN");
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await waitFor(() =>
      expect(chatService.sendMessageStream).toHaveBeenCalledWith(
        "p1",
        "hola KIN",
        expect.anything(),
      ),
    );
  });

  it("mensaje de exactamente 10.000 caracteres → permitido", async () => {
    const user = userEvent.setup();
    await loadPage();
    const input = screen.getByPlaceholderText("Escribe tu mensaje...") as HTMLInputElement;

    fireEvent.change(input, { target: { value: "a".repeat(MAX_CHAT_MESSAGE_LENGTH) } });
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await waitFor(() =>
      expect(chatService.sendMessageStream).toHaveBeenCalledWith(
        "p1",
        "a".repeat(MAX_CHAT_MESSAGE_LENGTH),
        expect.anything(),
      ),
    );
  });

  it("bloquea un mensaje que supera 10.000 caracteres sin perder el contenido", async () => {
    const user = userEvent.setup();
    await loadPage();
    const input = screen.getByPlaceholderText("Escribe tu mensaje...") as HTMLInputElement;

    fireEvent.change(input, { target: { value: "a".repeat(MAX_CHAT_MESSAGE_LENGTH + 1) } });
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    expect(chatService.sendMessageStream).not.toHaveBeenCalled();
    expect(screen.getByTestId("chat-limit-error")).toHaveTextContent(
      /supera el límite de 10000 caracteres/,
    );
    expect(input.value).toHaveLength(MAX_CHAT_MESSAGE_LENGTH + 1);
  });

  it("error después de tokens conserva el contenido parcial (no muestra el mensaje fijo)", async () => {
    let onToken: ((token: string) => void) | undefined;
    let onError: ((error: Error) => void) | undefined;
    vi.mocked(chatService.sendMessageStream).mockImplementation((_p, _c, callbacks) => {
      onToken = callbacks.onToken;
      onError = callbacks.onError;
      return new AbortController();
    });

    const user = userEvent.setup();
    await loadPage();

    await user.type(screen.getByPlaceholderText("Escribe tu mensaje..."), "hola KIN");
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await waitFor(() => expect(onToken).toBeDefined());
    act(() => {
      onToken!("respuesta parcial ");
      onToken!("de DeepSeek");
    });

    const err = new Error("Stream ended without completion") as Error & { hadTokens: boolean };
    err.hadTokens = true;
    act(() => onError!(err));

    await waitFor(() =>
      expect(screen.getByText(/respuesta parcial de DeepSeek/)).toBeInTheDocument(),
    );
    expect(screen.queryByText("(error al generar respuesta)")).not.toBeInTheDocument();
  });

  it("error sin tokens muestra el mensaje de error", async () => {
    let onError: ((error: Error) => void) | undefined;
    vi.mocked(chatService.sendMessageStream).mockImplementation((_p, _c, callbacks) => {
      onError = callbacks.onError;
      return new AbortController();
    });

    const user = userEvent.setup();
    await loadPage();

    await user.type(screen.getByPlaceholderText("Escribe tu mensaje..."), "hola KIN");
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await waitFor(() => expect(onError).toBeDefined());
    const err = new Error("boom") as Error & { hadTokens: boolean };
    err.hadTokens = false;
    act(() => onError!(err));

    await waitFor(() =>
      expect(screen.getByText("(error al generar respuesta)")).toBeInTheDocument(),
    );
  });

  it("done con content vacío conserva los tokens acumulados", async () => {
    let onToken: ((token: string) => void) | undefined;
    let onDone: ((response: {
      userMessageId: string;
      assistantMessageId: string;
      content: string;
      tokensUsed: number;
    }) => void) | undefined;
    vi.mocked(chatService.sendMessageStream).mockImplementation((_p, _c, callbacks) => {
      onToken = callbacks.onToken;
      onDone = callbacks.onDone;
      return new AbortController();
    });

    const user = userEvent.setup();
    await loadPage();

    await user.type(screen.getByPlaceholderText("Escribe tu mensaje..."), "hola KIN");
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    await waitFor(() => expect(onToken).toBeDefined());
    act(() => onToken!("contenido acumulado"));

    act(() =>
      onDone!({
        userMessageId: "u1",
        assistantMessageId: "a1",
        content: "",
        tokensUsed: 0,
      }),
    );

    await waitFor(() =>
      expect(screen.getByText("contenido acumulado")).toBeInTheDocument(),
    );
    expect(screen.queryByText("(error al generar respuesta)")).not.toBeInTheDocument();
  });
});
