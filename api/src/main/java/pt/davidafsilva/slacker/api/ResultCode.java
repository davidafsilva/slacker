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

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * The result code for the slacker commands
 *
 * @author david
 * @since 1.0
 */
public enum ResultCode {
  OK(HttpResponseStatus.OK),
  INVALID(HttpResponseStatus.BAD_REQUEST),
  ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR);

  // the underlying HTTP status
  private final HttpResponseStatus httpStatus;

  /**
   * Default result code constructor with the underlying HTTP status mapping
   *
   * @param httpStatus the HTTP status mapping for this result code
   */
  ResultCode(final HttpResponseStatus httpStatus) {
    this.httpStatus = httpStatus;
  }

  /**
   * Returns the underlying HTTP status for this result code
   *
   * @return the HTTP status
   */
  public HttpResponseStatus getHttpStatus() {
    return httpStatus;
  }
}
