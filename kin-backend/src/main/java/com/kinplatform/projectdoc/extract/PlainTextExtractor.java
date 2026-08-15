package com.kinplatform.projectdoc.extract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Extrae el contenido de archivos de texto plano (TXT) y CSV sin dependencias. */
@Component
public class PlainTextExtractor implements DocumentExtractor {

    @Override
    public boolean supports(String mimeType, String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".txt") || name.endsWith(".csv") || (mimeType != null && mimeType.startsWith("text/"));
    }

    @Override
    public String extract(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
}
