"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { projectsService, type Project } from "@/services/projects";
import { chatService, type ChatMessage, type ChatResponse } from "@/services/chat";
import { authService } from "@/services/auth";
import { forceLogout } from "@/services/session";
import ViabilityScore from "@/components/ViabilityScore";
import PdfReportButton from "@/components/PdfReportButton";
import ProgressCircle from "@/components/ProgressCircle";
import { categoryBadge, statusBadge } from "@/utils/badgeColors";

const STREAMING_ID_PREFIX = "streaming-";

type Props = {
  params: Promise<{ id: string }>;
};

export default function ProjectDetailPage({ params }: Props) {
  const { id } = use(params);
  const router = useRouter();

  const [project, setProject] = useState<Project | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [loading, setLoading] = useState(true);
  const [streamingId, setStreamingId] = useState<string | null>(null);
  const [infoOpen, setInfoOpen] = useState(false);

  const chatEndRef = useRef<HTMLDivElement>(null);
  const sendingRef = useRef(false);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    const token = authService.getToken();
    if (!token) {
      router.push("/login");
      return;
    }

    Promise.all([
      projectsService.getById(id),
      chatService.getHistory(id),
    ])
      .then(([projectData, history]) => {
        setProject(projectData);
        setMessages(history);
      })
      .catch(() => {
        forceLogout();
      })
      .finally(() => setLoading(false));
  }, [id, router]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    return () => {
      abortRef.current?.abort();
    };
  }, []);

  const handleSend = () => {
    const text = input.trim();
    if (!text || sendingRef.current) return;
    sendingRef.current = true;
    setSending(true);
    setInput("");

    const optimisticUser: ChatMessage = {
      id: crypto.randomUUID(),
      projectId: id,
      userId: "",
      role: "USER",
      content: text,
      metadata: null,
      tokensUsed: 0,
      createdAt: new Date().toISOString(),
    };

    const aiId = STREAMING_ID_PREFIX + crypto.randomUUID();
    const optimisticAi: ChatMessage = {
      id: aiId,
      projectId: id,
      userId: "",
      role: "ASSISTANT",
      content: "",
      metadata: null,
      tokensUsed: 0,
      createdAt: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, optimisticUser, optimisticAi]);
    setStreamingId(aiId);

    const done = () => {
      sendingRef.current = false;
      setStreamingId(null);
      setSending(false);
    };

    abortRef.current = chatService.sendMessageStream(id, text, {
      onToken: (token: string) => {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === aiId ? { ...m, content: m.content + token } : m
          )
        );
      },
      onDone: (res: ChatResponse) => {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === aiId
              ? {
                  ...m,
                  id: res.assistantMessageId,
                  content: res.content || m.content,
                  tokensUsed: res.tokensUsed,
                }
              : m
          )
        );
        done();
      },
      onError: () => {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === aiId
              ? { ...m, content: "(error al generar respuesta)" }
              : m.id === optimisticUser.id
                ? m
                : m
          )
        );
        done();
      },
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  if (loading) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Cargando proyecto...</p>
      </main>
    );
  }

  if (!project) {
    return (
      <main className="flex-1 flex items-center justify-center">
        <p className="text-neutral-500">Proyecto no encontrado</p>
      </main>
    );
  }

  return (
    <main className="flex-1 flex flex-col lg:flex-row">
      <aside className="lg:w-80 xl:w-96 border-b lg:border-b-0 lg:border-r border-neutral-200 p-6 overflow-y-auto shrink-0">
        <div className="flex items-start justify-between">
          <div>
            <button
              onClick={() => router.push("/dashboard/projects")}
              className="text-sm text-neutral-500 hover:text-primary-600 mb-1 block transition"
            >
              &larr; Volver
            </button>
            <h1 className="text-xl font-bold tracking-tight">{project.title}</h1>
          </div>
          <button
            onClick={() => setInfoOpen(!infoOpen)}
            className="lg:hidden flex items-center justify-center w-10 h-10 rounded-lg hover:bg-neutral-100 transition shrink-0 mt-1"
            aria-label={infoOpen ? "Ocultar detalles del proyecto" : "Mostrar detalles del proyecto"}
          >
            <svg className={`w-5 h-5 text-neutral-500 transition-transform ${infoOpen ? "rotate-180" : ""}`} fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </button>
        </div>

        <div className={`${infoOpen ? "" : "hidden"} lg:block mt-4`}>
          {project.description && (
            <p className="text-sm text-neutral-600 mb-4 leading-relaxed">
              {project.description}
            </p>
          )}

          <div className="flex flex-wrap gap-2 mb-6">
            <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${categoryBadge(project.category)}`}>
              {project.category}
            </span>
            <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${statusBadge(project.status)}`}>
              {project.status}
            </span>
            {project.viabilityScore != null && (
              <span className="text-xs bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-full font-medium">
                Score: {project.viabilityScore}
              </span>
            )}
          </div>

          {project.progressPercentage !== null && (
            <div className="mb-6">
              <ProgressCircle percentage={project.progressPercentage} />
            </div>
          )}

          <PdfReportButton project={project} messages={messages} />

          {project.aiSummary && (
            <div>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-neutral-400 mb-1">
                Resumen IA
              </h3>
              <p className="text-sm text-neutral-600 whitespace-pre-line">
                {project.aiSummary}
              </p>
            </div>
          )}
        </div>
      </aside>

      <section className="flex-1 flex flex-col min-w-0">
        <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-16">
              <div className="w-12 h-12 rounded-xl bg-primary-100 flex items-center justify-center mx-auto mb-4">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-primary-600">
                  <polyline points="4 7 12 12 4 17" />
                  <polyline points="12 7 20 12 12 17" />
                </svg>
              </div>
              <p className="text-neutral-500 text-sm font-medium">
                KIN te guiará en la estructuración de tu proyecto.
              </p>
              <p className="text-neutral-400 text-sm mt-1">
                Escribe un mensaje para comenzar.
              </p>
            </div>
          )}

          {messages.map((msg) => {
            const isUser = msg.role === "USER";
            const isStreaming = msg.id === streamingId;
            return (
              <div
                key={msg.id}
                className={`flex items-start gap-2 ${isUser ? "justify-end" : "justify-start"}`}
              >
                {!isUser && (
                  <div className="w-7 h-7 rounded-full bg-primary-100 flex items-center justify-center shrink-0 mt-1">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="text-primary-600">
                      <polyline points="4 7 12 12 4 17" />
                      <polyline points="12 7 20 12 12 17" />
                    </svg>
                  </div>
                )}
                <div
                  className={`max-w-[80%] lg:max-w-[70%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                    isUser
                      ? "bg-neutral-900 text-white rounded-br-md"
                      : "bg-primary-50 text-neutral-900 rounded-bl-md"
                  }`}
                >
                  {!isUser && (
                    <p className="text-[10px] font-semibold text-primary-600 uppercase tracking-wide mb-0.5">
                      KIN
                    </p>
                  )}
                  <RenderContent content={msg.content} />
                  {isStreaming && (
                    <span className="inline-block w-2 h-4 bg-primary-500 animate-pulse ml-0.5" />
                  )}
                </div>
              </div>
            );
          })}
          <div ref={chatEndRef} />
        </div>

        <div className="border-t border-neutral-200 px-4 py-3 shrink-0 sticky bottom-0 bg-white z-10">
          <div className="flex gap-2 max-w-4xl mx-auto">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Escribe tu mensaje..."
              disabled={sending}
              className="flex-1 rounded-xl border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50 min-h-11"
            />
            <button
              onClick={handleSend}
              disabled={sending || !input.trim()}
              className="rounded-xl bg-primary-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-primary-700 transition disabled:opacity-50 min-h-11"
            >
              {sending ? "..." : "Enviar"}
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}

const SCORE_REGEX = /###\s*Scoring\s+de\s+Viabilidad\s+Estimado:\s*\*\*(\d+)\/100\*\*/;

function RenderContent({ content }: { content: string }) {
  const scoreMatch = content.match(SCORE_REGEX);

  if (scoreMatch) {
    const score = parseInt(scoreMatch[1], 10);
    const idx = scoreMatch.index!;
    const before = content.slice(0, idx);
    const after = content.slice(idx + scoreMatch[0].length);

    return (
      <>
        {before && <span className="whitespace-pre-line">{before}</span>}
        <ViabilityScore score={score} />
        {after && <span className="whitespace-pre-line">{after}</span>}
      </>
    );
  }

  return <span className="whitespace-pre-line">{content}</span>;
}
