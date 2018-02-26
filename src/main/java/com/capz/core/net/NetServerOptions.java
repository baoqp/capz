

package com.capz.core.net;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode
public class NetServerOptions {

    public static final int DEFAULT_PORT = 0;

    public static final String DEFAULT_HOST = "0.0.0.0";

    public static final int DEFAULT_ACCEPT_BACKLOG = -1;

    public static final boolean DEFAULT_SNI = false;

    private int port;
    private String host;
    private int acceptBacklog;
    private boolean sni;

    public NetServerOptions() {
        init();
    }


    public NetServerOptions(NetServerOptions other) {
        this.port = other.getPort();
        this.host = other.getHost();
        this.acceptBacklog = other.getAcceptBacklog();
        this.sni = other.isSni();
    }


    private void init() {
        this.port = DEFAULT_PORT;
        this.host = DEFAULT_HOST;
        this.acceptBacklog = DEFAULT_ACCEPT_BACKLOG;
        this.sni = DEFAULT_SNI;
    }

}
