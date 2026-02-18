package com.example.docsigning;

import com.example.docsigning.model.CertificateBundle;
import com.example.docsigning.service.CertificateArchiveService;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CertificateArchiveServiceTest {

    @Test
    public void shouldExtractCrtAndKeyFromZip() throws Exception {
        byte[] zipBytes = buildZip();

        CertificateArchiveService service = new CertificateArchiveService();
        CertificateBundle bundle = service.fromZip(zipBytes);

        Assert.assertEquals("CRT", new String(bundle.getCrtBytes(), StandardCharsets.UTF_8));
        Assert.assertEquals("KEY", new String(bundle.getPrivateKeyBytes(), StandardCharsets.UTF_8));
    }

    private byte[] buildZip() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(out);

        zos.putNextEntry(new ZipEntry("certificate.crt"));
        zos.write("CRT".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.putNextEntry(new ZipEntry("private.key"));
        zos.write("KEY".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.close();
        return out.toByteArray();
    }
}
