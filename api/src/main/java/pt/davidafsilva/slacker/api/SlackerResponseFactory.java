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

import java.util.Objects;
import java.util.Optional;

/**
 * A simple factory for the creation of {@link SlackerResponse} messages.
 *
 * @author david
 * @since 1.0
 */
final class SlackerResponseFactory {

  // private constructor
  private SlackerResponseFactory() {
    throw new UnsupportedOperationException("no no no");
  }

  /**
   * Creates a slacker response with the given response code and optionally the response message.
   *
   * @param code    the result code
   * @param message the result message
   * @return a new instance of {@link SlackerResponse} with the specified parameters
   * @throws NullPointerException if any of the arguments is {@code null}
   */
  static SlackerResponse create(final ResultCode code, final Optional<String> message) {
    return new SlackerResponseImpl(Objects.requireNonNull(code, "code"),
        Objects.requireNonNull(message, "message"));
  }

  // straight-forward implementation of the slacker response
  static final class SlackerResponseImpl implements SlackerResponse {

    // the result code
    private final ResultCode code;
    // the message
    private final Optional<String> message;

    /**
     * Default constructor
     *
     * @param code    the result code
     * @param message the result message
     */
    private SlackerResponseImpl(final ResultCode code, final Optional<String> message) {
      this.code = code;
      this.message = message;
    }

    @Override
    public ResultCode getCode() {
      return code;
    }

    @Override
    public Optional<String> getResponse() {
      return message;
    }
  }
}
