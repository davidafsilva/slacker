package pt.davidafsilva.slacker.api;

/*
 * #%L
 * slacker-api
 * %%
 * Copyright (C) 2015 David Silva
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.Verticle;

/**
 * <p>
 * This interface defines the contract for the slacker executors as they're all bound to have
 * a specific set of properties, such as an {@link #identifier()} and help {@link #description()}.
 * </p>
 * <p>
 * The executors {@link #execute(SlackerRequest, Future)} method will be called by the
 * same thread that received the request in a asynchronous fashion, which means the execution
 * result will be supplied asynchronously via the {@link AsyncResultHandler}, avoiding blocking the
 * listener thread.
 * </p>
 * <p>
 * This interface extends the {@link Verticle} as all executors will be bound to a verticle
 * lifecycle as well, but with a little caveat. Executors require a certain protocol in order for
 * them to be registered within the slacker-server and be able to receive incoming requests via
 * the event-bus.
 * The protocol is described as follows, step by step:
 * </p>
 * <pre>
 *   Slacker Executor = SE
 *     Slacker Server = SS
 *
 * SE ----&gt; [ HELLO REQ ] ----&gt; SS
 *      {
 *        "i": &lt;identifier&gt;,
 *        "d": &lt;description&gt;,
 *        "v": &lt;version&gt;
 *      }
 * SE &lt;---- [ HELLO RSP } &lt;---- SS
 *      {
 *        "s": &lt;true|false&gt;,
 *        "m": &lt;message&gt;
 *      }
 * </pre>
 * <p>
 * All HELLO REQ message fields are straightforward enough. The identifier is used to identify the
 * executor as well as the channel command. The description is used to display at the help message.
 * Finally, the version is used to ensure that only instances with the same or newer (in-service
 * upgrade) version are enabled.
 *
 * The HELLO RSP message contains the response status along with a response message, e.g. used to
 * include the failure reason.
 * </p>
 *
 * @author david
 * @since 1.0
 */
public interface SlackerExecutor extends Verticle {

  /**
   * Returns the identifier of the command that this executor will be listening to.
   *
   * Note that this identifier will be used as the channel command, prefixed with the slacker
   * executor character. It advised to use short and concise names so users can memorize them
   * easily.
   *
   * This identifier must be unique, such that different implementations of an executor with
   * the same id will collide, and only one will be made available.
   *
   * @return the slacker executor identifier
   */
  String identifier();

  /**
   * Returns a brief description of the slacker executor that this implementation is supporting.
   * This description should be at maximum 32 characters long.
   *
   * @return the slacker executor description
   */
  String description();

  /**
   * Returns a version of the slacker executor that this implementation is supporting.
   *
   * @return the slacker executor version
   */
  String version();

  /**
   * Invoke and handles the incoming request, posting the execution result asynchronously to the
   * specified future.
   *
   * @param request the incoming slacker request
   * @param result  the outgoing result with the actual execution result
   */
  void execute(final SlackerRequest request, final Future<SlackerResponse> result);
}
