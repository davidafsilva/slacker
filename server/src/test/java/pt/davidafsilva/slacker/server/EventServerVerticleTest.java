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

import java.time.Instant;
import java.util.Optional;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import pt.davidafsilva.slacker.api.ResultCode;
import pt.davidafsilva.slacker.api.SlackerBaseTest;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerRequestBuilder;
import pt.davidafsilva.slacker.api.SlackerRequestMessageCodec;
import pt.davidafsilva.slacker.api.SlackerResponse;
import pt.davidafsilva.slacker.api.SlackerResponseMessageCodec;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link EventServerVerticle} object.
 *
 * @author david
 */
@RunWith(VertxUnitRunner.class)
public class EventServerVerticleTest extends SlackerBaseTest {

  // the request used for the test execution
  private static final SlackerRequest REQUEST = new SlackerRequestBuilder()
      .timestamp(Instant.now())
      .channelId("12345")
      .channelName("#dope")
      .userId("6789")
      .userName("david")
      .teamDomain("slack.davidafsilva.pt")
      .teamIdentifier("davidafsilva")
      .command("test")
      .args("123 456")
      .build();

  @Override
  public void setup() throws Exception {
    super.setup();

    // register the codecs
    vertx.eventBus()
        .registerCodec(new SlackerRequestMessageCodec())
        .registerCodec(new SlackerResponseMessageCodec());

    // register the event verticle
    assertTrue(deployVerticle(new EventServerVerticle()).succeeded());
  }

  @Test
  public void test_register_nullRequest() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS, null, reply -> {
      assertTrue(reply.failed());
      assertEquals("invalid register event received", reply.cause().getMessage());
      latch.countDown();
    }));
  }

  @Test
  public void test_register_nonJsonRequest() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS, "x", reply -> {
      assertTrue(reply.failed());
      assertEquals("invalid register event received", reply.cause().getMessage());
      latch.countDown();
    }));
  }

  @Test
  public void test_register_invalidRequestData() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS,
        new JsonObject().put("v", "1.0.0"), reply -> {
          assertTrue(reply.failed());
          assertEquals("unable to register executor: missing required fields",
              reply.cause().getMessage());
          latch.countDown();
        }));
  }

  @Test
  public void test_register_success() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS,
        new JsonObject().put("i", "test").put("v", "1.0.0"), reply -> {
          assertTrue(reply.succeeded());
          assertNotNull(reply.result().body());
          assertThat(reply.result().body(), instanceOf(JsonObject.class));
          final JsonObject json = (JsonObject) reply.result().body();
          assertTrue(json.containsKey("r"));
          assertEquals("OK", json.getString("r"));
          assertTrue(json.containsKey("a"));
          assertEquals(32 + 17, json.getString("a").length());
          latch.countDown();
        }));
  }

  @Test
  public void test_request_nullRequest() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, null, reply -> {
      assertTrue(reply.failed());
      assertEquals("invalid request event received", reply.cause().getMessage());
      latch.countDown();
    }));
  }

  @Test
  public void test_request_nonSlackerRequest() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, "x", reply -> {
      assertTrue(reply.failed());
      assertEquals("invalid request event received", reply.cause().getMessage());
      latch.countDown();
    }));
  }

  @Test
  public void test_request_noExecutor() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, REQUEST,
        new DeliveryOptions().setCodecName(SlackerRequestMessageCodec.NAME),
        reply -> {
          assertTrue(reply.failed());
          assertEquals("no executor available for the command: test", reply.cause().getMessage());
          latch.countDown();
        }));
  }

  @Test
  public void test_request_failedExecutorProcessing() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS,
        new JsonObject().put("i", "test").put("v", "1.0.0"), reply -> {
          final JsonObject json = (JsonObject) reply.result().body();
          vertx.eventBus().consumer(json.getString("a"), req -> req.fail(99, "dummy"));
          latch.countDown();
        }));

    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, REQUEST,
        new DeliveryOptions().setCodecName(SlackerRequestMessageCodec.NAME),
        reply -> {
          assertTrue(reply.failed());
          assertEquals("failed test processing: dummy", reply.cause().getMessage());
          latch.countDown();
        }));
  }

  @Test
  public void test_request_executorReplyWithWrongType() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS,
        new JsonObject().put("i", "test").put("v", "1.0.0"), reply -> {
          final JsonObject json = (JsonObject) reply.result().body();
          vertx.eventBus().consumer(json.getString("a"), req -> req.reply("dummy"));
          latch.countDown();
        }));

    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, REQUEST,
        new DeliveryOptions().setCodecName(SlackerRequestMessageCodec.NAME),
        reply -> {
          assertTrue(reply.failed());
          assertEquals("failed test processing: invalid response", reply.cause().getMessage());
          latch.countDown();
        }));
  }

  @Test
  public void test_request_success() {
    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REG_SERVER_ADDRESS,
        new JsonObject().put("i", "test").put("v", "1.0.0"), reply -> {
          final JsonObject json = (JsonObject) reply.result().body();
          vertx.eventBus().consumer(json.getString("a"), req -> req.reply(new SlackerResponse() {
            @Override
            public ResultCode getCode() {
              return ResultCode.OK;
            }

            @Override
            public Optional<String> getResponse() {
              return Optional.of("test");
            }
          }, new DeliveryOptions().setCodecName(SlackerResponseMessageCodec.NAME)));
          latch.countDown();
        }));

    wrapExec(latch -> vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, REQUEST,
        new DeliveryOptions().setCodecName(SlackerRequestMessageCodec.NAME),
        reply -> {
          assertTrue(reply.succeeded());
          assertNotNull(reply.result().body());
          assertThat(reply.result().body(), instanceOf(SlackerResponse.class));
          final SlackerResponse response = (SlackerResponse) reply.result().body();
          assertEquals(ResultCode.OK, response.getCode());
          assertTrue(response.getResponse().isPresent());
          assertEquals("test", response.getResponse().get());
          latch.countDown();
        }));
  }
}
