package org.greencheek.elasticache.memcached.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically calls a {@link ClusterConfigDiscovery} class and updates
 * a {@link ClusterConfigProcessor}
 */
public class ScheduledClusterConfigDiscovery implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledClusterConfigDiscovery.class);

    public static final long FIVE_MINUTE = 300000;
    public static final long ONE_MINUTE = 60000;
    public static final long TWO_MINUTE = 120000;

    private final ScheduledExecutorService executorService;
    private final ClusterConfigDiscovery discovery;
    private final ClusterConfigProcessor processor;

    public ScheduledClusterConfigDiscovery(ClusterConfigDiscovery discovery, ClusterConfigProcessor processor) {
        this(discovery,processor,TWO_MINUTE);
    }

    public ScheduledClusterConfigDiscovery(ClusterConfigDiscovery discovery,
                                           ClusterConfigProcessor processor,
                                           long scheduleInMillis) {
        this(discovery, processor,scheduleInMillis,Executors.newSingleThreadScheduledExecutor());
    }

    public ScheduledClusterConfigDiscovery(ClusterConfigDiscovery discovery,
                                           ClusterConfigProcessor processor,
                                           long scheduleInMillis,ScheduledExecutorService executorService) {
        this.discovery = discovery;
        this.processor = processor;
        this.executorService = executorService;

        executorService.scheduleAtFixedRate(this, 0, scheduleInMillis, TimeUnit.MILLISECONDS);

    }


    @Override
    public void run() {
        try {
            String cluster = discovery.discoverClusterConfiguration();
            LOG.debug("Cluster Configuration URL is: {}", cluster);
            processor.process(cluster);
        } catch (Throwable e) {
            LOG.warn("Failure during background route polling",e);
        }
    }

    public void shutdownNow() {
        this.executorService.shutdownNow();
    }
}
