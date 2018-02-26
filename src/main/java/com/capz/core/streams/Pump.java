
package com.capz.core.streams;



/**
 * Pumps data from a {@link ReadStream} to a {@link WriteStream} and performs flow control where necessary to
 * prevent the write stream buffer from getting overfull.
 * <p>
 * Instances of this class read items from a {@link ReadStream} and write them to a {@link WriteStream}. If data
 * can be read faster than it can be written this could result in the write queue of the {@link WriteStream} growing
 * without bound, eventually causing it to exhaust all available RAM.
 * <p>
 * To prevent this, after each write, instances of this class check whether the write queue of the {@link
 * WriteStream} is full, and if so, the {@link ReadStream} is paused, and a {@code drainHandler} is set on the
 * {@link WriteStream}.
 * <p>
 * When the {@link WriteStream} has processed half of its backlog, the {@code drainHandler} will be
 * called, which results in the pump resuming the {@link ReadStream}.
 * <p>
 * This class can be used to pump from any {@link ReadStream} to any {@link WriteStream},
 * e.g. from an {@link io.vertx.core.http.HttpServerRequest} to an {@link io.vertx.core.file.AsyncFile},
 * or from {@link io.vertx.core.net.NetSocket} to a {@link io.vertx.core.http.WebSocket}.
 * <p>
 * Please see the documentation for more information.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface Pump {



  /**
   * Set the write queue max size to {@code maxSize}
   *
   * @param maxSize  the max size
   * @return a reference to this, so the API can be used fluently
   */
  Pump setWriteQueueMaxSize(int maxSize);

  /**
   * Start the Pump. The Pump can be started and stopped multiple times.
   *
   * @return a reference to this, so the API can be used fluently
   */

  Pump start();

  /**
   * Stop the Pump. The Pump can be started and stopped multiple times.
   *
   * @return a reference to this, so the API can be used fluently
   */
  Pump stop();

  /**
   * Return the total number of items pumped by this pump.
   */
  int numberPumped();

}
