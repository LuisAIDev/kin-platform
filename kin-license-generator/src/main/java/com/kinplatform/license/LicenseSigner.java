package com.kinplatform.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Base64;

public class LicenseSigner {

    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEY_ALIAS = "kin-license";

    private final ObjectMapper objectMapper;
    private final Path keyStorePath;
    private final char[] keyStorePassword;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public LicenseSigner() throws Exception {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register BouncyCastle security provider
        Security.addProvider(new BouncyCastleProvider());

        // Keystore location: ~/.kin/license.keystore
        String userHome = System.getProperty("user.home");
        this.keyStorePath = Paths.get(userHome, ".kin", "license.keystore");
        this.keyStorePassword = "kin-license-generator".toCharArray();

        initializeKeyStore();
    }

    public LicenseSigner(String keyStorePath, String keyStorePassword) throws Exception {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Register BouncyCastle security provider
        Security.addProvider(new BouncyCastleProvider());

        this.keyStorePath = Paths.get(keyStorePath);
        this.keyStorePassword = keyStorePassword.toCharArray();

        initializeKeyStore();
    }

    private void initializeKeyStore() throws Exception {
        // Ensure directory exists
        Files.createDirectories(keyStorePath.getParent());

        if (Files.exists(keyStorePath)) {
            loadKeyStore();
        } else {
            generateKeyPair();
            saveKeyStore();
        }
    }

    private void loadKeyStore() throws Exception {
        try (FileInputStream fis = new FileInputStream(keyStorePath.toFile())) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(fis, keyStorePassword);
            this.privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, keyStorePassword);
            this.publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        }
    }

    private void generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        // Create keystore and store the key pair
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, keyStorePassword);

        // Create a self-signed certificate for the public key
        Certificate cert = createSelfSignedCertificate(keyPair);
        keyStore.setKeyEntry(KEY_ALIAS, privateKey, keyStorePassword, new Certificate[]{cert});
    }

    private java.security.cert.X509Certificate createSelfSignedCertificate(KeyPair keyPair) throws Exception {
        // Generate a self-signed X.509 certificate using BouncyCastle
        org.bouncycastle.asn1.x500.X500Name subject = new org.bouncycastle.asn1.x500.X500Name("CN=KIN License Generator");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        
        java.util.Date notBefore = java.util.Date.from(java.time.Instant.now());
        java.util.Date notAfter = java.util.Date.from(java.time.Instant.now().plus(java.time.Duration.ofDays(3650))); // 10 years
        
        org.bouncycastle.cert.X509v3CertificateBuilder certBuilder = new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(
            subject, serial,
            notBefore,
            notAfter,
            subject, keyPair.getPublic()
        );
        
        org.bouncycastle.operator.ContentSigner signer = new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.getPrivate());
        
        org.bouncycastle.cert.X509CertificateHolder certHolder = certBuilder.build(signer);
        return new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
    }

    

    private void saveKeyStore() throws Exception {
        try (FileOutputStream fos = new FileOutputStream(keyStorePath.toFile())) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(null, keyStorePassword);
            keyStore.store(fos, keyStorePassword);
        }
    }

    /**
     * Signs a license payload.
     *
     * @param payload the payload to sign (signature field should be null)
     * @return base64 encoded signature
     */
    public String sign(LicensePayload payload) throws Exception {
        if (payload.getSignature() != null) {
            throw new IllegalArgumentException("Payload already has a signature");
        }

        // Serialize to canonical JSON (without signature field)
        LicensePayload payloadToSign = payload.withoutSignature();
        String json = objectMapper.writeValueAsString(payloadToSign);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // Sign the data
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        byte[] signatureBytes = signature.sign();

        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Verifies a license payload signature.
     *
     * @param payload the payload with signature
     * @return true if signature is valid
     */
    public boolean verify(LicensePayload payload) throws Exception {
        if (payload.getSignature() == null) {
            return false;
        }

        // Serialize to canonical JSON (without signature field)
        LicensePayload payloadToVerify = payload.withoutSignature();
        String json = objectMapper.writeValueAsString(payloadToVerify);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        // Verify the signature
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(data);
        byte[] signatureBytes = Base64.getDecoder().decode(payload.getSignature());

        return signature.verify(signatureBytes);
    }

    public void exportPublicKey(OutputStream out) throws Exception {
        out.write("-----BEGIN PUBLIC KEY-----\n".getBytes(StandardCharsets.UTF_8));
        out.write(Base64.getEncoder().encode(publicKey.getEncoded()));
        out.write("\n-----END PUBLIC KEY-----\n".getBytes(StandardCharsets.UTF_8));
    }

    public void exportPublicKey(File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            exportPublicKey(fos);
        }
    }

    public void exportPrivateKey(OutputStream out) throws Exception {
        out.write("-----BEGIN PRIVATE KEY-----\n".getBytes(StandardCharsets.UTF_8));
        out.write(Base64.getEncoder().encode(privateKey.getEncoded()));
        out.write("\n-----END PRIVATE KEY-----\n".getBytes(StandardCharsets.UTF_8));
    }

    public void exportPrivateKey(File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            exportPrivateKey(fos);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Parses a license key JSON string into a LicensePayload object.
     */
    public LicensePayload parseLicense(String licenseJson) throws Exception {
        return objectMapper.readValue(licenseJson, LicensePayload.class);
    }
}