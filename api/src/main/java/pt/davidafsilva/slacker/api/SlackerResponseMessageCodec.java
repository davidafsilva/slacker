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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * The code for the the {@link SlackerRequest} message.
 * This is required to sent this message types across the vertx event-bus.
 *
 * @author david
 * @since 1.0
 */
public final class SlackerResponseMessageCodec
    implements MessageCodec<SlackerResponse, SlackerResponse> {

  // the code name
  public static final String NAME = "slacker-rsp";

  @Override
  public void encodeToWire(final Buffer buffer, final SlackerResponse response) {
    // response code
    buffer.appendInt(response.getCode().ordinal());
    // response text
    buffer.appendInt(response.getResponse().filter(Objects::nonNull).map(String::length).orElse(0));
    response.getResponse().filter(Objects::nonNull).ifPresent(buffer::appendString);
  }

  @Override
  public SlackerResponse decodeFromWire(final int pos, final Buffer buffer) {
    int offset = pos;
    // result code
    final ResultCode code = ResultCode.values()[buffer.getInt(offset)];
    offset += 4;
    // response body
    final int length = buffer.getInt(offset);
    offset += 4;
    final Optional<String> response;
    if (length > 0) {
      response = Optional.ofNullable(buffer.getString(offset, offset + length));
    } else {
      response = Optional.empty();
    }

    return SlackerResponseFactory.create(code, response);
  }

  @Override
  public SlackerResponse transform(final SlackerResponse response) {
    return response;
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
