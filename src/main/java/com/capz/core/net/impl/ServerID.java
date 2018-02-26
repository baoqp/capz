package com.capz.core.net.impl;

import java.io.Serializable;

public class ServerID implements Serializable {

    public int port;
    public String host;

    public ServerID(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public ServerID() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ServerID)) return false;

        ServerID serverID = (ServerID) o;

        if (port != serverID.port) return false;
        if (!host.equals(serverID.host)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = port;
        result = 31 * result + host.hashCode();
        return result;
    }

    public String toString() {
        return host + ":" + port;
    }
}