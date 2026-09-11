import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import ChatView from "@/components/telemedicine/ChatView";

const { telemedicineService } = vi.hoisted(() => ({
  telemedicineService: {
    messages: vi.fn(),
    sendMessage: vi.fn(),
  },
}));

vi.mock("@/services/telemedicine", () => ({ telemedicineService }));

describe("ChatView", () => {
  it("muestra los mensajes de la conversación", async () => {
    telemedicineService.messages.mockResolvedValue([
      {
        id: "m1",
        senderId: "other",
        receiverId: "me",
        conversationId: "c1",
        content: "Hola, ¿cómo te sientes?",
        read: true,
        mine: false,
        createdAt: "2026-08-26T10:00:00Z",
      },
      {
        id: "m2",
        senderId: "me",
        receiverId: "other",
        conversationId: "c1",
        content: "Mejor, gracias",
        read: true,
        mine: true,
        createdAt: "2026-08-26T10:05:00Z",
      },
    ]);

    render(<ChatView otherId="other" />);

    expect(await screen.findByText("Hola, ¿cómo te sientes?")).toBeInTheDocument();
    expect(screen.getByText("Mejor, gracias")).toBeInTheDocument();
  });

  it("envía un mensaje", async () => {
    telemedicineService.messages.mockResolvedValue([]);
    const user = userEvent.setup();
    render(<ChatView otherId="other" />);

    await user.type(screen.getByPlaceholderText("Escribe un mensaje..."), "Tengo fiebre");
    await user.click(screen.getByRole("button", { name: "Enviar" }));

    expect(telemedicineService.sendMessage).toHaveBeenCalledWith("other", "Tengo fiebre");
  });
});
