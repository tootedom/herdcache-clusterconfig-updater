package org.greencheek.elasticache.memcached.discovery.dns;

import sun.net.spi.nameservice.NameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class AWSNameService implements NameService {

    public static final String CUSTOM_HOST_NAME = "aws.localhost.com";

    public static byte[] LOOKBACK = {0x7f, 0x00, 0x00, 0x01};

    public static final String TRACKING_NAME_SERVICE_ENABLED_PROPERTY = "aws.nameservice.enabled";

    public AWSNameService() {

    }

    @Override
    public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
        if (isNameServiceEnabled()) {
            if(s.matches("^.*"+CUSTOM_HOST_NAME+"$")) {
                return new InetAddress[]{InetAddress.getByAddress(CUSTOM_HOST_NAME, LOOKBACK)};
            } else {
                throw new UnknownHostException("This Resolver is not enabled.");
            }

        } else {
            throw new UnknownHostException("This Resolver is not enabled.");
        }
    }

    @Override
    public String getHostByAddr(byte[] bytes) throws UnknownHostException {
        if(isNameServiceEnabled()) {
            return null;
        } else {
            throw new UnknownHostException("This Resolver is not enabled.");
        }
    }

    private static boolean isNameServiceEnabled() {
        return Boolean.parseBoolean(System.getProperty(TRACKING_NAME_SERVICE_ENABLED_PROPERTY, "false"));
    }


}
