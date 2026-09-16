'use client';

import { useState } from 'react';
import { X, MessageCircle, Mail, Link2, Copy, Check, Shield } from 'lucide-react';

interface InvitePhysicianModalProps {
  isOpen: boolean;
  onClose: () => void;
  patientId: string;
  patientName?: string;
}

export function InvitePhysicianModal({ 
  isOpen, 
  onClose, 
  patientId,
  patientName 
}: InvitePhysicianModalProps) {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const registerUrl = `https://www.kin-platform-medical.com/register/salud/medico?invited_by=${patientId}`;
  
  const genericMessage = `Hola${patientName ? `, soy ${patientName}` : ''}. Tengo una consulta médica y me gustaría compartirla contigo por KIN Medical, una plataforma donde puedo gestionar mi salud. ¿Podrías registrarte? ${registerUrl}`;

  function handleWhatsApp() {
    const url = `https://wa.me/?text=${encodeURIComponent(genericMessage)}`;
    window.open(url, '_blank');
  }

  function handleEmail() {
    const subject = 'Consulta médica desde KIN Medical';
    const body = genericMessage;
    window.location.href = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  }

  async function handleCopyLink() {
    try {
      await navigator.clipboard.writeText(registerUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 3000);
    } catch (err) {
      console.error('No se pudo copiar:', err);
    }
  }

  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-lg bg-white rounded-2xl shadow-xl p-6"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-xl font-bold text-neutral-900">
              Compartir con mi médico
            </h2>
            <p className="text-sm text-neutral-500 mt-1">
              Tu médico aún no está en KIN Medical
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-neutral-400 hover:text-neutral-600 transition"
            aria-label="Cerrar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Aviso de privacidad */}
        <div className="mb-5 p-3 rounded-lg bg-blue-50 border border-blue-200 flex items-start gap-2">
          <Shield className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
          <p className="text-xs text-blue-900">
            <strong>Tu privacidad importa.</strong> Este mensaje no incluye datos médicos. 
            Tu médico verá los detalles dentro de KIN después de registrarse.
          </p>
        </div>

        {/* Opciones */}
        <div className="space-y-3">
          <button
            onClick={handleWhatsApp}
            className="w-full flex items-center gap-3 p-4 rounded-xl border border-neutral-200 hover:border-green-400 hover:bg-green-50 transition text-left"
          >
            <div className="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
              <MessageCircle className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <p className="font-medium text-neutral-900">Compartir por WhatsApp</p>
              <p className="text-xs text-neutral-500">Enviar invitación por WhatsApp</p>
            </div>
          </button>

          <button
            onClick={handleEmail}
            className="w-full flex items-center gap-3 p-4 rounded-xl border border-neutral-200 hover:border-medical-400 hover:bg-medical-50 transition text-left"
          >
            <div className="w-10 h-10 rounded-full bg-medical-100 flex items-center justify-center flex-shrink-0">
              <Mail className="w-5 h-5 text-medical-600" />
            </div>
            <div>
              <p className="font-medium text-neutral-900">Enviar por email</p>
              <p className="text-xs text-neutral-500">Enviar invitación por correo</p>
            </div>
          </button>

          <button
            onClick={handleCopyLink}
            className="w-full flex items-center gap-3 p-4 rounded-xl border border-neutral-200 hover:border-neutral-400 hover:bg-neutral-50 transition text-left"
          >
            <div className="w-10 h-10 rounded-full bg-neutral-100 flex items-center justify-center flex-shrink-0">
              {copied ? (
                <Check className="w-5 h-5 text-green-600" />
              ) : (
                <Link2 className="w-5 h-5 text-neutral-600" />
              )}
            </div>
            <div>
              <p className="font-medium text-neutral-900">
                {copied ? 'Link copiado!' : 'Copiar link de invitación'}
              </p>
              <p className="text-xs text-neutral-500">
                {copied ? 'Pégalo donde quieras' : 'Compártelo por cualquier canal'}
              </p>
            </div>
          </button>
        </div>

        {/* Footer */}
        <div className="mt-5 pt-4 border-t border-neutral-200">
          <p className="text-xs text-neutral-500 text-center">
            Cuando tu médico se registre, podrá vincularse contigo para gestionar tu salud en KIN.
          </p>
        </div>
      </div>
    </div>
  );
}