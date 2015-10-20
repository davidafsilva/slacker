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

/**
 * This abstract implementation serves as a baseline for slacker command executors.
 * It includes the communication protocol for registering the executor for the supported command
 * as well as utility methods for creating the sending the execution results.
 *
 * @author david
 * @since 1.0
 */
public interface SlackerExecutor {

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
   * Invoke and handles the incoming request, posting the execution result asynchronously to the
   * specified result handler.
   *
   * @param request the incoming slacker request
   * @param result  the outgoing result handler with the actual execution result
   */
  void execute(final SlackerRequest request, final AsyncResultHandler<SlackerResponse> result);
}
