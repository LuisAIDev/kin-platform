import { describe, expect, it } from "vitest";
import {
  assertChatMessageLength,
  isChatMessageTooLong,
  MAX_CHAT_MESSAGE_LENGTH,
} from "@/utils/chatLimits";

describe("chatLimits", () => {
  it("permite un mensaje normal", () => {
    expect(isChatMessageTooLong("hola")).toBe(false);
  });

  it("permite un mensaje de exactamente 10.000 caracteres", () => {
    expect(isChatMessageTooLong("a".repeat(MAX_CHAT_MESSAGE_LENGTH))).toBe(false);
  });

  it("bloquea un mensaje de 10.001 caracteres", () => {
    expect(isChatMessageTooLong("a".repeat(MAX_CHAT_MESSAGE_LENGTH + 1))).toBe(true);
  });

  it("assertChatMessageLength no lanza con 10.000 caracteres", () => {
    expect(() => assertChatMessageLength("a".repeat(MAX_CHAT_MESSAGE_LENGTH))).not.toThrow();
  });

  it("assertChatMessageLength lanza con más de 10.000 caracteres", () => {
    expect(() => assertChatMessageLength("a".repeat(MAX_CHAT_MESSAGE_LENGTH + 1))).toThrow(
      /no puede superar 10000 caracteres/,
    );
  });
});
