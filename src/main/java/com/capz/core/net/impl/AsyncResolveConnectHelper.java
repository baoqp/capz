package com.capz.core.net.impl;

import com.capz.core.AsyncResult;
import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.impl.Future;
import com.capz.core.net.SocketAddress;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class AsyncResolveConnectHelper {

    private List<Handler<AsyncResult<Channel>>> handlers = new ArrayList<>();
    private ChannelFuture future;
    private AsyncResult<Channel> result;

    public synchronized void addListener(Handler<AsyncResult<Channel>> handler) {
        if (result != null) {
            if (future != null) {
                future.addListener(v -> handler.handle(result));
            } else {
                handler.handle(result);
            }
        } else {
            handlers.add(handler);
        }
    }

    private synchronized void handle(ChannelFuture cf, AsyncResult<Channel> res) {
        if (result == null) {
            for (Handler<AsyncResult<Channel>> handler : handlers) {
                handler.handle(res);
            }
            future = cf;
            result = res;
        } else {
            throw new IllegalStateException("Already complete!");
        }
    }

    private static void checkPort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port " + port);
        }
    }


    public static java.net.SocketAddress convert(com.capz.core.net.SocketAddress address, boolean resolved) {
        if (address.path() != null) {
            throw new IllegalArgumentException("Domain socket not supported by JDK transport");
        } else {
            if (resolved) {
                return new InetSocketAddress(address.host(), address.port());
            } else {
                return InetSocketAddress.createUnresolved(address.host(), address.port());
            }
        }
    }


    public static AsyncResolveConnectHelper doBind(CapzInternal capz, SocketAddress socketAddress,
                                                   ServerBootstrap bootstrap) {
        AsyncResolveConnectHelper asyncResolveConnectHelper = new AsyncResolveConnectHelper();
        bootstrap.channel(NioServerSocketChannel.class);

        java.net.SocketAddress converted = convert(socketAddress, true);

        ChannelFuture future = bootstrap.bind(converted);

        future.addListener(f -> {
            if (f.isSuccess()) {
                asyncResolveConnectHelper.handle(future, Future.succeededFuture(future.channel()));
            } else {
                asyncResolveConnectHelper.handle(future, Future.failedFuture(f.cause()));
            }
        });

        return asyncResolveConnectHelper;
    }
}
