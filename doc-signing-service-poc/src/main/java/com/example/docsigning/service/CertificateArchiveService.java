package com.example.docsigning.service;

import com.example.docsigning.model.CertificateBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CertificateArchiveService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateArchiveService.class);

    public CertificateBundle fromZip(byte[] zipBytes) throws IOException {
        byte[] crtBytes = null;
        byte[] keyBytes = null;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String fileName = entry.getName().toLowerCase();
                byte[] bytes = readEntry(zis);
                if (fileName.endsWith(".crt")) {
                    crtBytes = bytes;
                    LOGGER.info("Found certificate file in ZIP: {}", entry.getName());
                } else if (fileName.endsWith(".key") || fileName.endsWith(".pem")) {
                    keyBytes = bytes;
                    LOGGER.info("Found potential private key file in ZIP: {}", entry.getName());
                }
            }
        }

        if (crtBytes == null) {
            throw new IOException("No .crt file found in DigiCert download ZIP.");
        }
        return new CertificateBundle(crtBytes, keyBytes);
    }

    private byte[] readEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = zis.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
