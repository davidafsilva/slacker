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

import java.time.Instant;
import java.util.function.Consumer;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * The code for the the {@link SlackerRequest} message.
 * This is required to sent this message types across the vertx event-bus.
 *
 * @author david
 */
public final class SlackerRequestMessageCodec
    implements MessageCodec<SlackerRequest, SlackerRequest> {

  // the code name
  public static final String NAME = "slacker-req";

  @Override
  public void encodeToWire(final Buffer buffer, final SlackerRequest request) {
    // timestamp
    buffer.appendLong(request.getTimestamp().getEpochSecond())
        .appendInt(request.getTimestamp().getNano());
    // team identifier
    writeString(buffer, request.getTeamIdentifier());
    // team domain
    writeString(buffer, request.getTeamDomain());
    // channel id
    writeString(buffer, request.getChannelId());
    // channel name
    writeString(buffer, request.getChannelName());
    // user id
    writeString(buffer, request.getUserId());
    // user name
    writeString(buffer, request.getUserName());
    // command
    writeString(buffer, request.getCommand());
    // arguments
    writeString(buffer, request.getArguments().orElse(""));
  }

  /**
   * Writes the (length, text) tuple to the buffer for the given string
   *
   * @param buffer the buffer where the string is going to be written
   * @param str    the string to be written
   */
  private void writeString(final Buffer buffer, final String str) {
    buffer.appendInt(str.length()).appendString(str);
  }

  @Override
  public SlackerRequest decodeFromWire(final int pos, final Buffer buffer) {
    final SlackerRequestBuilder builder = new SlackerRequestBuilder();
    // timestamp
    builder.timestamp(Instant.ofEpochSecond(buffer.getLong(pos), buffer.getInt(pos + 8)));
    int offset = pos + 12;
    // team identifier
    offset = readString(buffer, offset, builder::teamIdentifier);
    // team domain
    offset = readString(buffer, offset, builder::teamDomain);
    // channel id
    offset = readString(buffer, offset, builder::channelId);
    // channel name
    offset = readString(buffer, offset, builder::channelName);
    // user id
    offset = readString(buffer, offset, builder::userId);
    // user name
    offset = readString(buffer, offset, builder::userName);
    // trigger
    offset = readString(buffer, offset, builder::command);
    // text message
    readString(buffer, offset, builder::args);

    return builder.build();
  }

  /**
   * Reads a string previously written by this codec from the given buffer
   *
   * @param buffer   the buffer where to read the string from
   * @param offset   the offset from which starts the string
   * @param consumer the consumer for the read value
   * @return the new position offset for the next read
   */
  private int readString(final Buffer buffer, int offset, final Consumer<String> consumer) {
    final int length = buffer.getInt(offset);
    final int end = offset + 4 + length;
    final String str = length == 0 ? "" : buffer.getString(offset + 4, end);
    consumer.accept(str);
    return end;
  }

  @Override
  public SlackerRequest transform(final SlackerRequest request) {
    return request;
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
