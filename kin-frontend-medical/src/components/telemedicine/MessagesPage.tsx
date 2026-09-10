"use client";

import { useEffect, useState } from "react";
import { telemedicineService, type Conversation, type Contact } from "@/services/telemedicine";
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
  const [newContact, setNewContact] = useState<Contact | null>(null);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [filteredContacts, setFilteredContacts] = useState<Contact[]>([]);
  const [contactSearch, setContactSearch] = useState("");
  const [showNew, setShowNew] = useState(false);
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
      })
      .finally(() => {
        if (!cancelled) {
          // no-op
        }
      });
    const interval = setInterval(loadConversations, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  const loadContacts = async () => {
    try {
      const data = await telemedicineService.contacts();
      setContacts(data);
      setFilteredContacts(data);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const openNewConversation = async () => {
    setShowNew(true);
    setError("");
    setContactSearch("");
    await loadContacts();
  };

  const filterContacts = (search: string) => {
    setContactSearch(search);
    const filtered = contacts.filter((c) =>
      c.name.toLowerCase().includes(search.toLowerCase())
    );
    setFilteredContacts(filtered);
  };

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
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={openNewConversation}
              className="rounded-lg bg-primary-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-primary-700 transition"
            >
              Nueva conversación
            </button>
            {unread > 0 && (
              <span className="rounded-full bg-primary-600 text-white px-3 py-1 text-sm font-medium">
                {unread} no leídos
              </span>
            )}
          </div>
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 px-4 py-2.5 rounded-lg">{error}</p>
        )}

        {showNew && (
          <div className="rounded-xl border border-neutral-200 bg-white p-4 flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-semibold">Iniciar nueva conversación</h2>
                <p className="text-xs text-neutral-400">
                  Busca un contacto para iniciar la conversación.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowNew(false)}
                aria-label="Cerrar"
                className="text-neutral-400 hover:text-neutral-600 text-xl leading-none"
              >
                ×
              </button>
            </div>
            <div className="flex flex-col gap-2">
              <input
                type="text"
                placeholder="Buscar contactos..."
                value={contactSearch}
                onChange={(e) => filterContacts(e.target.value)}
                className="rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                aria-label="Buscar contactos"
              />
              {filteredContacts.length === 0 ? (
                <p className="text-sm text-neutral-400 text-center py-4">
                  {contactSearch
                    ? "No se encontraron contactos con ese nombre."
                    : "No tienes contactos con relación activa. Un médico o paciente debe vincularte primero."}
                </p>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-60 overflow-y-auto">
                  {filteredContacts.map((c) => (
                    <button
                      key={c.id}
                      type="button"
                      onClick={() => {
                        setNewContact(c);
                        setSelected(null);
                        setShowNew(false);
                        setContactSearch("");
                      }}
                      className="rounded-lg border border-neutral-200 px-3 py-2 text-sm text-left hover:bg-neutral-50 transition"
                    >
                      {c.name} <span className="text-xs text-neutral-400">({c.role})</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
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
                onClick={() => {
                  setSelected(c);
                  setNewContact(null);
                }}
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
            ) : newContact ? (
              <ChatView
                otherId={newContact.id}
                onUnreadChange={() => {
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
