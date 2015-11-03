package org.greencheek.elasticache.memcached.discovery.herdcache;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigServerUpdater;
import org.greencheek.elasticache.memcached.discovery.ClusterConfigProcessor;

/**
 * Config process that takes a configuration url and calls the elastic cache ElastiCacheConfigServerUpdater
 * to inform the cache that an update has occurred.  This is only done if the url is not null or empty string
 */
public class ConfigServerUpdater implements ClusterConfigProcessor {

    private final ElastiCacheConfigServerUpdater clusterConfigUpdater;

    public ConfigServerUpdater(ElastiCacheConfigServerUpdater updater) {
        clusterConfigUpdater = updater;
    }

    @Override
    public void process(String config) {
        if(config!=null && config.trim().length()>0) {
            clusterConfigUpdater.connectionUpdated(config);
        }
    }
}
