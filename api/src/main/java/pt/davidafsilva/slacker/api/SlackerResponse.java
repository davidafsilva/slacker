package pt.davidafsilva.slacker.api;

/*
 * #%L
 * slack-hello-back
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

import java.util.Optional;

/**
 * The response of the (remote) slacker command execution.
 *
 * @author david
 * @since 1.0
 */
public interface SlackerResponse {

  /**
   * Returns the result code of the remote execution.
   * The result codes are bound to:
   * <ul>
   * <li>{@link ResultCode#OK}: everything went smoothly</li>
   * <li>{@link ResultCode#INVALID}: invalid arguments/input data</li>
   * <li>{@link ResultCode#ERROR}: unexpected error happened</li>
   * </ul>
   *
   * @return the result code
   */
  ResultCode getCode();

  /**
   * Returns the response body of the execution, if any, as it should be empty when no
   * response is intended to be returned.
   *
   * This response may also contain error reasons when the result code is not
   * {@link ResultCode#OK}.
   *
   * @return the failure reason of the execution
   */
  Optional<String> getResponse();
}
