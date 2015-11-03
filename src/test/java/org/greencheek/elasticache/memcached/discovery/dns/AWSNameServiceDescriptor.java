package org.greencheek.elasticache.memcached.discovery.dns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;


/**
 * Creates the TrackingNameService
 */
public class AWSNameServiceDescriptor implements NameServiceDescriptor {

    /**
     * Returns a reference to a TrackingNameService name server provider.
     */
    public NameService createNameService() {
        return new AWSNameService();
    }

    public String getType() {
        return "dns";
    }

    public String getProviderName() {
        return "localdns";
    }

}