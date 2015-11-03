package org.greencheek.elasticache.memcached.discovery;

/**
 * Implementations are provided with the Cluster Configuration URL,
 * and it is upto the implementation to decide what to do with that url.
 * The implementation is responsible for determining if the url is a new url
 * or not; or if it is an empty string (or null)
 */
public interface ClusterConfigProcessor {
    public void process(String config);
}

