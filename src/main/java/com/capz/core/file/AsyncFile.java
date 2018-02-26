package com.capz.core.file;


import com.capz.core.AsyncResult;
import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.streams.ReadStream;
import com.capz.core.streams.WriteStream;

/**
 * Represents a file on the file-system which can be read from, or written to asynchronously.
 */
public interface AsyncFile extends ReadStream<Buffer>, WriteStream<Buffer> {

    @Override
    AsyncFile handler(Handler<Buffer> handler);

    @Override
    AsyncFile pause();

    @Override
    AsyncFile resume();

    @Override
    AsyncFile endHandler(Handler<Void> endHandler);

    @Override
    AsyncFile write(Buffer data);

    @Override
    AsyncFile setWriteQueueMaxSize(int maxSize);

    @Override
    AsyncFile drainHandler(Handler<Void> handler);

    @Override
    AsyncFile exceptionHandler(Handler<Throwable> handler);


    @Override
    void end();

    //Close the file. The actual close happens asynchronously.
    void close();


    void close(Handler<AsyncResult<Void>> handler);

    /**
     * Write a Buffer to the file at the position in the file, asynchronously.
     * If  position lies outside of the current size
     * of the file, the file will be enlarged to encompass it.
     * When multiple writes are invoked on the same file
     * there are no guarantees as to order in which those writes actually occur
     * The handler will be called when the write is complete, or if an error occurs.
     */
    AsyncFile write(Buffer buffer, long position, Handler<AsyncResult<Void>> handler);

    /**
     * Reads {@code length} bytes of data from the file at position {@code position} in the file, asynchronously.
     * The read data will be written into the specified {@code Buffer buffer} at position {@code offset}.
     * If data is read past the end of the file then zero bytes will be read.<p>
     * When multiple reads are invoked on the same file there are no guarantees as to order in which those reads actually occur.
     * The handler will be called when the close is complete, or if an error occurs.
     */

    AsyncFile read(Buffer buffer, int offset, long position, int length, Handler<AsyncResult<Buffer>> handler);


    AsyncFile flush();


    AsyncFile flush(Handler<AsyncResult<Void>> handler);


    AsyncFile setReadPos(long readPos);


    AsyncFile setWritePos(long writePos);

    AsyncFile setReadBufferSize(int readBufferSize);
}
