package com.example.docsigning;

import com.amazonaws.services.cloudhsmv2.AWSCloudHSMV2;
import com.amazonaws.services.cloudhsmv2.model.Cluster;
import com.amazonaws.services.cloudhsmv2.model.DescribeClustersResult;
import com.amazonaws.services.cloudhsmv2.model.Tag;
import com.example.docsigning.service.CloudHsmDocumentSigningService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

public class CloudHsmDocumentSigningServiceTest {

    @Test
    public void shouldResolveClusterByNameTagAndSignDocument() throws Exception {
        AWSCloudHSMV2 client = buildClient("hsm-cluster-name", "cluster-123", "arn:aws:cloudhsm:eu-west-1:111111111111:cluster/cluster-123");

        CloudHsmDocumentSigningService service = new CloudHsmDocumentSigningService(
                client,
                "hsm-cluster-name",
                "SunRsaSign",
                "RSA",
                "SHA256withRSA",
                2048
        );

        service.initializeKeyPairFromCloudHsm();
        byte[] signature = service.signDocument("hello".getBytes(StandardCharsets.UTF_8));

        Assert.assertEquals("cluster-123", service.getResolvedClusterIdentifier());
        Assert.assertNotNull(signature);
        Assert.assertTrue(signature.length > 0);
    }

    private AWSCloudHSMV2 buildClient(String name, String clusterId, String arn) {
        Cluster cluster = new Cluster();
        cluster.setClusterId(clusterId);
        cluster.setClusterArn(arn);
        cluster.withTagList(new Tag().withKey("Name").withValue(name));

        DescribeClustersResult result = new DescribeClustersResult().withClusters(cluster);

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("describeClusters".equals(method.getName())) {
                    return result;
                }
                if ("shutdown".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException("Method not needed in this test: " + method.getName());
            }
        };

        return (AWSCloudHSMV2) Proxy.newProxyInstance(
                AWSCloudHSMV2.class.getClassLoader(),
                new Class[]{AWSCloudHSMV2.class},
                handler
        );
    }
}
