package org.greencheek.elasticache.memcached.discovery.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * get a random ephemeral port
 */
public class PortUtil {
    /**
     * Return a random ephemeral port
     *
     * @return
     */
    public static int findFreePort() {
        ServerSocket server = null;
        int port = 8909;
        try {
            server = new ServerSocket(0, 1000, InetAddress.getLocalHost());
            server.setReuseAddress(true);
            port = server.getLocalPort();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }
}
