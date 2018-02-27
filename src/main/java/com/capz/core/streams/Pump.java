
package com.capz.core.streams;


/**
 * Pumps data from a ReadStream to a WriteStream and performs flow control where necessary to
 * prevent the write stream buffer from getting overfull.
 * <p>
 * Instances of this class read items from a ReadStream and write them to a WriteStream. If data
 * can be read faster than it can be written this could result in the write queue of the WriteStream
 * growing without bound, eventually causing it to exhaust all available RAM.
 * <p>
 * To prevent this, after each write, instances of this class check whether the write queue of the
 * WriteStream is full, and if so, the ReadStream is paused, and a drainHandler is set on the WriteStream.
 * <p>
 * When the WriteStream has processed half of its backlog, the  drainHandler will be called,
 * which results in the pump resuming the ReadStream .
 * <p>
 * This class can be used to pump from any ReadStream to any WriteStream, e.g. from an HttpServerRequest
 * to an AsyncFile
 */
public interface Pump {

    /**
     * Set the write queue max size to maxSize
     */
    Pump setWriteQueueMaxSize(int maxSize);


    /**
     * Start the Pump. The Pump can be started and stopped multiple times.
     */
    Pump start();


    Pump stop();

    /**
     * Return the total number of items pumped by this pump.
     */
    int numberPumped();


}
