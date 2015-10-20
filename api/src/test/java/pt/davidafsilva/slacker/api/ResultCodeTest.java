package pt.davidafsilva.slacker.api;

/*
 * #%L
 * slacker-server
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

import java.util.Arrays;
import java.util.Objects;

import io.netty.handler.codec.http.HttpResponseStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Simple unitary tests for the {@link ResultCode} HTTP status code mapping
 *
 * @author david
 */
public class ResultCodeTest {

  @Test
  public void test_nonNullMapping() {
    assertTrue(Arrays.stream(ResultCode.values()) // "shut-up" missing coverage for values()
        .map(ResultCode::name) // "shut-up" missing coverage for valueOf()
        .map(ResultCode::valueOf)
        .map(ResultCode::getHttpStatus)
        .noneMatch(Objects::isNull));
  }

  @Test
  public void test_httpCodeMapping() {
    assertEquals(ResultCode.OK.getHttpStatus(), HttpResponseStatus.OK);
    assertEquals(ResultCode.INVALID.getHttpStatus(), HttpResponseStatus.BAD_REQUEST);
    assertEquals(ResultCode.ERROR.getHttpStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
  }
}
