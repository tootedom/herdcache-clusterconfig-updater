package org.greencheek.elasticache.memcached.discovery;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Uses the AWS SDK to find a cluster configuration url for a
 * cluster with a given cluster name
 */
public class ElasticacheDescribeClusterConfigDiscovery implements ClusterConfigDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticacheDescribeClusterConfigDiscovery.class);

    public static final String ELASTICACHE_ENDPOINT = "OVERRIDING_ELASTICACHE_ENDPOINT";


    private static final Integer TWENTY = 20;
    private static final String NO_CLUSTER = "";
    private final String clusterName;
    private final AmazonElastiCacheClient client;

    public ElasticacheDescribeClusterConfigDiscovery(String clusterName) {
        this(clusterName,defaultConfig());
    }

    public ElasticacheDescribeClusterConfigDiscovery(String clusterName, ClientConfiguration configuration) {
        this(clusterName,System.getProperty(ELASTICACHE_ENDPOINT,null),configuration);
    }

    private ElasticacheDescribeClusterConfigDiscovery(String clusterName,String endpoint,ClientConfiguration configuration) {
        this.clusterName = clusterName;
        client = new AmazonElastiCacheClient(configuration);
        if(endpoint!=null) {
           client.setEndpoint(endpoint);
        }
    }

    public static ClientConfiguration defaultConfig() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout(5000);
        clientConfiguration.setRequestTimeout(5000);
        clientConfiguration.setConnectionTTL(30000);
        clientConfiguration.setMaxErrorRetry(1);
        return clientConfiguration;
    }

    @Override
    public String discoverClusterConfiguration() {
        DescribeCacheClustersRequest request = new DescribeCacheClustersRequest().withCacheClusterId(clusterName);
        request.setMaxRecords(TWENTY);

        try {
            DescribeCacheClustersResult result = client.describeCacheClusters(request);
            List<CacheCluster> clusterList = result.getCacheClusters();
            if(clusterList.size()>0) {
                CacheCluster cluster = clusterList.get(0);
                Endpoint endpoint = cluster.getConfigurationEndpoint();
                return endpoint.getAddress() + ":" + endpoint.getPort();
            } else {
                return NO_CLUSTER;
            }
        } catch(Exception e) {
            e.printStackTrace();
            LOG.error("Exception describing elasticache cluster",e);
            return NO_CLUSTER;
        }


    }
}
