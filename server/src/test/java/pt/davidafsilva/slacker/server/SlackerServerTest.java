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

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.spi.FutureFactory;
import pt.davidafsilva.slacker.api.SlackerRequestMessageCodec;
import pt.davidafsilva.slacker.api.SlackerResponseMessageCodec;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SlackerServer} object.
 *
 * @author david
 */
@RunWith(MockitoJUnitRunner.class)
public class SlackerServerTest {

  @Mock
  private Future<Void> voidFuture;

  @Mock
  private Vertx vertx;

  @Mock
  private EventBus eventBus;

  @Captor
  private ArgumentCaptor<Handler<AsyncResult<String>>> startCaptor;

  @Captor
  private ArgumentCaptor<Handler<AsyncResult<Void>>> stopCaptor;

  // the future factory
  private final FutureFactory factory = ServiceHelper.loadFactory(FutureFactory.class);

  // the server instance to be tested
  private SlackerServer server;

  @Before
  public void setup() {
    server = new SlackerServer();
    server.init(vertx, mock(Context.class));
    when(vertx.eventBus()).thenReturn(eventBus);
    when(eventBus.registerCodec(any())).thenReturn(eventBus);
  }

  @Test
  public void test_codecRegistry() throws Exception {
    server.start(voidFuture);
    verify(eventBus).registerCodec(isA(SlackerRequestMessageCodec.class));
    verify(eventBus).registerCodec(isA(SlackerResponseMessageCodec.class));
  }

  @Test
  public void test_start_eventDeployFailure() throws Exception {
    server.start(voidFuture);

    verify(vertx, times(1)).deployVerticle(isA(EventServerVerticle.class), startCaptor.capture());
    final Handler<AsyncResult<String>> handler = startCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture("dummy", true));

    verify(vertx, never()).deployVerticle(isA(HttpServerVerticle.class), startCaptor.capture());
    verify(voidFuture, never()).complete();
    verify(voidFuture, times(1)).fail("failed to deploy event verticle");
  }

  @Test
  public void test_start_httpDeployFailure() throws Exception {
    server.start(voidFuture);

    verify(vertx, times(1)).deployVerticle(isA(EventServerVerticle.class), startCaptor.capture());
    Handler<AsyncResult<String>> handler = startCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture());

    verify(vertx, times(1)).deployVerticle(isA(HttpServerVerticle.class), startCaptor.capture());
    handler = startCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture("dummy", true));

    verify(voidFuture, never()).complete();
    verify(voidFuture, times(1)).fail("failed to deploy http verticle");
  }

  @Test
  public void test_start_success() throws Exception {
    server.start(voidFuture);

    verify(vertx, times(1)).deployVerticle(isA(EventServerVerticle.class), startCaptor.capture());
    Handler<AsyncResult<String>> handler = startCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture());

    verify(vertx, times(1)).deployVerticle(isA(HttpServerVerticle.class), startCaptor.capture());
    handler = startCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture());

    verify(voidFuture, times(1)).complete();
    verify(voidFuture, never()).fail(anyString());
    verify(voidFuture, never()).fail(any(Throwable.class));
  }

  @Test
  public void test_stop_success() throws Exception {
    server.stop(voidFuture);

    verify(vertx, times(1)).undeploy(any(), stopCaptor.capture());
    Handler<AsyncResult<Void>> handler = stopCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture());

    verify(vertx, times(2)).undeploy(any(), stopCaptor.capture());
    handler = stopCaptor.getValue();
    assertNotNull(handler);
    handler.handle(factory.completedFuture());

    verify(voidFuture, times(1)).complete();
    verify(voidFuture, never()).fail(anyString());
    verify(voidFuture, never()).fail(any(Throwable.class));
  }
}
