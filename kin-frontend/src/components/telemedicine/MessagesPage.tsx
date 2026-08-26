"use client";

import { useEffect, useState } from "react";
import { telemedicineService, type Conversation } from "@/services/telemedicine";
import ChatView from "@/components/telemedicine/ChatView";

function formatDate(iso: string) {
  return new Date(iso).toLocaleString("es-ES", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function MessagesPage() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [unread, setUnread] = useState(0);
  const [error, setError] = useState("");

  const loadConversations = async () => {
    try {
      const data = await telemedicineService.conversations();
      setConversations(data);
      const unreadData = await telemedicineService.unread();
      setUnread(unreadData.unread);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([telemedicineService.conversations(), telemedicineService.unread()])
      .then(([convos, unreadData]) => {
        if (cancelled) return;
        setConversations(convos);
        setUnread(unreadData.unread);
      })
      .catch((err) => {
        if (!cancelled) setError((err as Error).message);
      });
    const interval = setInterval(loadConversations, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  return (
    <main className="flex-1 flex items-start justify-center px-6 pt-10 pb-12">
      <div className="w-full max-w-5xl flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Mensajes</h1>
            <p className="text-sm text-neutral-500 mt-1">
              Comunicación segura con tu equipo de salud.
            </p>
          </div>
          {unread > 0 && (
            <span className="rounded-full bg-primary-600 text-white px-3 py-1 text-sm font-medium">
              {unread} no leídos
            </span>
          )}
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="rounded-xl border border-neutral-200 bg-white overflow-hidden lg:h-[520px] lg:overflow-y-auto">
            {conversations.length === 0 && (
              <p className="text-sm text-neutral-400 p-6 text-center">
                Aún no tienes conversaciones.
              </p>
            )}
            {conversations.map((c) => (
              <button
                key={c.otherId}
                type="button"
                onClick={() => setSelected(c)}
                className={`w-full text-left px-4 py-3 border-b border-neutral-100 hover:bg-neutral-50 transition flex items-start justify-between gap-2 ${
                  selected?.otherId === c.otherId ? "bg-primary-50" : ""
                }`}
              >
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="text-sm font-semibold text-neutral-800">{c.otherName}</span>
                  <span className="text-xs text-neutral-500 truncate">{c.lastMessage}</span>
                  <span className="text-[10px] text-neutral-400">{formatDate(c.lastMessageAt)}</span>
                </div>
                {c.unread > 0 && (
                  <span className="rounded-full bg-primary-600 text-white px-2 py-0.5 text-[10px] font-bold shrink-0">
                    {c.unread}
                  </span>
                )}
              </button>
            ))}
          </div>

          <div className="lg:col-span-2">
            {selected ? (
              <ChatView
                otherId={selected.otherId}
                onUnreadChange={() => {
                  setUnread((prev) => Math.max(0, prev - 1));
                  loadConversations();
                }}
              />
            ) : (
              <div className="rounded-xl border border-neutral-200 bg-white h-[520px] flex items-center justify-center text-sm text-neutral-400">
                Selecciona una conversación.
              </div>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
