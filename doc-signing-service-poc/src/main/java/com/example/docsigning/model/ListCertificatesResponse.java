package com.example.docsigning.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListCertificatesResponse {

    @JsonAlias({"certificates", "items"})
    private List<DigiCertCertificateSummary> certificates;

    public List<DigiCertCertificateSummary> getCertificates() {
        return certificates == null ? Collections.<DigiCertCertificateSummary>emptyList() : certificates;
    }

    public void setCertificates(List<DigiCertCertificateSummary> certificates) {
        this.certificates = certificates;
    }
}
