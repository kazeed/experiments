package com.example.docsigning.model;

public class SignedDocument {

    private final byte[] originalDocument;
    private final byte[] signature;
    private final byte[] certificate;
    private final String signatureType;

    public SignedDocument(byte[] originalDocument, byte[] signature, byte[] certificate, String signatureType) {
        this.originalDocument = originalDocument;
        this.signature = signature;
        this.certificate = certificate;
        this.signatureType = signatureType;
    }

    public byte[] getOriginalDocument() {
        return originalDocument;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public String getSignatureType() {
        return signatureType;
    }
}
