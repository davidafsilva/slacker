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

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;
import java.util.function.Consumer;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import pt.davidafsilva.slacker.api.ResultCode;
import pt.davidafsilva.slacker.api.SlackerBaseTest;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerResponse;
import pt.davidafsilva.slacker.api.SlackerResponseMessageCodec;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link HttpServerVerticle} object.
 *
 * @author david
 */
@RunWith(VertxUnitRunner.class)
public class HttpServerVerticleTest extends SlackerBaseTest {

  // the server port
  private static final int SERVER_PORT = 10001;

  // the post data for the tests
  private static final String POST_DATA = "timestamp=1355517523.000005&" +
      "team_id=xpto&" +
      "team_domain=slack.davidafsilva.pt&" +
      "channel_id=C12345&" +
      "channel_name=xpto&" +
      "user_id=U6789&" +
      "user_name=david&" +
      "trigger_word=!&" +
      "text=blabla";

  @Override
  public void setup() throws Exception {
    super.setup();
    assertTrue("unable to deploy server", deployVerticle(new HttpServerVerticle(),
        new DeploymentOptions().setConfig(new JsonObject()
            .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), SERVER_PORT)))
        .succeeded());
  }

  @Test
  public void test_failDeployment() {
    final AsyncResult<String> result = deployVerticle(new HttpServerVerticle(),
        new DeploymentOptions().setConfig(new JsonObject().put("HTTP_PORT", 1)));
    assertFalse("deployed server when wasn't expected", result.succeeded());
  }

  @Test
  public void test_invalidUri() {
    // test invalid URI
    wrapExec(latch -> basicPost("/wrong", res -> {
      assertEquals(404, res.statusCode());
      latch.countDown();
    }).end("{}"));
  }

  @Test
  public void test_invalidContentType() {
    // test invalid headers content-type
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(404, res.statusCode());
      latch.countDown();
    }).putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end("{}"));
  }

  @Test
  public void test_invalidRequestData() {
    // test valid URI and headers - invalid request
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(400, res.statusCode());
      validateResponseHeaders(res);
      latch.countDown();
    }).end("{}"));
  }

  @Test
  public void test_failReply() {
    assertTrue(deployVerticle(new DummyEventListener(m -> m.fail(1, "oops"))).succeeded());
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(500, res.statusCode());
      validateResponseHeaders(res);
      res.bodyHandler(b -> {
        assertEquals("{\"text\":\"<@U6789|david>: something funky happened..\"}", b.toString());
        latch.countDown();
      });
    }).end(POST_DATA));
  }

  @Test
  public void test_nullResponse() {
    testWithInvalidResponse(null);
  }

  @Test
  public void test_invalidResponseType() {
    testWithInvalidResponse("xpto");
  }

  private void testWithInvalidResponse(final String reply) {
    assertTrue(deployVerticle(new DummyEventListener(m -> m.reply(reply))).succeeded());
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(500, res.statusCode());
      validateResponseHeaders(res);
      res.bodyHandler(b -> {
        assertEquals(0, b.length());
        latch.countDown();
      });
    }).end(POST_DATA));
  }

  @Test
  public void test_responseWithoutText() {
    final SlackerResponse response = new SlackerResponse() {
      @Override
      public ResultCode getCode() {
        return ResultCode.OK;
      }

      @Override
      public Optional<String> getResponse() {
        return Optional.empty();
      }
    };
    assertTrue(deployVerticle(new DummyEventListener(m -> m.reply(response,
        new DeliveryOptions().setCodecName(SlackerResponseMessageCodec.NAME)))).succeeded());
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(200, res.statusCode());
      validateResponseHeaders(res);
      res.bodyHandler(b -> {
        assertEquals(0, b.length());
        latch.countDown();
      });
    }).end(POST_DATA));
  }

  @Test
  public void test_responseWithText() {
    final SlackerResponse response = new SlackerResponse() {
      @Override
      public ResultCode getCode() {
        return ResultCode.INVALID;
      }

      @Override
      public Optional<String> getResponse() {
        return Optional.of("some reason");
      }
    };
    assertTrue(deployVerticle(new DummyEventListener(m -> m.reply(response,
        new DeliveryOptions().setCodecName(SlackerResponseMessageCodec.NAME)))).succeeded());
    wrapExec(latch -> basicPost("/command", res -> {
      assertEquals(400, res.statusCode());
      validateResponseHeaders(res);
      res.bodyHandler(b -> {
        assertEquals("{\"text\":\"some reason\"}", b.toString());
        latch.countDown();
      });
    }).end(POST_DATA));
  }

  private void validateResponseHeaders(final HttpClientResponse response) {
    final String cacheHeader = response.getHeader("Cache-Control");
    assertNotNull(cacheHeader);
    assertEquals("no-store, no-cache", cacheHeader);
  }

  private HttpClientRequest basicPost(final String uri,
      final Handler<HttpClientResponse> response) {
    return vertx.createHttpClient().post(SERVER_PORT, "localhost", uri, response)
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
        .putHeader(HttpHeaders.ACCEPT, "application/json");
  }

  // dummy event listeners for the test
  private static final class DummyEventListener extends AbstractVerticle {

    private final Consumer<Message<SlackerRequest>> consumer;

    private DummyEventListener(final Consumer<Message<SlackerRequest>> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void start() throws Exception {
      vertx.eventBus()
          .consumer("slacker-server", (Handler<Message<SlackerRequest>>) message -> {
            assertNotNull(message);
            assertThat(message.body(), instanceOf(SlackerRequest.class));
            consumer.accept(message);
          });
    }
  }
}
