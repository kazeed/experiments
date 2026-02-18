package com.example.docsigning.digicert;

import com.example.docsigning.model.DigiCertCertificateSummary;
import com.example.docsigning.model.ListCertificatesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DigiCertApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DigiCertApiClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ObjectMapper objectMapper;

    public DigiCertApiClient(String baseUrl, String apiKey, int connectTimeoutMillis, int readTimeoutMillis) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.objectMapper = new ObjectMapper();
    }

    public DigiCertCertificateSummary selectCertificate() throws IOException {
        List<DigiCertCertificateSummary> certificates = listCertificates();
        Optional<DigiCertCertificateSummary> franceCertificate = certificates.stream()
                .filter(c -> c.getCountry() != null && "FR".equalsIgnoreCase(c.getCountry()))
                .findFirst();
        if (franceCertificate.isPresent()) {
            LOGGER.info("Selected DigiCert certificate ID {} (country=FR)", franceCertificate.get().getId());
            return franceCertificate.get();
        }

        Optional<DigiCertCertificateSummary> fallback = certificates.stream()
                .filter(c -> c.getStatus() != null && "issued".equalsIgnoreCase(c.getStatus()))
                .findFirst();

        if (fallback.isPresent()) {
            LOGGER.info("No FR certificate found; using fallback certificate ID {} with status {}",
                    fallback.get().getId(), fallback.get().getStatus());
            return fallback.get();
        }

        if (certificates.isEmpty()) {
            throw new IOException("No certificates returned by DigiCert List Certificates API.");
        }

        LOGGER.info("No FR/issued filter match found; selecting first available certificate ID {}", certificates.get(0).getId());
        return certificates.get(0);
    }

    public List<DigiCertCertificateSummary> listCertificates() throws IOException {
        String endpoint = baseUrl + "/certificate";
        LOGGER.info("Calling DigiCert List Certificates endpoint: {}", endpoint);
        HttpURLConnection connection = openGet(endpoint);
        int status = connection.getResponseCode();
        byte[] responseBody = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
        ensureSuccess(status, responseBody);

        ListCertificatesResponse response = objectMapper.readValue(responseBody, ListCertificatesResponse.class);
        LOGGER.info("DigiCert List Certificates returned {} certificates", response.getCertificates().size());
        return response.getCertificates();
    }

    public byte[] downloadCertificateZip(long certificateId) throws IOException {
        String endpoint = String.format(Locale.ROOT, "%s/certificate/%d/download", baseUrl, certificateId);
        LOGGER.info("Calling DigiCert Download Certificate endpoint: {}", endpoint);

        HttpURLConnection connection = openGet(endpoint);
        int status = connection.getResponseCode();
        byte[] responseBody = readBody(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
        ensureSuccess(status, responseBody);

        LOGGER.info("Downloaded certificate ZIP for certificate ID {} ({} bytes)", certificateId, responseBody.length);
        return responseBody;
    }

    private HttpURLConnection openGet(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setRequestProperty("X-DC-DEVKEY", apiKey);
        connection.setRequestProperty("Accept", "application/json, application/zip");
        return connection;
    }

    private void ensureSuccess(int statusCode, byte[] body) throws IOException {
        if (statusCode < 200 || statusCode >= 300) {
            String payload = body == null ? "" : new String(body, StandardCharsets.UTF_8);
            throw new IOException("DigiCert API call failed with status " + statusCode + ". Payload: " + payload);
        }
    }

    private byte[] readBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
