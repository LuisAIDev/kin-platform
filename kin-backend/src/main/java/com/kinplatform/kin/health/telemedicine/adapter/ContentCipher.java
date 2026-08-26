package com.kinplatform.kin.health.telemedicine.adapter;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado en reposo del contenido de mensajes de telemedicina (ADR-032).
 *
 * <p>Infraestructura: AES/GCM/NoPadding con soporte de <strong>rotación de
 * claves</strong>. La lista de claves está ordenada de más reciente a más
 * antigua; la primera ({@code active}) se usa para cifrar y, al descifrar, se
 * prueban todas las claves en orden (permite leer datos cifrados con claves
 * anteriores durante la rotación). El dominio nunca ve el contenido cifrado.</p>
 */
public final class ContentCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final List<SecretKeySpec> keys;

    /**
     * @param secrets claves separadas por comas; la primera es la activa. Si se
     *                omite o está vacío, se usa un valor de desarrollo (no apto
     *                para producción).
     */
    public ContentCipher(String secrets) {
        this.keys = new ArrayList<>();
        for (String secret : split(secrets)) {
            keys.add(toKey(secret));
        }
        if (keys.isEmpty()) {
            keys.add(toKey("kin-telemedicine-dev-key"));
        }
    }

    ContentCipher(List<SecretKeySpec> keys) {
        this.keys = List.copyOf(keys);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return "";
        }
        return encrypt(keys.get(0), plaintext);
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return "";
        }
        for (SecretKeySpec key : keys) {
            try {
                return decrypt(key, ciphertext);
            } catch (Exception ignored) {
                // probar la siguiente clave (rotación)
            }
        }
        throw new IllegalStateException("No se pudo descifrar el contenido del mensaje");
    }

    private static String encrypt(SecretKeySpec key, String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar el contenido del mensaje", ex);
        }
    }

    private static String decrypt(SecretKeySpec key, String ciphertext) throws Exception {
        byte[] combined = Base64.getDecoder().decode(ciphertext);
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKeySpec toKey(String secret) {
        String material = secret == null || secret.isBlank() ? "kin-telemedicine-dev-key" : secret;
        byte[] keyBytes = new byte[16];
        byte[] materialBytes = material.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(materialBytes, 0, keyBytes, 0, Math.min(16, materialBytes.length));
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static List<String> split(String secrets) {
        if (secrets == null || secrets.isBlank()) {
            return List.of();
        }
        var out = new ArrayList<String>();
        for (String s : secrets.split(",")) {
            if (s != null && !s.isBlank()) {
                out.add(s.trim());
            }
        }
        return out;
    }
}
