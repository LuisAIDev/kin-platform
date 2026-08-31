package com.kinplatform.kin.health.documents.infrastructure;

import com.kinplatform.kin.health.documents.config.DocumentProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Almacenamiento local de archivos de documentos clínicos (ADR-036).
 *
 * <p>Guarda los archivos en una carpeta configurable
 * ({@code kin.health.documents.storage-path}) usando la {@code storageKey}
 * (generada como UUID + nombre sanitizado). En el futuro se puede migrar a
 * S3/Azure Blob detrás de la misma interfaz.</p>
 */
@Component
public class DocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorage.class);

    private final Path basePath;

    public DocumentStorage(DocumentProperties properties) {
        this.basePath = Path.of(properties.getStoragePath()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(basePath);
            log.info("DocumentStorage: almacenamiento local en {}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento: " + basePath, e);
        }
    }

    public void store(String storageKey, byte[] content) {
        try {
            Path target = resolveSafe(storageKey);
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo almacenar el documento: " + storageKey, e);
        }
    }

    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolveSafe(storageKey));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el documento: " + storageKey, e);
        }
    }

    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveSafe(storageKey));
        } catch (IOException e) {
            log.warn("DocumentStorage: no se pudo eliminar el archivo {}", storageKey);
        }
    }

    /** Evita path traversal: resuelve la key dentro del directorio base. */
    private Path resolveSafe(String storageKey) {
        Path resolved = basePath.resolve(storageKey).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("storageKey inválida");
        }
        return resolved;
    }
}
