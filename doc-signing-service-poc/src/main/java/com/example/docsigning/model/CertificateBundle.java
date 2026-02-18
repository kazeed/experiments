package com.example.docsigning.model;

import java.util.Base64;

public class CertificateBundle {

    private final byte[] crtBytes;
    private final byte[] privateKeyBytes;

    public CertificateBundle(byte[] crtBytes, byte[] privateKeyBytes) {
        this.crtBytes = crtBytes;
        this.privateKeyBytes = privateKeyBytes;
    }

    public byte[] getCrtBytes() {
        return crtBytes;
    }

    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    public String serialize() {
        String crt = Base64.getEncoder().encodeToString(crtBytes);
        String privateKey = privateKeyBytes == null ? "" : Base64.getEncoder().encodeToString(privateKeyBytes);
        return crt + ":" + privateKey;
    }

    public static CertificateBundle deserialize(String value) {
        String[] parts = value.split(":", -1);
        byte[] crt = Base64.getDecoder().decode(parts[0]);
        byte[] key = parts.length > 1 && !parts[1].isEmpty() ? Base64.getDecoder().decode(parts[1]) : null;
        return new CertificateBundle(crt, key);
    }
}
