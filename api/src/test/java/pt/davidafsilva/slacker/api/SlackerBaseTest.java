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

import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test class provides an abstraction for the execution of unitary tests over vert.x and
 * slacker executors, such as starting a vertx container/contexts environment and deployment of
 * verticles/slacker executors.
 *
 * @author david
 */
public class SlackerBaseTest {

  // the test instance of vertx
  protected Vertx vertx;

  @Before
  public void setup() throws Exception {
    vertx = Vertx.vertx(getOptions());
  }

  @After
  public void clean() throws Exception {
    final AsyncResult<Void> result = execSync(vertx::close);
    assertTrue("unable to close vertx instance", result.succeeded());
  }

  /**
   * Returns the options to be used when creating the vertx instance
   *
   * @return the vertx options
   */
  protected VertxOptions getOptions() {
    return new VertxOptions();
  }

  /**
   * Sends the specified request in name of the slacker server in a synchronous fashion.
   *
   * @param executor the target executor (slacker identifier)
   * @param request  the request to be sent
   * @return the reply of the server, if any, otherwise the test will fail due to timeout
   */
  protected AsyncResult<Message<Object>> sendRequest(final String executor,
      final SlackerRequest request) {
    final QuadConsumer<String, Object, DeliveryOptions, Handler<AsyncResult<Message<Object>>>> f =
        (a, b, c, d) -> vertx.eventBus().send(a, b, c, d);
    return execSync(f, executor, request, new DeliveryOptions()
        .setCodecName(SlackerRequestMessageCodec.NAME));
  }

  /**
   * Deploys the given verticle with the default deployment options in a synchronous fashion.
   *
   * @param verticle the verticle to be deployed
   * @return the result of the deployment
   * @see #deployVerticle(Verticle, DeploymentOptions)
   */
  protected AsyncResult<String> deployVerticle(final Verticle verticle) {
    return deployVerticle(verticle, new DeploymentOptions());
  }

  /**
   * Deploys the given verticle with given deployment options in a synchronous fashion.
   *
   * @param verticle the verticle to be deployed
   * @param options  the options for the deployment of the verticle
   * @return the result of the deployment
   * @see #deployVerticle(Verticle)
   */
  protected AsyncResult<String> deployVerticle(final Verticle verticle,
      final DeploymentOptions options) {
    return execSync(vertx::deployVerticle, verticle, options);
  }

  /**
   * Executes a void asynchronous procedure with no arguments in a synchronous fashion.
   *
   * @param consumer the async procedure to be executed
   * @param <R>      the type of the async result
   * @return the async result of the procedure
   */
  protected <R> R execSync(final Consumer<Handler<R>> consumer) {
    return execSync((a, b, c, d) -> consumer.accept(d), null, null, null);
  }

  /**
   * Executes a void asynchronous procedure with exactly one arguments in a synchronous fashion.
   *
   * @param consumer the async procedure to be executed
   * @param t        the first procedure argument
   * @param <T>      the type of the first procedure argument
   * @param <R>      the type of the async result
   * @return the async result of the procedure
   */
  protected <T, R> R execSync(final BiConsumer<T, Handler<R>> consumer, final T t) {
    return execSync((a, b, c, d) -> consumer.accept(a, d), t, null, null);
  }

  /**
   * Executes a void asynchronous procedure with exactly two arguments in a synchronous fashion.
   *
   * @param consumer the async procedure to be executed
   * @param t        the first procedure argument
   * @param u        the second procedure argument
   * @param <T>      the type of the first procedure argument
   * @param <U>      the type of the second procedure argument
   * @param <R>      the type of the async result
   * @return the async result of the procedure
   */
  protected <T, U, R> R execSync(final TriConsumer<T, U, Handler<R>> consumer, final T t,
      final U u) {
    return execSync((a, b, c, d) -> consumer.accept(a, b, d), t, u, null);
  }

  /**
   * Executes a void asynchronous procedure with exactly three arguments in a synchronous fashion.
   *
   * @param consumer the async procedure to be executed
   * @param t        the first procedure argument
   * @param u        the second procedure argument
   * @param v        the third procedure argument
   * @param <T>      the type of the first procedure argument
   * @param <U>      the type of the second procedure argument
   * @param <V>      the type of the third procedure argument
   * @param <R>      the type of the async result
   * @return the async result of the procedure
   */
  protected <T, U, V, R> R execSync(final QuadConsumer<T, U, V, Handler<R>> consumer,
      final T t, final U u, final V v) {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<R> reference = new AtomicReference<>();
    consumer.accept(t, u, v, r -> {
      reference.set(r);
      latch.countDown();
    });
    awaitLatch(latch);
    return reference.get();
  }

  /**
   * Awaits at the most 30 seconds for the latch to reach 0.
   *
   * @param latch the latch to wait on.
   */
  protected void awaitLatch(final CountDownLatch latch) {
    try {
      latch.await(30, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      fail("thread got interrupted: " + e.getMessage());
    }
  }

  // utility functional interfaces - just for the test API

  @FunctionalInterface
  private interface TriConsumer<T, U, V> {

    void accept(T t, U u, V v);
  }

  @FunctionalInterface
  private interface QuadConsumer<T, U, V, X> {

    void accept(T t, U u, V v, X x);
  }
}
