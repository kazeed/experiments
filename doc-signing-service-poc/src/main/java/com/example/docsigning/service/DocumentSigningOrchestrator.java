package com.example.docsigning.service;

import com.example.docsigning.cache.RedisCertificateCache;
import com.example.docsigning.digicert.DigiCertApiClient;
import com.example.docsigning.model.CertificateBundle;
import com.example.docsigning.model.DigiCertCertificateSummary;
import com.example.docsigning.model.SignedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentSigningOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSigningOrchestrator.class);

    private final RedisCertificateCache redisCache;
    private final DigiCertApiClient digicertApiClient;
    private final CertificateArchiveService archiveService;
    private final CertificateBasedSigner signer;

    public DocumentSigningOrchestrator(RedisCertificateCache redisCache,
                                       DigiCertApiClient digicertApiClient,
                                       CertificateArchiveService archiveService,
                                       CertificateBasedSigner signer) {
        this.redisCache = redisCache;
        this.digicertApiClient = digicertApiClient;
        this.archiveService = archiveService;
        this.signer = signer;
    }

    public SignedDocument signDocument(byte[] documentBytes) throws Exception {
        LOGGER.info("Starting document signing workflow for payload with {} bytes", documentBytes.length);
        CertificateBundle bundle = redisCache.get();
        if (bundle == null) {
            bundle = retrieveFromDigicert();
            redisCache.put(bundle);
        }

        SignedDocument signedDocument = signer.sign(documentBytes, bundle);
        LOGGER.info("Signing workflow completed");
        return signedDocument;
    }

    private CertificateBundle retrieveFromDigicert() throws Exception {
        LOGGER.info("Retrieving certificate from DigiCert because cache is empty");
        DigiCertCertificateSummary selected = digicertApiClient.selectCertificate();
        byte[] archiveBytes = digicertApiClient.downloadCertificateZip(selected.getId());
        CertificateBundle bundle = archiveService.fromZip(archiveBytes);
        LOGGER.info("Certificate successfully extracted from DigiCert ZIP response");
        return bundle;
    }
}
