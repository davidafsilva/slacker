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

import java.util.Optional;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerRequestMessageCodec;
import pt.davidafsilva.slacker.api.SlackerResponse;
import pt.davidafsilva.slacker.api.SlackerResponseMessageCodec;

/**
 * The event server that shall receive the incoming requests (events) from the http server and
 * forward them to the respective slacker-executor, if any is available.
 *
 * @author david
 */
final class EventServerVerticle extends AbstractVerticle {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(EventServerVerticle.class);

  // the event consumer addresses
  static final String REQ_SERVER_ADDRESS = "req.slacker-server";
  static final String REG_SERVER_ADDRESS = "reg.slacker-server";

  // the event consumer instances
  private MessageConsumer<Object> registerConsumer;
  private MessageConsumer<Object> requestConsumer;

  // the executor registry
  private ExecutorRegistry executorRegistry;

  @Override
  public void start() throws Exception {
    // create the executor registry
    executorRegistry = new ExecutorRegistry();

    // register the event consumers
    registerConsumer = vertx.eventBus().consumer(REG_SERVER_ADDRESS, this::handlerRegisterEvent);
    requestConsumer = vertx.eventBus().consumer(REQ_SERVER_ADDRESS, this::handlerRequestEvent);
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    // stop the consumers
    registerConsumer.unregister(r1 -> {
      LOGGER.info("slacker register consumer closed: {} (cause: {})", r1.succeeded(), r1.cause());
      requestConsumer.unregister(r2 -> {
        LOGGER.info("slacker request consumer closed: {} (cause: {})", r2.succeeded(), r2.cause());
        stopFuture.complete();
      });
    });
  }

  /**
   * Handles a executor register request message event by trying to register the executor with the
   * received information.
   * The registry might fail due to incompatible versions
   *
   * @param message the request message event
   */
  private void handlerRegisterEvent(final Message<Object> message) {
    LOGGER.debug("received register event message: {}", message.body());

    // validate the received event
    if (message.body() == null || !JsonObject.class.isInstance(message.body())) {
      message.fail(1, "invalid register event received");
      return;
    }

    // try to register the executor
    final JsonObject executorRequest = (JsonObject) message.body();
    executorRegistry.register(executorRequest,
        address -> message.reply(new JsonObject().put("a", address)),
        reason -> message.fail(1, String.format("unable to register executor: %s", reason)));
  }

  /**
   * Handles a request message event by delivering the request to the appropriate executor, if
   * any is registered to handle that particular type of request.
   *
   * @param message the request message event
   */
  private void handlerRequestEvent(final Message<Object> message) {
    LOGGER.debug("received request event message: {}", message.body());

    // validate the received event
    if (message.body() == null || !SlackerRequest.class.isInstance(message.body())) {
      LOGGER.error("invalid event body");
      message.fail(1, "invalid request event received");
      return;
    }

    // handle the request
    final SlackerRequest request = (SlackerRequest) message.body();
    executorRegistry.lookup(request.getCommand(),
        address -> sendRequestToExecutor(address, request, message),
        v -> message.fail(1, String.format("no executor available for the command: %s",
            request.getCommand())));
  }

  /**
   * Sends the requests to the executor and handles the reply
   *
   * @param address        the address of the executor
   * @param request        the request to be sent
   * @param requestMessage the original request message with the reply address
   */
  private void sendRequestToExecutor(final String address, final SlackerRequest request,
      final Message<Object> requestMessage) {
    LOGGER.debug("forwarding request message to {}..", address);
    vertx.eventBus().send(address, request, new DeliveryOptions()
        .setCodecName(SlackerRequestMessageCodec.NAME), reply -> {
      if (reply.succeeded() && SlackerResponse.class.isInstance(reply.result().body())) {
        requestMessage.reply(reply.result().body(), new DeliveryOptions()
            .setCodecName(SlackerResponseMessageCodec.NAME));
      } else {
        LOGGER.error("failed to process request", reply.cause());
        requestMessage.fail(2, String.format("failed %s processing: %s", request.getCommand(),
            Optional.ofNullable(reply.cause()).map(Throwable::getMessage)
                .orElse("invalid response")));
      }
    });
  }
}
