package com.capz.core.net;

import com.capz.core.net.impl.SocketAddressImpl;

public interface SocketAddress {

    /**
     * Create a inet socket address, {@code host} must be non {@code null} and {@code port} must be between {@code 0}
     * and {@code 65536}.
     *
     * @param port the address port
     * @param host the address host
     * @return the created socket address
     */
    static SocketAddress inetSocketAddress(int port, String host) {
        return new SocketAddressImpl(port, host);
    }

    /**
     * Create a domain socket address.
     *
     * @param path the address path
     * @return the created socket address
     */
    static SocketAddress domainSocketAddress(String path) {
        return new SocketAddressImpl(path);
    }


    String host();


    int port();


    String path();

}
