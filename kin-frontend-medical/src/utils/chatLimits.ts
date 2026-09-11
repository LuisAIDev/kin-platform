/**
 * Límites del mensaje de chat.
 *
 * <p>Contrato con el backend: {@code ChatRequest.content} tiene
 * {@code @Size(max = 10000)}. Este límite NO debe modificarse en el frontend
 * por decisión propia; el frontend únicamente impide que se envíe contenido
 * que el backend rechazaría, manteniendo intacto el contrato.</p>
 */

/** Límite máximo de caracteres de un mensaje de chat (idéntico al backend). */
export const MAX_CHAT_MESSAGE_LENGTH = 10000;

/** A partir de este número de caracteres el contador muestra advertencia visual. */
export const CHAT_LIMIT_WARNING_AT = 9000;

/** Indica si el contenido supera el límite permitido por el backend. */
export function isChatMessageTooLong(content: string): boolean {
  return content.length > MAX_CHAT_MESSAGE_LENGTH;
}

/** Lanza un error claro cuando el contenido excede el límite. */
export function assertChatMessageLength(content: string): void {
  if (isChatMessageTooLong(content)) {
    throw new Error(
      `El mensaje no puede superar ${MAX_CHAT_MESSAGE_LENGTH} caracteres (${content.length}). ` +
        'Resúmelo o usa "Importar información" / "Agregar documento" para incorporar contenido extenso.',
    );
  }
}
