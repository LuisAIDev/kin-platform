package com.kinplatform.kin.enterprise.renderer;

import com.kinplatform.kin.enterprise.valueobjects.DocumentType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilidades compartidas por los renderizadores de documentos (Fase 10,
 * Milestone 2H).
 *
 * <p>Centraliza las transformaciones neutras de texto que los renderizadores
 * PDF, DOCX y PPTX reutilizan: partición en líneas, título legible del tipo de
 * documento, escape XML (OOXML) y escritura de entradas ZIP. Clase de paquete:
 * no forma parte del API público del módulo.</p>
 */
final class RendererText {

    private RendererText() {
    }

    /**
     * Divide el contenido neutral en líneas (maneja {@code \n}, {@code \r\n} y
     * {@code \r}).
     *
     * @param content contenido del documento, o {@code null}
     * @return lista de líneas (posiblemente vacía)
     */
    static List<String> lines(String content) {
        return content == null ? List.of() : content.lines().toList();
    }

    /**
     * Título legible del tipo de documento (p. ej. {@code LEAN_CANVAS} →
     * {@code "LEAN CANVAS"}).
     *
     * @param type tipo de documento, o {@code null}
     * @return título legible, o cadena vacía si {@code type} es {@code null}
     */
    static String title(DocumentType type) {
        return type == null ? "" : type.name().replace('_', ' ');
    }

    /**
     * Escapa un texto para su uso en XML (OOXML): {@code &}, {@code <},
     * {@code >}, comillas simples y dobles.
     *
     * @param value texto a escapar, o {@code null}
     * @return texto escapado, o cadena vacía si {@code value} es {@code null}
     */
    static String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    /**
     * Ajusta una línea al ancho máximo sin partir palabras cuando es posible.
     *
     * @param line  línea original
     * @param width ancho máximo en caracteres (mayor que 0)
     * @return lista de líneas ajustadas
     */
    static List<String> wrap(String line, int width) {
        if (line == null || line.length() <= width) {
            return line == null ? List.of() : List.of(line);
        }
        List<String> wrapped = new ArrayList<>();
        String remaining = line;
        while (remaining.length() > width) {
            int split = remaining.lastIndexOf(' ', width);
            if (split <= 0) {
                split = width;
            }
            wrapped.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
            if (remaining.isEmpty()) {
                return List.copyOf(wrapped);
            }
        }
        wrapped.add(remaining);
        return List.copyOf(wrapped);
    }

    /**
     * Añade una entrada ZIP con contenido UTF-8.
     *
     * @param zip     flujo ZIP de salida
     * @param name    nombre de la entrada (ruta interna del paquete OOXML)
     * @param content contenido XML/texto de la entrada
     * @throws IOException si el flujo ZIP falla
     */
    static void writeZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
