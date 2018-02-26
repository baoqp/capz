package com.capz.core.http;


import com.capz.core.Handler;
import com.capz.core.buffer.Buffer;
import com.capz.core.streams.ReadStream;

/**
 * Represents an file upload from an HTML FORM.
 *
 */

public interface HttpServerFileUpload extends ReadStream<Buffer> {

    @Override
    HttpServerFileUpload exceptionHandler(Handler<Throwable> handler);

    @Override
    HttpServerFileUpload handler(Handler<Buffer> handler);

    @Override
    HttpServerFileUpload endHandler(Handler<Void> endHandler);

    @Override
    HttpServerFileUpload pause();

    @Override
    HttpServerFileUpload resume();

    /**
     * Stream the content of this upload to the given file on storage.
     *
     * @param filename the name of the file
     */
    HttpServerFileUpload streamToFileSystem(String filename);


    String filename();


    String name();


    String contentType();


    String contentTransferEncoding();


    String charset();

    /**
     * The size of the upload may not be available until it is all read.
     * Check {@link #isSizeAvailable} to determine this
     *
     * @return the size of the upload (in bytes)
     */
    long size();

    /**
     * @return true if the size of the upload can be retrieved via {@link #size()}.
     */
    boolean isSizeAvailable();
}
