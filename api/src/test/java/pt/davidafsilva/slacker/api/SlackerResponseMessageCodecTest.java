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

import java.util.Optional;

import io.vertx.core.buffer.Buffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link SlackerResponseMessageCodec}.
 *
 * @author david
 */
public class SlackerResponseMessageCodecTest {

  @Test
  public void test_staticProperties() {
    final SlackerResponseMessageCodec codec = new SlackerResponseMessageCodec();
    assertEquals(SlackerResponseMessageCodec.NAME, codec.name());
    assertEquals(-1, codec.systemCodecID());
  }

  @Test
  public void test_transform() {
    final SlackerResponseMessageCodec codec = new SlackerResponseMessageCodec();
    final SlackerResponse response = mock(SlackerResponse.class);
    assertEquals(response, codec.transform(response));
  }

  @Test
  public void test_encodeDecode_withoutText() {
    final SlackerResponseMessageCodec codec = new SlackerResponseMessageCodec();
    final SlackerResponse response = SlackerResponseFactory.create(ResultCode.OK,
        Optional.empty());

    // encode
    final Buffer buffer = Buffer.buffer();
    codec.encodeToWire(buffer, response);
    // 2x integers
    assertEquals(4 + 4, buffer.length());

    // decode
    final SlackerResponse decoded = codec.decodeFromWire(0, buffer.copy());
    assertNotNull(decoded);
    assertEquals(response.getCode(), decoded.getCode());
    assertFalse(decoded.getResponse().isPresent());
  }

  @Test
  public void test_encodeDecode_withText() {
    final SlackerResponseMessageCodec codec = new SlackerResponseMessageCodec();
    final SlackerResponse response = SlackerResponseFactory.create(ResultCode.OK,
        Optional.of("test"));

    // encode
    final Buffer buffer = Buffer.buffer();
    codec.encodeToWire(buffer, response);
    // 2x integers + "test"
    assertEquals(4 + 4 + 4, buffer.length());

    // decode
    final SlackerResponse decoded = codec.decodeFromWire(0, buffer.copy());
    assertNotNull(decoded);
    assertEquals(response.getCode(), decoded.getCode());
    assertTrue(decoded.getResponse().isPresent());
    assertEquals(response.getResponse().get(), decoded.getResponse().get());
  }
}
