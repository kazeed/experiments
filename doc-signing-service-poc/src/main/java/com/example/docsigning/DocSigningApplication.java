package com.example.docsigning;

import com.example.docsigning.aws.SsmApiKeyProvider;
import com.example.docsigning.cache.RedisCertificateCache;
import com.example.docsigning.config.AppConfig;
import com.example.docsigning.digicert.DigiCertApiClient;
import com.example.docsigning.model.SignedDocument;
import com.example.docsigning.service.CertificateArchiveService;
import com.example.docsigning.service.CertificateBasedSigner;
import com.example.docsigning.service.DocumentSigningOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DocSigningApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocSigningApplication.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();
        SsmApiKeyProvider keyProvider = new SsmApiKeyProvider(config.getApiKeySsmParamName());
        String apiKey = keyProvider.getApiKey();

        DigiCertApiClient digicertApiClient = new DigiCertApiClient(
                config.getDigicertBaseUrl(),
                apiKey,
                config.getHttpConnectTimeoutMillis(),
                config.getHttpReadTimeoutMillis()
        );

        try (RedisCertificateCache redisCache = new RedisCertificateCache(
                config.getRedisEndpoint(),
                config.getRedisPort(),
                config.getRedisCacheKey(),
                config.getRedisTtlSeconds())) {

            DocumentSigningOrchestrator orchestrator = new DocumentSigningOrchestrator(
                    redisCache,
                    digicertApiClient,
                    new CertificateArchiveService(),
                    new CertificateBasedSigner()
            );

            byte[] samplePayload = "Sample XML/PDF/XLS bytes".getBytes(StandardCharsets.UTF_8);
            SignedDocument signedDocument = orchestrator.signDocument(samplePayload);
            LOGGER.info("Signature type: {}", signedDocument.getSignatureType());
            LOGGER.info("Signature payload (base64): {}", Base64.getEncoder().encodeToString(signedDocument.getSignature()));
        }
    }
}
