package com.kinplatform.kin.health.documents.application;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Slf4j
public class GoogleVisionOcrService {

    private ImageAnnotatorClient client;

    /**
     * Extrae texto de una imagen usando Google Cloud Vision DOCUMENT_TEXT_DETECTION.
     *
     * @param imageBytes contenido de la imagen
     * @return texto extraído (puede estar vacío si no se detecta texto)
     * @throws IOException si falla la conexión con Google Cloud
     */
    public String extractText(byte[] imageBytes) throws IOException {
        if (client == null) {
            initializeClient();
        }

        ByteString imgBytes = ByteString.copyFrom(imageBytes);
        Image img = Image.newBuilder().setContent(imgBytes).build();

        Feature feat = Feature.newBuilder()
                .setType(Feature.Type.DOCUMENT_TEXT_DETECTION)
                .build();

        ImageContext context = ImageContext.newBuilder()
                .addLanguageHints("es")
                .build();

        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .setImageContext(context)
                .build();

        BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));
        AnnotateImageResponse res = response.getResponses(0);

        if (res.hasError()) {
            log.error("Google Vision error: {}", res.getError().getMessage());
            throw new IOException("Error de OCR: " + res.getError().getMessage());
        }

        if (res.hasFullTextAnnotation()) {
            String text = res.getFullTextAnnotation().getText();
            log.info("OCR extraído: {} caracteres", text.length());
            return text;
        }

        log.warn("OCR no detectó texto en la imagen");
        return "";
    }

    private synchronized void initializeClient() throws IOException {
        if (client != null) return;

        String credentialsJson = System.getenv("GOOGLE_APPLICATION_CREDENTIALS_JSON");

        if (credentialsJson == null || credentialsJson.isBlank()) {
            // Fallback: intentar desde credenciales por defecto (para local con gcloud auth)
            try {
                client = ImageAnnotatorClient.create();
                log.info("Google Vision inicializado desde credenciales por defecto");
                return;
            } catch (IOException e) {
                throw new IOException(
                    "Google Vision no configurado. " +
                    "Falta GOOGLE_APPLICATION_CREDENTIALS_JSON en las variables de entorno.",
                    e
                );
            }
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))
        );

        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();

        client = ImageAnnotatorClient.create(settings);
        log.info("Google Vision inicializado desde GOOGLE_APPLICATION_CREDENTIALS_JSON");
    }
}