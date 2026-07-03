"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { projectsService, type Project } from "@/services/projects";
import { chatService, type ChatMessage, type ChatResponse } from "@/services/chat";
import { authService } from "@/services/auth";
import { forceLogout } from "@/services/session";
import ViabilityScore from "@/components/ViabilityScore";
import PdfReportButton from "@/components/PdfReportButton";

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
        <button
          onClick={() => router.push("/dashboard/projects")}
          className="text-sm text-neutral-500 hover:text-neutral-900 mb-4 block"
        >
          &larr; Volver
        </button>

        <h1 className="text-xl font-bold mb-2">{project.title}</h1>

        {project.description && (
          <p className="text-sm text-neutral-600 mb-4">
            {project.description}
          </p>
        )}

        <div className="flex flex-wrap gap-2 mb-6">
          <span className="text-xs bg-neutral-100 px-2.5 py-1 rounded-full font-medium">
            {project.category}
          </span>
          <span className="text-xs bg-neutral-100 px-2.5 py-1 rounded-full font-medium">
            {project.status}
          </span>
          {project.viabilityScore !== null && (
            <span className="text-xs bg-green-100 text-green-800 px-2.5 py-1 rounded-full font-medium">
              Score: {project.viabilityScore}
            </span>
          )}
        </div>

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
      </aside>

      <section className="flex-1 flex flex-col min-w-0">
        <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-16">
              <p className="text-neutral-400 text-sm">
                KIN te guiará en la estructuración de tu proyecto.
              </p>
              <p className="text-neutral-400 text-sm">
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
                className={`flex ${isUser ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[80%] lg:max-w-[70%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                    isUser
                      ? "bg-neutral-900 text-white rounded-br-md"
                      : "bg-neutral-100 text-neutral-900 rounded-bl-md"
                  }`}
                >
                  <RenderContent content={msg.content} projectTitle={project.title} />
                  {isStreaming && (
                    <span className="inline-block w-2 h-4 bg-neutral-500 animate-pulse ml-0.5" />
                  )}
                </div>
              </div>
            );
          })}
          <div ref={chatEndRef} />
        </div>

        <div className="border-t border-neutral-200 px-4 py-3 shrink-0">
          <div className="flex gap-2 max-w-4xl mx-auto">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Escribe tu mensaje..."
              disabled={sending}
              className="flex-1 rounded-xl border border-neutral-300 px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-neutral-900 disabled:opacity-50"
            />
            <button
              onClick={handleSend}
              disabled={sending || !input.trim()}
              className="rounded-xl bg-neutral-900 px-5 py-2.5 text-sm font-medium text-white hover:bg-neutral-800 transition disabled:opacity-50"
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

function RenderContent({ content, projectTitle }: { content: string; projectTitle: string }) {
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
        <PdfReportButton projectTitle={projectTitle} content={content} />
        {after && <span className="whitespace-pre-line">{after}</span>}
      </>
    );
  }

  const hasKeywords = /resumen\s+ejecutivo|Scoring|Viabilidad|recomendaría/i.test(content);
  if (hasKeywords) {
    return (
      <>
        <span className="whitespace-pre-line">{content}</span>
        <PdfReportButton projectTitle={projectTitle} content={content} />
      </>
    );
  }

  return <span className="whitespace-pre-line">{content}</span>;
}
