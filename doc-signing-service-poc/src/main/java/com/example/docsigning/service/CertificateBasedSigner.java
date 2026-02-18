package com.example.docsigning.service;

import com.example.docsigning.model.CertificateBundle;
import com.example.docsigning.model.SignedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CertificateBasedSigner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateBasedSigner.class);

    public SignedDocument sign(byte[] document, CertificateBundle bundle) throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(bundle.getCrtBytes()));

        if (bundle.getPrivateKeyBytes() == null) {
            LOGGER.warn("No private key found in certificate bundle; creating attestation digest instead of RSA signature");
            byte[] attestation = buildCertificateBoundDigest(document, bundle.getCrtBytes());
            return new SignedDocument(document, attestation, bundle.getCrtBytes(), "CERT_BOUND_SHA256_DIGEST");
        }

        PrivateKey privateKey = parsePrivateKey(bundle.getPrivateKeyBytes());

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(document);
        byte[] signatureBytes = signature.sign();

        LOGGER.info("Document signed successfully using certificate subject: {}", certificate.getSubjectX500Principal().getName());
        return new SignedDocument(document, signatureBytes, bundle.getCrtBytes(), "SHA256withRSA");
    }

    private PrivateKey parsePrivateKey(byte[] keyBytes) throws Exception {
        String keyText = new String(keyBytes, StandardCharsets.UTF_8);

        if (keyText.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            throw new IllegalStateException("PKCS#1 private keys are not supported in this POC. Provide PKCS#8 PEM (BEGIN PRIVATE KEY).");
        }

        String keyContent = keyText
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private byte[] buildCertificateBoundDigest(byte[] document, byte[] certificate) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(document);
        digest.update(certificate);
        return digest.digest();
    }
}
