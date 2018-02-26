package com.capz.core.http.impl;

import com.capz.core.Handler;
import com.capz.core.http.HttpConnection;
import com.capz.core.http.HttpServerOptions;
import com.capz.core.impl.AbstractContext;
import com.capz.core.net.impl.HandlerHolder;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Http1xServerHandler extends CapzHttpHandler<Http1xServerConnection> {

    private static final Logger log = LoggerFactory.getLogger(Http1xServerHandler.class);


    private final String serverOrigin;
    private final HttpServerOptions options;
    private final HandlerHolder<HttpHandlers> holder;

    public Http1xServerHandler(HttpServerOptions options, String serverOrigin, HandlerHolder<HttpHandlers> holder) {
        this.holder = holder;
        this.options = options;
        this.serverOrigin = serverOrigin;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        Http1xServerConnection conn = new Http1xServerConnection(holder.context.owner(),
                options, ctx, holder.context, serverOrigin);
        setConnection(conn);
        conn.requestHandler(holder.handler.requesthHandler);
        holder.context.executeFromIO(() -> {
            Handler<HttpConnection> connHandler = holder.handler.connectionHandler;
            if (connHandler != null) {
                connHandler.handle(conn);
            }
        });
    }

    @Override
    protected void handleMessage(Http1xServerConnection conn, AbstractContext context,
                                 ChannelHandlerContext chctx, Object msg) {
        conn.handleMessage(msg);
    }

}
