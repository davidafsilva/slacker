package pt.davidafsilva.slacker.server;

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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link ExecutorRegistry} object.
 *
 * @author david
 */
@RunWith(MockitoJUnitRunner.class)
public class ExecutorRegistryTest {

  @Mock
  private Handler<String> successHandler;

  @Mock
  private Handler<String> registerErrorHandler;

  @Mock
  private Handler<Void> lookupErrorHandler;

  @Captor
  private ArgumentCaptor<String> addressCaptor;

  // the test executor registry
  private ExecutorRegistry executorRegistry;

  @Before
  public void setup() {
    executorRegistry = new ExecutorRegistry();
  }

  @Test
  public void register_invalidRequestId() {
    executorRegistry.register(new JsonObject().put("v", "1.0.0"), successHandler,
        registerErrorHandler);
    executorRegistry.register(new JsonObject().put("i", "").put("v", "1.0.0"), successHandler,
        registerErrorHandler);
    verify(successHandler, never()).handle(any());
    verify(registerErrorHandler, times(2)).handle(ExecutorRegistry.INVALID_REQUEST_MSG);
  }

  @Test
  public void register_invalidRequestVersion() {
    executorRegistry.register(new JsonObject().put("i", "xpto"), successHandler,
        registerErrorHandler);
    executorRegistry.register(new JsonObject().put("i", "xpto").put("v", ""), successHandler,
        registerErrorHandler);
    verify(successHandler, never()).handle(any());
    verify(registerErrorHandler, times(2)).handle(ExecutorRegistry.INVALID_REQUEST_MSG);
  }

  @Test
  public void register_invalidRequestVersionFormat() {
    executorRegistry.register(new JsonObject().put("i", "xpto").put("v", "1.meh"), successHandler,
        registerErrorHandler);
    verify(successHandler, never()).handle(any());
    verify(registerErrorHandler, times(1)).handle(ExecutorRegistry.INVALID_REQUEST_MSG);
  }

  @Test
  public void test_register_success_noDescription() {
    register_success(new JsonObject().put("i", "xpto").put("v", "1.0.0"));
  }

  @Test
  public void test_register_success_withDescription() {
    register_success(new JsonObject().put("i", "xpto").put("d", "woop!").put("v", "1.0.0"));
  }

  @Test
  public void test_register_failure_incompatibleVersion() {
    register_success(new JsonObject().put("i", "xpto").put("v", "1.0.0"));

    // fail with incompatible version
    executorRegistry.register(new JsonObject().put("i", "xpto").put("v", "0.0.9"), successHandler,
        registerErrorHandler);
    verify(successHandler, times(3)).handle(any());
    verify(registerErrorHandler, times(1))
        .handle("incompatible version, found 0.0.9, expected 1.0.0 or greater");
  }

  @Test
  public void test_register_success_compatibleVersion() {
    register_success(new JsonObject().put("i", "xpto").put("v", "1.0.0"));

    // fail with incompatible version
    executorRegistry.register(new JsonObject().put("i", "xpto").put("v", "1.1.0"), successHandler,
        registerErrorHandler);
    verify(successHandler, times(4)).handle(any());
    verify(registerErrorHandler, never()).handle(anyString());
  }

  private void register_success(final JsonObject request) {
    // register
    executorRegistry.register(request, successHandler, registerErrorHandler);
    verify(registerErrorHandler, never()).handle(any());
    verify(successHandler, times(1)).handle(addressCaptor.capture());
    final String address = addressCaptor.getValue();
    assertNotNull(address);
    assertTrue(address.endsWith(".slacker-executor"));
    assertEquals(32 + 17, address.length());

    // try to register it again
    executorRegistry.register(request, successHandler, registerErrorHandler);
    verify(registerErrorHandler, never()).handle(any());
    verify(successHandler, times(2)).handle(address);

    // lookup
    executorRegistry.lookup("xpto", successHandler, lookupErrorHandler);
    verify(successHandler, times(3)).handle(address);
    verify(lookupErrorHandler, never()).handle(any());

    // executors
    final List<ExecutorRegistry.ExecutorEntry> executors = executorRegistry.executors()
        .collect(Collectors.toList());
    assertEquals(1, executors.size());
    assertEquals(request.getString("i"), executors.get(0).getId());
    assertEquals(request.getString("d", ExecutorRegistry.DEFAULT_DESCRIPTION),
        executors.get(0).getDescription());
    assertEquals(request.getString("v"), executors.get(0).getVersion().toString());
  }

  @Test
  public void lookup_invalidId() {
    executorRegistry.lookup("xpto", successHandler, lookupErrorHandler);
    verify(successHandler, never()).handle(any());
    verify(lookupErrorHandler, times(1)).handle(any());
  }
}
