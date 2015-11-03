package org.greencheek.elasticache.memcached.discovery;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SDKGlobalConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigServerUpdater;
import org.greencheek.elasticache.memcached.discovery.dns.AWSNameService;
import org.greencheek.elasticache.memcached.discovery.herdcache.ConfigServerUpdater;
import org.greencheek.elasticache.memcached.discovery.util.PortUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ElasticacheDescribeClusterConfigDiscoveryTest {

    public int port = PortUtil.findFreePort();
    public int sslPort = PortUtil.findFreePort();

    private static final String KEYSTORE_PATH;

    static {
        java.net.URL url = ElasticacheDescribeClusterConfigDiscoveryTest.class.getResource("/sslcerts/server.jks");
        String path = "";
        try {
            java.nio.file.Path resPath = java.nio.file.Paths.get(url.toURI());
            path = resPath.toFile().getAbsolutePath();
            System.out.println("keystore path: "+path);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        KEYSTORE_PATH = path;

    }


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(port).httpsPort(sslPort).needClientAuth(false).keystorePath(KEYSTORE_PATH));

    @Before
    public void setUp() throws URISyntaxException, NoSuchAlgorithmException, UnsupportedEncodingException {

        System.setProperty(AWSNameService.TRACKING_NAME_SERVICE_ENABLED_PROPERTY, "true");
        System.setProperty(SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY, "accessKey");
        System.setProperty(SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY, "secretKey");
        System.setProperty(ElasticacheDescribeClusterConfigDiscovery.ELASTICACHE_ENDPOINT, "https://" + AWSNameService.CUSTOM_HOST_NAME + ":" + sslPort);

        String response = "<DescribeCacheClustersResponse xmlns=\"http://elasticache.amazonaws.com/doc/2015-02-02/\">\n" +
                "  <DescribeCacheClustersResult>\n" +
                "    <CacheClusters>\n" +
                "      <CacheCluster>\n" +
                "        <CacheParameterGroup>\n" +
                "          <ParameterApplyStatus>in-sync</ParameterApplyStatus>\n" +
                "          <CacheParameterGroupName>default.memcached1.4</CacheParameterGroupName>\n" +
                "          <CacheNodeIdsToReboot/>\n" +
                "        </CacheParameterGroup>\n" +
                "        <CacheClusterId>simcoprod42</CacheClusterId>\n" +
                "        <CacheClusterStatus>available</CacheClusterStatus>\n" +
                "        <ConfigurationEndpoint>\n" +
                "          <Port>11211</Port>\n" +
                "          <Address>simcoprod42.m2st2p.cfg.cache.amazonaws.com</Address>\n" +
                "        </ConfigurationEndpoint>\n" +
                "        <ClientDownloadLandingPage>\n" +
                "          https://console.aws.amazon.com/elasticache/home#client-download:\n" +
                "        </ClientDownloadLandingPage>\n" +
                "        <CacheNodeType>cache.m1.large</CacheNodeType>\n" +
                "        <Engine>memcached</Engine>\n" +
                "        <PendingModifiedValues/>\n" +
                "        <PreferredAvailabilityZone>us-west-2c</PreferredAvailabilityZone>\n" +
                "        <CacheClusterCreateTime>2015-02-02T01:21:46.607Z</CacheClusterCreateTime>\n" +
                "        <EngineVersion>1.4.5</EngineVersion>\n" +
                "        <AutoMinorVersionUpgrade>true</AutoMinorVersionUpgrade>\n" +
                "        <PreferredMaintenanceWindow>fri:08:30-fri:09:30</PreferredMaintenanceWindow>\n" +
                "        <CacheSecurityGroups>\n" +
                "          <CacheSecurityGroup>\n" +
                "            <CacheSecurityGroupName>default</CacheSecurityGroupName>\n" +
                "            <Status>active</Status>\n" +
                "          </CacheSecurityGroup>\n" +
                "        </CacheSecurityGroups>\n" +
                "        <NotificationConfiguration>\n" +
                "          <TopicStatus>active</TopicStatus>\n" +
                "          <TopicArn>arn:aws:sns:us-west-2:123456789012:ElastiCacheNotifications</TopicArn>\n" +
                "        </NotificationConfiguration>\n" +
                "        <NumCacheNodes>6</NumCacheNodes>\n" +
                "      </CacheCluster>\n" +
                "    </CacheClusters>\n" +
                "  </DescribeCacheClustersResult>\n" +
                "  <ResponseMetadata>\n" +
                "    <RequestId>f270d58f-b7fb-11e0-9326-b7275b9d4a6c</RequestId>\n" +
                "  </ResponseMetadata>\n" +
                "</DescribeCacheClustersResponse>";



        stubFor(post(urlMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("x-amzn-RequestId", "12345")
                        .withHeader("Content-Type", "application/xml")
                        .withBody(response)));


    }

    @After
    public void after() {
//        System.clearProperty("javax.net.ssl.trustStore");
//        System.clearProperty("javax.net.ssl.trustStoreType");
    }

    @Test
    public void testLoadConfiguration() {
        ClusterConfigDiscovery discovery = new ElasticacheDescribeClusterConfigDiscovery("ecm-dev-myapp");

        String url = discovery.discoverClusterConfiguration();

        assertEquals("cluster config url does not match","simcoprod42.m2st2p.cfg.cache.amazonaws.com:11211",url);

    }

    @Test
    public void testScheduledConfigurationLoading() {
        final AtomicReference<String> field = new AtomicReference<>("init");
        ClusterConfigProcessor processor = new ClusterConfigProcessor() {
            @Override
            public void process(String config) {
                field.set(config);
            }
        };
        ScheduledClusterConfigDiscovery discovery = new ScheduledClusterConfigDiscovery(new ElasticacheDescribeClusterConfigDiscovery("ecm-dev-myapp"),processor,1000);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        discovery.shutdownNow();
        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/.*")));

        assertTrue(requests.size()>3);
    }

    @Test
    public void testConfigServerUpdater() {
        final AtomicReference<String> field = new AtomicReference<>("init");

        ClusterConfigProcessor processor = new ConfigServerUpdater(new ElastiCacheConfigServerUpdater() {
            @Override
            public void setUpdateConsumer(Consumer<String> l) {

            }

            @Override
            public void connectionUpdated(String connectionString) {
                field.set(connectionString);
            }
        });
        ScheduledClusterConfigDiscovery discovery = new ScheduledClusterConfigDiscovery(new ElasticacheDescribeClusterConfigDiscovery("ecm-dev-myapp"),processor,1000);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        discovery.shutdownNow();
        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/.*")));

        assertTrue(requests.size()>3);
        assertNotEquals("init",field.get());
    }

}