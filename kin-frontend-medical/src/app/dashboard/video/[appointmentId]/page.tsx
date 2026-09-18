'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';

export default function VideoCallPage() {
  const params = useParams();
  const router = useRouter();
  const appointmentId = params.appointmentId as string;

  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function initVideoRoom() {
      try {
        const res = await fetch(
          `/api/v1/medical/telemedicine/appointments/${appointmentId}/video-room`,
          { method: 'POST', credentials: 'include' }
        );

        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || 'No se pudo iniciar la videollamada');
        }

        const { videoUrl } = await res.json();
        setVideoUrl(videoUrl);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }

    initVideoRoom();
  }, [appointmentId]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p>Preparando videollamada...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="max-w-md bg-white rounded-2xl border border-neutral-200 p-8 text-center">
          <h1 className="text-xl font-bold text-neutral-900 mb-2">No se pudo iniciar</h1>
          <p className="text-neutral-600 mb-4">{error}</p>
          <Link
            href="/dashboard/appointments"
            className="inline-flex items-center justify-center rounded-lg bg-medical-600 px-4 py-2 text-sm font-medium text-white hover:bg-medical-700"
          >
            Volver
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black flex flex-col">
      <div className="bg-neutral-900 text-white px-4 py-2 flex items-center justify-between">
        <span className="text-sm">Videollamada médica — KIN Medical</span>
        <button
          onClick={() => router.back()}
          className="text-sm text-red-400 hover:text-red-300"
        >
          Salir
        </button>
      </div>
      <iframe
        src={videoUrl!}
        allow="camera; microphone; fullscreen; display-capture; autoplay"
        className="flex-1 w-full border-0"
        title="Videollamada médica"
      />
    </div>
  );
}
