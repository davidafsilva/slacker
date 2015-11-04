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

import org.junit.Test;

import java.time.Instant;

import io.vertx.core.buffer.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link SlackerRequestMessageCodec}.
 *
 * @author david
 */
public class SlackerRequestMessageCodecTest {

  @Test
  public void test_staticProperties() {
    final SlackerRequestMessageCodec codec = new SlackerRequestMessageCodec();
    assertEquals(SlackerRequestMessageCodec.NAME, codec.name());
    assertEquals(-1, codec.systemCodecID());
  }


  @Test
  public void test_transform() {
    final SlackerRequestMessageCodec codec = new SlackerRequestMessageCodec();
    final SlackerRequest request = mock(SlackerRequest.class);
    assertEquals(request, codec.transform(request));
  }

  @Test
  public void test_encodeDecode() {
    final SlackerRequestMessageCodec codec = new SlackerRequestMessageCodec();
    final Instant now = Instant.now();
    final SlackerRequest request = new SlackerRequestBuilder()
        .timestamp(now)
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .trigger("!")
        .text("!test")
        .build();

    // encode
    final Buffer buffer = Buffer.buffer();
    codec.encodeToWire(buffer, request);
    assertEquals((8 + 4) // timestamp
            + 4 + 5 // channel id
            + 4 + 5 // channel name
            + 4 + 4 // user id
            + 4 + 5 // user name
            + 4 + 21 // team domain
            + 4 + 12 // team id
            + 4 + 1 // trigger
            + 4 + 5 // text
        , buffer.length());

    // decode
    final SlackerRequest decoded = codec.decodeFromWire(0, buffer.copy());
    assertNotNull(decoded);
    assertEquals(now, decoded.getTimestamp());
    assertEquals(request.getChannelId(), decoded.getChannelId());
    assertEquals(request.getChannelName(), decoded.getChannelName());
    assertEquals(request.getUserId(), decoded.getUserId());
    assertEquals(request.getUserName(), decoded.getUserName());
    assertEquals(request.getTeamDomain(), decoded.getTeamDomain());
    assertEquals(request.getTeamIdentifier(), decoded.getTeamIdentifier());
    assertEquals(request.getTrigger(), decoded.getTrigger());
    assertEquals(request.getText(), decoded.getText());
  }
}
