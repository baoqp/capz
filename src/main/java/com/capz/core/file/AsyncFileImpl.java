package com.capz.core.file;

import com.capz.core.AsyncResult;
import com.capz.core.CapzInternal;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.impl.AbstractContext;
import com.capz.core.impl.Future;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class AsyncFileImpl implements AsyncFile {

    private static final Logger log = LoggerFactory.getLogger(AsyncFile.class);

    public static final int DEFAULT_READ_BUFFER_SIZE = 8192;

    private final CapzInternal capz;
    private final AsynchronousFileChannel ch;
    private final AbstractContext context;
    private boolean closed;
    private Runnable closedDeferred;
    private long writesOutstanding;
    private Handler<Throwable> exceptionHandler;
    private Handler<Void> drainHandler;
    private long writePos;
    private int maxWrites = 128 * 1024;
    private int lwm = maxWrites / 2;
    private int readBufferSize = DEFAULT_READ_BUFFER_SIZE;
    private boolean paused;
    private Handler<Buffer> dataHandler;
    private Handler<Void> endHandler;
    private long readPos;
    private boolean readInProgress;

    public AsyncFileImpl(CapzInternal capz, String path, OpenOptions options, AbstractContext context) {
        this.capz = capz;
        this.context = context;
        Path file = Paths.get(path);
        HashSet<OpenOption> opts = new HashSet<>();
        if (options.isRead()) opts.add(StandardOpenOption.READ);
        if (options.isWrite()) opts.add(StandardOpenOption.WRITE);
        if (options.isCreate()) opts.add(StandardOpenOption.CREATE);
        if (options.isCreateNew()) opts.add(StandardOpenOption.CREATE_NEW);
        if (options.isSync()) opts.add(StandardOpenOption.SYNC);
        if (options.isDsync()) opts.add(StandardOpenOption.DSYNC);
        if (options.isDeleteOnClose()) opts.add(StandardOpenOption.DELETE_ON_CLOSE);
        if (options.isSparse()) opts.add(StandardOpenOption.SPARSE);
        if (options.isTruncateExisting()) opts.add(StandardOpenOption.TRUNCATE_EXISTING);

        try {
            if (options.getPerms() != null) {
                FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(options.getPerms()));
                ch = AsynchronousFileChannel.open(file, opts, capz.getWorkerPool().getExecutorService(), attrs);
            } else {
                ch = AsynchronousFileChannel.open(file, opts, capz.getWorkerPool().getExecutorService());
            }
            if (options.isAppend()) writePos = ch.size();
        } catch (Exception e) {
            throw new FileSystemException(e);
        }



    }

    @Override
    public void close() {
        closeInternal(null);
    }

    @Override
    public void close(Handler<AsyncResult<Void>> handler) {
        closeInternal(handler);
    }

    @Override
    public void end() {
        close();
    }

    @Override
    public synchronized AsyncFile read(Buffer buffer, int offset, long position, int length, Handler<AsyncResult<Buffer>> handler) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(handler, "handler");
        assert offset >= 0 ;
        assert position >= 0;
        assert length >= 0;
        check();
        ByteBuffer bb = ByteBuffer.allocate(length);
        doRead(buffer, offset, bb, position, handler);
        return this;
    }

    @Override
    public AsyncFile write(Buffer buffer, long position, Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(handler, "handler");
        return doWrite(buffer, position, handler);
    }

    private synchronized AsyncFile doWrite(Buffer buffer, long position, Handler<AsyncResult<Void>> handler) {
        Objects.requireNonNull(buffer, "buffer");
        assert position >= 0 ;
        check();
        Handler<AsyncResult<Void>> wrapped = ar -> {
            if (ar.succeeded()) {
                checkContext();
                Runnable action;
                synchronized (AsyncFileImpl.this) {
                    if (writesOutstanding == 0 && closedDeferred != null) {
                        action = closedDeferred;
                    } else {
                        action = this::checkDrained;
                    }
                }
                action.run();
                if (handler != null) {
                    handler.handle(ar);
                }
            } else {
                if (handler != null) {
                    handler.handle(ar);
                } else {
                    handleException(ar.cause());
                }
            }
        };
        ByteBuf buf = buffer.getByteBuf();
        if (buf.nioBufferCount() > 1) {
            doWrite(buf.nioBuffers(), position, wrapped);
        } else {
            ByteBuffer bb = buf.nioBuffer();
            doWrite(bb, position, bb.limit(), wrapped);
        }
        return this;
    }

    @Override
    public synchronized AsyncFile write(Buffer buffer) {
        int length = buffer.length();
        doWrite(buffer, writePos, null);
        writePos += length;
        return this;
    }

    @Override
    public synchronized AsyncFile setWriteQueueMaxSize(int maxSize) {
        assert maxSize >= 2 ;
        check();
        this.maxWrites = maxSize;
        this.lwm = maxWrites / 2;
        return this;
    }

    @Override
    public synchronized AsyncFile setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    @Override
    public synchronized boolean writeQueueFull() {
        check();
        return writesOutstanding >= maxWrites;
    }

    @Override
    public synchronized AsyncFile drainHandler(Handler<Void> handler) {
        check();
        this.drainHandler = handler;
        checkDrained();
        return this;
    }

    @Override
    public synchronized AsyncFile exceptionHandler(Handler<Throwable> handler) {
        check();
        this.exceptionHandler = handler;
        return this;
    }

    @Override
    public synchronized AsyncFile handler(Handler<Buffer> handler) {
        check();
        this.dataHandler = handler;
        if (dataHandler != null && !paused && !closed) {
            doRead();
        }
        return this;
    }

    @Override
    public synchronized AsyncFile endHandler(Handler<Void> handler) {
        check();
        this.endHandler = handler;
        return this;
    }

    @Override
    public synchronized AsyncFile pause() {
        check();
        paused = true;
        return this;
    }

    @Override
    public synchronized AsyncFile resume() {
        check();
        if (paused && !closed) {
            paused = false;
            if (dataHandler != null) {
                doRead();
            }
        }
        return this;
    }


    @Override
    public AsyncFile flush() {
        doFlush(null);
        return this;
    }

    @Override
    public AsyncFile flush(Handler<AsyncResult<Void>> handler) {
        doFlush(handler);
        return this;
    }

    @Override
    public synchronized AsyncFile setReadPos(long readPos) {
        this.readPos = readPos;
        return this;
    }

    @Override
    public synchronized AsyncFile setWritePos(long writePos) {
        this.writePos = writePos;
        return this;
    }

    private synchronized void checkDrained() {
        if (drainHandler != null && writesOutstanding <= lwm) {
            Handler<Void> handler = drainHandler;
            drainHandler = null;
            handler.handle(null);
        }
    }

    private void handleException(Throwable t) {
        if (exceptionHandler != null && t instanceof Exception) {
            exceptionHandler.handle(t);
        } else {
            log.error("Unhandled exception", t);

        }
    }

    private synchronized void doWrite(ByteBuffer[] buffers, long position, Handler<AsyncResult<Void>> handler) {
        AtomicInteger cnt = new AtomicInteger();
        AtomicBoolean sentFailure = new AtomicBoolean();
        for (ByteBuffer b : buffers) {
            int limit = b.limit();
            doWrite(b, position, limit, ar -> {
                if (ar.succeeded()) {
                    if (cnt.incrementAndGet() == buffers.length) {
                        handler.handle(ar);
                    }
                } else {
                    if (sentFailure.compareAndSet(false, true)) {
                        handler.handle(ar);
                    }
                }
            });
            position += limit;
        }
    }

    private synchronized void doRead() {
        if (!readInProgress) {
            readInProgress = true;
            Buffer buff = Buffer.buffer(readBufferSize);
            read(buff, 0, readPos, readBufferSize, ar -> {
                if (ar.succeeded()) {
                    readInProgress = false;
                    Buffer buffer = ar.result();
                    if (buffer.length() == 0) {
                        // Empty buffer represents end of file
                        handleEnd();
                    } else {
                        readPos += buffer.length();
                        handleData(buffer);
                        if (!paused && dataHandler != null) {
                            doRead();
                        }
                    }
                } else {
                    handleException(ar.cause());
                }
            });
        }
    }

    private synchronized void handleData(Buffer buffer) {
        if (dataHandler != null) {
            checkContext();
            dataHandler.handle(buffer);
        }
    }

    private synchronized void handleEnd() {
        dataHandler = null;
        if (endHandler != null) {
            checkContext();
            endHandler.handle(null);
        }
    }

    private synchronized void doFlush(Handler<AsyncResult<Void>> handler) {
        checkClosed();
        context.executeBlocking(() -> {
            try {
                ch.force(false);
                return null;
            } catch (IOException e) {
                throw new FileSystemException(e);
            }
        }, handler);
    }

    private void doWrite(ByteBuffer buff, long position, long toWrite, Handler<AsyncResult<Void>> handler) {
        if (toWrite == 0) {
            throw new IllegalStateException("Cannot save zero bytes");
        }
        synchronized (this) {
            writesOutstanding += toWrite;
        }
        writeInternal(buff, position, handler);
    }

    private void writeInternal(ByteBuffer buff, long position, Handler<AsyncResult<Void>> handler) {

        ch.write(buff, position, null, new java.nio.channels.CompletionHandler<Integer, Object>() {

            public void completed(Integer bytesWritten, Object attachment) {

                long pos = position;

                if (buff.hasRemaining()) {
                    // partial write
                    pos += bytesWritten;
                    // resubmit
                    writeInternal(buff, pos, handler);
                } else {
                    // It's been fully written
                    context.runOnContext((v) -> {
                        synchronized (AsyncFileImpl.this) {
                            writesOutstanding -= buff.limit();
                        }
                        handler.handle(Future.succeededFuture());
                    });
                }
            }

            public void failed(Throwable exc, Object attachment) {
                if (exc instanceof Exception) {
                    context.runOnContext((v) -> handler.handle(Future.failedFuture(exc)));
                } else {
                    log.error("Error occurred", exc);
                }
            }
        });
    }

    private void doRead(Buffer writeBuff, int offset, ByteBuffer buff, long position, Handler<AsyncResult<Buffer>> handler) {

        ch.read(buff, position, null, new java.nio.channels.CompletionHandler<Integer, Object>() {

            long pos = position;

            private void done() {
                context.runOnContext((v) -> {
                    buff.flip();
                    writeBuff.setBytes(offset, buff);
                    handler.handle(Future.succeededFuture(writeBuff));
                });
            }

            public void completed(Integer bytesRead, Object attachment) {
                if (bytesRead == -1) {
                    //End of file
                    done();
                } else if (buff.hasRemaining()) {
                    // partial read
                    pos += bytesRead;
                    // resubmit
                    doRead(writeBuff, offset, buff, pos, handler);
                } else {
                    // It's been fully written
                    done();
                }
            }

            public void failed(Throwable t, Object attachment) {
                context.runOnContext((v) -> handler.handle(Future.failedFuture(t)));
            }
        });
    }

    private void check() {
        checkClosed();
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("File handle is closed");
        }
    }

    private void checkContext() {
        if (!capz.getContext().equals(context)) {
            throw new IllegalStateException("AsyncFile must only be used in the context that created it, expected: "
                    + context + " actual " + capz.getContext());
        }
    }

    private void doClose(Handler<AsyncResult<Void>> handler) {
        AbstractContext handlerContext = capz.getOrCreateContext();
        handlerContext.executeBlocking(res -> {
            try {
                ch.close();
                res.complete(null);
            } catch (IOException e) {
                res.fail(e);
            }
        }, handler);
    }

    private synchronized void closeInternal(Handler<AsyncResult<Void>> handler) {
        check();

        closed = true;

        if (writesOutstanding == 0) {
            doClose(handler);
        } else {
            closedDeferred = () -> doClose(handler);
        }
    }

}
