"use client";

import { useEffect, useRef, useState } from "react";
import {
  telemedicineService,
  type TelemedicineMessage,
} from "@/services/telemedicine";

function formatTime(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ChatView({
  otherId,
  onUnreadChange,
}: {
  otherId: string;
  onUnreadChange?: (count: number) => void;
}) {
  const [messages, setMessages] = useState<TelemedicineMessage[]>([]);
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  const load = async () => {
    try {
      const data = await telemedicineService.messages(otherId);
      setMessages(data);
      if (onUnreadChange) onUnreadChange(0);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    if (!otherId) return;
    telemedicineService
      .messages(otherId)
      .then((data) => {
        if (cancelled) return;
        setMessages(data);
        if (onUnreadChange) onUnreadChange(0);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    const interval = setInterval(load, 15000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [otherId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView?.({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async () => {
    if (!content.trim()) return;
    setLoading(true);
    setError("");
    try {
      await telemedicineService.sendMessage(otherId, content.trim());
      setContent("");
      await load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  };

  if (!otherId) return null;

  return (
    <div className="flex flex-col h-[520px] rounded-xl border border-neutral-200 bg-white">
      <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-2">
        {messages.length === 0 && (
          <p className="text-sm text-neutral-400 text-center py-8">
            Inicia la conversación con un mensaje.
          </p>
        )}
        {messages.map((m) => {
          const mine = m.mine;
          return (
            <div key={m.id} className={`flex ${mine ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm flex flex-col gap-1 ${
                  mine
                    ? "bg-primary-600 text-white rounded-br-sm"
                    : "bg-neutral-100 text-neutral-800 rounded-bl-sm"
                }`}
              >
                <span>{m.content}</span>
                <span className={`text-[10px] ${mine ? "text-primary-100" : "text-neutral-400"}`}>
                  {formatTime(m.createdAt)}
                </span>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {error && (
        <p className="px-4 py-2 text-xs text-red-600 bg-red-50 border-t border-red-100">{error}</p>
      )}

      <div className="border-t border-neutral-200 p-3 flex gap-2">
        <input
          type="text"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSend();
          }}
          placeholder="Escribe un mensaje..."
          className="flex-1 rounded-lg border border-neutral-300 px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
        <button
          type="button"
          onClick={handleSend}
          disabled={loading || !content.trim()}
          className="rounded-lg bg-primary-600 px-5 py-2 text-sm font-medium text-white hover:bg-primary-700 transition disabled:bg-primary-300"
        >
          Enviar
        </button>
      </div>
    </div>
  );
}
