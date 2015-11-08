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
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link AbstractSlackerExecutor}.
 *
 * @author david
 */
@RunWith(VertxUnitRunner.class)
public class AbstractSlackerExecutorTest extends SlackerBaseTest {

  // the request used for the test execution
  private static final SlackerRequest REQUEST = new SlackerRequestBuilder()
      .timestamp(Instant.now())
      .channelId("12345")
      .channelName("#dope")
      .userId("6789")
      .userName("david")
      .teamDomain("slack.davidafsilva.pt")
      .teamIdentifier("davidafsilva")
      .trigger("!")
      .text("!test")
      .build();

  // the responses used for the test
  private static final SlackerResponse SUCCESS = SlackerResponseFactory.create(ResultCode.OK,
      Optional.of("success"));
  private static final SlackerResponse INVALID = SlackerResponseFactory.create(ResultCode.INVALID,
      Optional.of("invalid"));
  private static final SlackerResponse ERROR = SlackerResponseFactory.create(ResultCode.ERROR,
      Optional.of("error"));

  @Test
  public void test_responseCreation_success() {
    final AbstractSlackerExecutor executor = new TestSlackerExecutor(Future::complete);
    final SlackerResponse response = executor.success();
    assertNotNull(response);
    assertEquals(ResultCode.OK, response.getCode());
    assertFalse(response.getResponse().isPresent());
  }

  @Test
  public void test_responseCreation_successWithMessage() {
    final AbstractSlackerExecutor executor = new TestSlackerExecutor(Future::complete);
    final SlackerResponse response = executor.success("test");
    assertNotNull(response);
    assertEquals(ResultCode.OK, response.getCode());
    assertTrue(response.getResponse().isPresent());
    assertEquals("test", response.getResponse().get());
  }

  @Test
  public void test_responseCreation_invalid() {
    final AbstractSlackerExecutor executor = new TestSlackerExecutor(Future::complete);
    final SlackerResponse response = executor.invalid("test");
    assertNotNull(response);
    assertEquals(ResultCode.INVALID, response.getCode());
    assertTrue(response.getResponse().isPresent());
    assertEquals("test", response.getResponse().get());
  }

  @Test
  public void test_responseCreation_error() {
    final AbstractSlackerExecutor executor = new TestSlackerExecutor(Future::complete);
    final SlackerResponse response = executor.error("test");
    assertNotNull(response);
    assertEquals(ResultCode.ERROR, response.getCode());
    assertTrue(response.getResponse().isPresent());
    assertEquals("test", response.getResponse().get());
  }

  @Test
  public void test_failDeploy() throws InterruptedException {
    deployServer(r -> r.fail(0, "dummy reason"));
    assertFalse("deployed executor when wasn't expected",
        deployVerticle(new TestSlackerExecutor(Future::complete)).succeeded());
  }

  @Test
  public void test_successDeploy_failResponse() throws InterruptedException {
    deployServer(r -> r.reply(new JsonObject().put("s", true)));

    // deploy executor
    assertTrue("unable to deploy executor",
        deployVerticle(new TestSlackerExecutor(f -> f.fail("test-fail"))).succeeded());

    // test sending message with a fail response
    final AsyncResult<Message<Object>> reply = sendRequest("slacker-test", REQUEST);
    assertFalse(reply.succeeded());
    assertEquals("test-fail", reply.cause().getMessage());
  }

  @Test
  public void test_successDeploy_successResponse() throws InterruptedException {
    executeDeployWithMessage(SUCCESS);
  }

  @Test
  public void test_successDeploy_invalidResponse() throws InterruptedException {
    executeDeployWithMessage(INVALID);
  }

  @Test
  public void test_successDeploy_errorResponse() throws InterruptedException {
    executeDeployWithMessage(ERROR);
  }

  private void deployServer(final Consumer<Message<Object>> replyFunc)
      throws InterruptedException {
    assertTrue(deployVerticle(new TestSlackerServer(replyFunc)).succeeded());
  }

  private void executeDeployWithMessage(final SlackerResponse response)
      throws InterruptedException {
    deployServer(r -> r.reply(new JsonObject().put("s", true)));

    // deploy executor
    final AbstractSlackerExecutor executor = new TestSlackerExecutor(f -> f.complete(response));
    assertTrue("unable to deploy executor", deployVerticle(executor).succeeded());
    assertNotNull(executor.getVertx());

    // test sending message
    final AsyncResult<Message<Object>> reply = sendRequest("slacker-test", REQUEST);
    assertTrue(reply.succeeded());
    final Message<Object> result = reply.result();
    assertNotNull(result);
    assertNotNull(result.body());
    assertThat(result.body(), instanceOf(SlackerResponse.class));
    final SlackerResponse sr = SlackerResponse.class.cast(result.body());
    assertEquals(response.getCode(), sr.getCode());
    assertEquals(sr.getResponse().isPresent(), sr.getResponse().isPresent());
    if (sr.getResponse().isPresent()) {
      assertEquals(response.getResponse().get(), sr.getResponse().get());
    }
  }

  // the dummy test slacker executor
  private static class TestSlackerExecutor extends AbstractSlackerExecutor {

    // response function
    private final Consumer<Future<SlackerResponse>> responseFunc;

    /**
     * Default constructor
     *
     * @param responseFunc the response function
     */
    private TestSlackerExecutor(final Consumer<Future<SlackerResponse>> responseFunc) {
      this.responseFunc = responseFunc;
    }

    @Override
    public String identifier() {
      return "test";
    }

    @Override
    public String description() {
      return "some dummy description";
    }

    @Override
    public String version() {
      return "1.0.0";
    }

    @Override
    public void execute(final SlackerRequest request, final Future<SlackerResponse> result) {
      responseFunc.accept(result);
    }
  }

  // the dummy test slacker server
  private class TestSlackerServer extends AbstractVerticle {

    // the reply function
    private final Consumer<Message<Object>> replyFunc;

    // the vertx instance
    private MessageConsumer<Object> consumer;

    private TestSlackerServer(final Consumer<Message<Object>> replyFunc) {
      this.replyFunc = replyFunc;
    }

    @Override
    public void start() throws Exception {
      consumer = vertx.eventBus().consumer("slacker-server", r -> {
        final Object body = r.body();
        assertThat(body, instanceOf(JsonObject.class));
        final JsonObject json = (JsonObject) body;
        assertTrue(json.containsKey("i"));
        assertEquals("test", json.getString("i"));
        assertTrue(json.containsKey("v"));
        assertEquals("1.0.0", json.getString("v"));
        assertTrue(json.containsKey("d"));
        assertEquals("some dummy description", json.getString("d"));
        replyFunc.accept(r);
      });
    }

    @Override
    public void stop() throws Exception {
      consumer.unregister();
    }
  }
}
