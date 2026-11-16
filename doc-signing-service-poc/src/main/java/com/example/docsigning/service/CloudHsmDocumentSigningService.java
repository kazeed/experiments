package com.example.docsigning.service;

import com.amazonaws.services.cloudhsmv2.AWSCloudHSMV2;
import com.amazonaws.services.cloudhsmv2.model.Cluster;
import com.amazonaws.services.cloudhsmv2.model.DescribeClustersRequest;
import com.amazonaws.services.cloudhsmv2.model.DescribeClustersResult;
import com.amazonaws.services.cloudhsmv2.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;

/**
 * POC service that resolves an AWS CloudHSM cluster by identifier/ARN/name and then
 * uses a configured JCE provider (for example CloudHSM client provider) to create a key pair
 * and sign document bytes.
 */
public class CloudHsmDocumentSigningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudHsmDocumentSigningService.class);

    private final AWSCloudHSMV2 cloudHsmClient;
    private final String clusterReference;
    private final String keyAlgorithm;
    private final String signatureAlgorithm;
    private final int keySize;
    private final String securityProvider;

    private String resolvedClusterIdentifier;
    private KeyPair keyPair;

    public CloudHsmDocumentSigningService(AWSCloudHSMV2 cloudHsmClient,
                                          String clusterReference,
                                          String securityProvider) {
        this(cloudHsmClient, clusterReference, securityProvider, "RSA", "SHA256withRSA", 2048);
    }

    public CloudHsmDocumentSigningService(AWSCloudHSMV2 cloudHsmClient,
                                          String clusterReference,
                                          String securityProvider,
                                          String keyAlgorithm,
                                          String signatureAlgorithm,
                                          int keySize) {
        this.cloudHsmClient = cloudHsmClient;
        this.clusterReference = clusterReference;
        this.securityProvider = securityProvider;
        this.keyAlgorithm = keyAlgorithm;
        this.signatureAlgorithm = signatureAlgorithm;
        this.keySize = keySize;
    }

    public synchronized void initializeKeyPairFromCloudHsm() throws Exception {
        Cluster cluster = findCluster(clusterReference);
        if (cluster == null) {
            throw new IllegalArgumentException("No CloudHSM cluster matches reference: " + clusterReference);
        }

        resolvedClusterIdentifier = cluster.getClusterId();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm, securityProvider);
        keyPairGenerator.initialize(keySize);
        keyPair = keyPairGenerator.generateKeyPair();

        LOGGER.info("Generated {} key pair using provider {} for CloudHSM cluster {}", keyAlgorithm, securityProvider, resolvedClusterIdentifier);
    }

    public synchronized byte[] signDocument(byte[] document) throws Exception {
        if (keyPair == null) {
            throw new IllegalStateException("Key pair is not initialized. Call initializeKeyPairFromCloudHsm() first.");
        }

        Signature signature = Signature.getInstance(signatureAlgorithm, securityProvider);
        signature.initSign(keyPair.getPrivate());
        signature.update(document);
        return signature.sign();
    }

    public synchronized String getResolvedClusterIdentifier() {
        return resolvedClusterIdentifier;
    }

    private Cluster findCluster(String reference) {
        List<Cluster> clusters = loadAllClusters();
        for (Cluster cluster : clusters) {
            if (reference.equals(cluster.getClusterId()) || reference.equals(cluster.getClusterArn())) {
                return cluster;
            }
            if (hasNameTag(cluster, reference)) {
                return cluster;
            }
        }
        return null;
    }

    private List<Cluster> loadAllClusters() {
        List<Cluster> clusters = new ArrayList<Cluster>();
        String nextToken = null;

        do {
            DescribeClustersRequest request = new DescribeClustersRequest();
            request.setNextToken(nextToken);

            DescribeClustersResult result = cloudHsmClient.describeClusters(request);
            if (result.getClusters() != null) {
                clusters.addAll(result.getClusters());
            }

            nextToken = result.getNextToken();
        } while (nextToken != null && !nextToken.trim().isEmpty());

        return clusters;
    }

    private boolean hasNameTag(Cluster cluster, String name) {
        if (cluster.getTagList() == null) {
            return false;
        }

        for (Tag tag : cluster.getTagList()) {
            if ("Name".equals(tag.getKey()) && name.equals(tag.getValue())) {
                return true;
            }
        }

        return false;
    }
}
