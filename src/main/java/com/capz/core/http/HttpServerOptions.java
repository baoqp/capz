
package com.capz.core.http;


import com.capz.core.net.NetServerOptions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode
public class HttpServerOptions extends NetServerOptions {


    public static final int DEFAULT_PORT = 80;

    public static final boolean DEFAULT_COMPRESSION_SUPPORTED = false;

    public static final int DEFAULT_COMPRESSION_LEVEL = 6;


    public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;

    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4096;

    public static final int DEFAULT_MAX_HEADER_SIZE = 8192;

    public static final boolean DEFAULT_HANDLE_100_CONTINE_AUTOMATICALLY = false;

    public static final long DEFAULT_INITIAL_SETTINGS_MAX_CONCURRENT_STREAMS = 100;


    public static final boolean DEFAULT_DECOMPRESSION_SUPPORTED = false;

    public static final boolean DEFAULT_ACCEPT_UNMASKED_FRAMES = false;

    public static final int DEFAULT_DECODER_INITIAL_BUFFER_SIZE = 128;

    private boolean compressionSupported;
    private int compressionLevel;
    private boolean handle100ContinueAutomatically;
    private int maxChunkSize;
    private int maxInitialLineLength;
    private int maxHeaderSize;
    private boolean decompressionSupported;
    private boolean acceptUnmaskedFrames;
    private int decoderInitialBufferSize;


    public HttpServerOptions() {
        super();
        init();
        setPort(DEFAULT_PORT); // We override the default for port
    }


    public HttpServerOptions(HttpServerOptions other) {
        super(other);
        this.compressionSupported = other.isCompressionSupported();
        this.compressionLevel = other.getCompressionLevel();
        this.handle100ContinueAutomatically = other.handle100ContinueAutomatically;
        this.maxChunkSize = other.getMaxChunkSize();
        this.maxInitialLineLength = other.getMaxInitialLineLength();
        this.maxHeaderSize = other.getMaxHeaderSize();
        this.decompressionSupported = other.isDecompressionSupported();
        this.acceptUnmaskedFrames = other.isAcceptUnmaskedFrames();
        this.decoderInitialBufferSize = other.getDecoderInitialBufferSize();
    }


    private void init() {
        compressionSupported = DEFAULT_COMPRESSION_SUPPORTED;
        compressionLevel = DEFAULT_COMPRESSION_LEVEL;

        handle100ContinueAutomatically = DEFAULT_HANDLE_100_CONTINE_AUTOMATICALLY;
        maxChunkSize = DEFAULT_MAX_CHUNK_SIZE;
        maxInitialLineLength = DEFAULT_MAX_INITIAL_LINE_LENGTH;
        maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;

        decompressionSupported = DEFAULT_DECOMPRESSION_SUPPORTED;
        acceptUnmaskedFrames = DEFAULT_ACCEPT_UNMASKED_FRAMES;
        decoderInitialBufferSize = DEFAULT_DECODER_INITIAL_BUFFER_SIZE;
    }

}
