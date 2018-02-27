package com.capz.core.http.impl;

import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.file.AsyncFile;
import com.capz.core.file.AsyncFileImpl;
import com.capz.core.file.OpenOptions;
import com.capz.core.http.HttpServerFileUpload;
import com.capz.core.http.HttpServerRequest;
import com.capz.core.streams.Pump;
import com.capz.core.streams.impl.PumpImpl;

import java.nio.charset.Charset;

class HttpServerFileUploadImpl implements HttpServerFileUpload {

    private final HttpServerRequest req;
    private final CapzInternal capz;
    private final String name;
    private final String filename;
    private final String contentType;
    private final String contentTransferEncoding;
    private final Charset charset;

    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private Handler<Throwable> exceptionHandler;

    private AsyncFile file;

    private long size;
    private boolean paused;
    private Buffer pauseBuff;
    private boolean complete;
    private boolean lazyCalculateSize;

    HttpServerFileUploadImpl(CapzInternal capz, HttpServerRequest req, String name, String filename, String contentType,
                             String contentTransferEncoding, Charset charset, long size) {
        this.capz = capz;
        this.req = req;
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.charset = charset;
        this.size = size;
        if (size == 0) {
            lazyCalculateSize = true;
        }
    }

    @Override
    public String filename() {
        return filename;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public String contentTransferEncoding() {
        return contentTransferEncoding;
    }

    @Override
    public String charset() {
        return charset.toString();
    }

    @Override
    public synchronized long size() {
        return size;
    }

    @Override
    public synchronized HttpServerFileUpload handler(Handler<Buffer> handler) {
        this.dataHandler = handler;
        return this;
    }


    @Override
    public synchronized HttpServerFileUpload exceptionHandler(Handler<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    @Override
    public synchronized HttpServerFileUpload endHandler(Handler<Void> handler) {
        this.endHandler = handler;
        return this;
    }



    @Override
    public synchronized HttpServerFileUpload pause() {
        req.pause();
        paused = true;
        return this;
    }

    @Override
    public synchronized HttpServerFileUpload resume() {
        if (paused) {
            req.resume();
            paused = false;
            if (pauseBuff != null) {
                doReceiveData(pauseBuff);
                pauseBuff = null;
            }
            if (complete) {
                handleComplete();
            }
        }
        return this;
    }


    @Override
    public HttpServerFileUpload streamToFileSystem(String filename) {
        // TODO 异步 ???
        pause();
        file = new AsyncFileImpl(capz, filename, new OpenOptions(), capz.getOrCreateContext());
        Pump p = new PumpImpl(HttpServerFileUploadImpl.this, file);
        p.start();
        resume();
        return this;
    }

    @Override
    public synchronized boolean isSizeAvailable() {
        return !lazyCalculateSize;
    }

    synchronized void receiveData(Buffer data) {
        if (data.length() != 0) {
            // Can sometimes receive zero length packets from Netty!
            if (lazyCalculateSize) {
                size += data.length();
            }
            doReceiveData(data);
        }
    }

    synchronized void doReceiveData(Buffer data) {
        if (!paused) {
            if (dataHandler != null) {
                dataHandler.handle(data);
            }
        } else {
            if (pauseBuff == null) {
                pauseBuff = Buffer.buffer();
            }
            pauseBuff.appendBuffer(data);
        }
    }

    synchronized void complete() {
        if (paused) {
            complete = true;
        } else {
            handleComplete();
        }
    }

    private void handleComplete() {
        lazyCalculateSize = false;
        if (file == null) {
            notifyEndHandler();
        } else {
            file.close(ar -> {
                if (ar.failed()) {
                    notifyExceptionHandler(ar.cause());
                }
                notifyEndHandler();
            });
        }
    }

    private void notifyEndHandler() {
        if (endHandler != null) {
            endHandler.handle(null);
        }
    }

    private void notifyExceptionHandler(Throwable cause) {
        if (exceptionHandler != null) {
            exceptionHandler.handle(cause);
        }
    }
}
