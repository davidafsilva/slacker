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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link SlackerResponseFactory}.
 *
 * @author david
 */
public class SlackerResponseFactoryTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_constructor() throws Exception {
    thrown.expect(InvocationTargetException.class);
    Constructor<SlackerResponseFactory> c = SlackerResponseFactory.class.getDeclaredConstructor();
    c.setAccessible(true);
    c.newInstance();
  }

  @Test
  public void test_creation_nullCode() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("code");
    SlackerResponseFactory.create(null, Optional.empty());
  }

  @Test
  public void test_creation_nullMessage() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("message");
    SlackerResponseFactory.create(ResultCode.OK, null);
  }

  @Test
  public void test_creation() {
    final SlackerResponse response = SlackerResponseFactory.create(ResultCode.OK,
        Optional.of("test"));
    assertNotNull(response);
    assertEquals(ResultCode.OK, response.getCode());
    assertTrue(response.getResponse().isPresent());
    assertEquals("test", response.getResponse().get());
  }
}
