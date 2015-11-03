package org.greencheek.elasticache.memcached.discovery;

/**
 * Implementation performs the discovery for the new Cluster Confugration url.
 * The discovery mechanism is needed if an entire cluster is destroyed and then recreated.
 * This could occur in several circumstances: Stopping the elasticache cluster in the evening (dev env)
 * or having to rebuild the cluster to a new machine type.
 */
public interface ClusterConfigDiscovery {
    public String discoverClusterConfiguration();
}
