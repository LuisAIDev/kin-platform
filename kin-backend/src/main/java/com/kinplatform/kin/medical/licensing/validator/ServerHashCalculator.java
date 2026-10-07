package com.kinplatform.kin.medical.licensing.validator;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ServerHashCalculator {

    public String calculate() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("os.name", "unknown"));
            sb.append("|").append(System.getProperty("os.arch", "unknown"));
            sb.append("|").append(System.getProperty("user.name", "unknown"));
            sb.append("|").append(Runtime.getRuntime().availableProcessors());
            sb.append("|").append(getMacAddress());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Error calculando server hash: " + e.getMessage(), e);
        }
    }

    private String getMacAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : interfaces) {
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    return bytesToHex(mac);
                }
            }
        } catch (Exception ignored) { }
        return "no-mac";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

