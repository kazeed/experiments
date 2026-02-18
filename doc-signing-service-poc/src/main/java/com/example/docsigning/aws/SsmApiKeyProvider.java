package com.example.docsigning.aws;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SsmApiKeyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SsmApiKeyProvider.class);

    private final AWSSimpleSystemsManagement ssmClient;
    private final String apiKeyParameterName;

    public SsmApiKeyProvider(String apiKeyParameterName) {
        this(AWSSimpleSystemsManagementClientBuilder.defaultClient(), apiKeyParameterName);
    }

    SsmApiKeyProvider(AWSSimpleSystemsManagement ssmClient, String apiKeyParameterName) {
        this.ssmClient = ssmClient;
        this.apiKeyParameterName = apiKeyParameterName;
    }

    public String getApiKey() {
        LOGGER.info("Reading DigiCert API key from AWS SSM parameter: {}", apiKeyParameterName);
        GetParameterResult result = ssmClient.getParameter(new GetParameterRequest()
                .withName(apiKeyParameterName)
                .withWithDecryption(true));
        return result.getParameter().getValue();
    }
}
