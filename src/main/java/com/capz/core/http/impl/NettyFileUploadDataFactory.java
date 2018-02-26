package com.capz.core.http.impl;

import com.capz.core.Capz;
import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.http.HttpServerFileUpload;
import com.capz.core.http.HttpServerRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.FileUpload;

import java.nio.charset.Charset;
import java.util.function.Supplier;

class NettyFileUploadDataFactory extends DefaultHttpDataFactory {

    final CapzInternal capz;
    final HttpServerRequest request;
    final Supplier<Handler<HttpServerFileUpload>> lazyUploadHandler;

    NettyFileUploadDataFactory(CapzInternal capz, HttpServerRequest request, Supplier<Handler<HttpServerFileUpload>> lazyUploadHandler) {
        super(false);
        this.capz = capz;
        this.request = request;
        this.lazyUploadHandler = lazyUploadHandler;
    }

    @Override
    public FileUpload createFileUpload(HttpRequest httpRequest, String name, String filename, String contentType,
                                       String contentTransferEncoding, Charset charset, long size) {

        HttpServerFileUploadImpl upload = new HttpServerFileUploadImpl(capz, request, name, filename, contentType,
                contentTransferEncoding, charset, size);

        NettyFileUpload nettyUpload = new NettyFileUpload(upload, name, filename, contentType, contentTransferEncoding, charset);

        Handler<HttpServerFileUpload> uploadHandler = lazyUploadHandler.get();
        if (uploadHandler != null) {
            uploadHandler.handle(upload);
        }
        return nettyUpload;
    }
}