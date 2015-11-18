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

import java.util.Objects;
import java.util.Optional;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.ServiceHelper;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.FutureFactory;

/**
 * This abstract implementation serves as a baseline for slacker command executors.
 * It includes the communication protocol for registering the executor for the supported command
 * as well as utility methods for creating the sending the execution results.
 *
 * @author david
 * @since 1.0
 */
public abstract class AbstractSlackerExecutor implements SlackerExecutor {

  // the logger instance
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSlackerExecutor.class);

  // the vertx instance that deployed this verticle
  private Vertx vertx;

  // the verticle context
  private Context context;

  // the future factory
  private FutureFactory futureFactory;

  // the executor slacker requests consumer
  private Optional<MessageConsumer<SlackerRequest>> consumer;

  @Override
  public void init(final Vertx vertx, final Context context) {
    this.vertx = vertx;
    this.context = context;
    this.futureFactory = ServiceHelper.loadFactory(FutureFactory.class);
    // register the slacker message codecs
    registerCodecs();
  }

  /**
   * Registers the required {@link MessageCodec} for the {@link SlackerRequest} and {@link
   * SlackerResponse} messages.
   */
  private void registerCodecs() {
    try {
      vertx.eventBus()
          .registerCodec(new SlackerRequestMessageCodec())
          .registerCodec(new SlackerResponseMessageCodec());
    } catch (final IllegalStateException e) {
      LOGGER.debug("codecs already registered", e);
    }
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    LOGGER.info("starting {}..", identifier());

    // start the HELLO slacker protocol
    final JsonObject helloMessage = new JsonObject()
        .put("i", identifier())
        .put("d", description())
        .put("v", version());
    vertx.eventBus().send("slacker-server", helloMessage, result -> {
      if (result.succeeded() && JsonObject.class.isInstance(result.result().body())) {
        final JsonObject response = (JsonObject) result.result().body();
        if (response.containsKey("a")) {
          // everything went smoothly - register the listener and complete the startup
          registerListener(response.getString("a"));
          LOGGER.info("successfully registered '{}' executor", identifier());
          startFuture.complete();
        } else {
          failStart(startFuture, "no address to bind was received");
        }
      } else {
        // something unexpected happened
        failStart(startFuture, Optional.ofNullable(result.cause())
            .map(Throwable::getMessage)
            .orElse("invalid response"));
      }
    });
  }

  /**
   * Registers the listener for for this executor on the underlying event bus, so that this
   * executor can successfully receive slacker command requests.
   *
   * Note that this method should only be called when the executor has been successfully registered
   * at the slacker-server.
   *
   * @param address the address assigned by slacker-server
   */
  protected void registerListener(final String address) {
    consumer = Optional.of(vertx.eventBus().consumer(address, this::handleExecutorEvent));
  }

  /**
   * Creates a success response with the result code equal to {@link ResultCode#OK} and no
   * message as reply.
   * This factory method shall be used whenever the executor work-flow has been completed and as
   * such we want to send a reply to the server without sending any message to the channel/issuer
   * of the command.
   *
   * @return a new instance of a success slacker response
   */
  protected SlackerResponse success() {
    return SlackerResponseFactory.create(ResultCode.OK, Optional.empty());
  }

  /**
   * Creates a success response with the result code equal to {@link ResultCode#OK} and with the
   * given message as reply.
   * This factory method shall be used whenever the executor work-flow has been completed and as
   * such we want to send a reply to the server and also inform the channel/issuer of the command.
   *
   * @param message the message to be included at the response
   * @return a new instance of an success slacker response
   */
  protected SlackerResponse success(final String message) {
    return response(ResultCode.OK, message);
  }

  /**
   * Creates a error response with the result code equal to {@link ResultCode#ERROR} and with the
   * given message as the error reason.
   * This factory method shall be used to create the reply message whenever and unexpected error
   * has occurred, such as an {@link Exception} that has been thrown/catched.
   *
   * @param message the message with the error reason
   * @return a new instance of an error slacker response
   */
  protected SlackerResponse error(final String message) {
    return response(ResultCode.ERROR, message);
  }

  /**
   * Creates a invalid response with the result code equal to {@link ResultCode#INVALID} and with
   * the given message as the error reason.
   * This factory method shall be used whenever invalid data as been received.
   *
   * @param message the message with the error reason
   * @return a new instance of an error slacker response
   */
  protected SlackerResponse invalid(final String message) {
    return response(ResultCode.INVALID, message);
  }

  private SlackerResponse response(final ResultCode code, final String message) {
    return SlackerResponseFactory.create(code, Optional.of(
        Objects.requireNonNull(message, "message")));
  }

  /**
   * Handles an incoming request from the event bus
   *
   * @param request the request message to be handled
   */
  private void handleExecutorEvent(final Message<SlackerRequest> request) {
    LOGGER.info("<=<= receiving incoming request <=<=");
    LOGGER.debug(request);

    // execute the request handling asynchronously
    context.runOnContext(a -> {
      final Future<SlackerResponse> future = futureFactory.future();
      execute(request.body(), future);
      future.setHandler(handler -> {
        if (handler.succeeded()) {
          LOGGER.info("=>=> successfully handled request =>=>");
          LOGGER.debug(handler.result());
          request.reply(handler.result(), new DeliveryOptions()
              .setCodecName(SlackerResponseMessageCodec.NAME));
        } else {
          request.fail(ResultCode.ERROR.ordinal(), handler.cause().getMessage());
          LOGGER.error("failed to handle request", handler.cause());
        }
      });
    });
  }

  /**
   * Fails the startup of this executor with the given failure reason
   *
   * @param startFuture  the start future to be canceled
   * @param errorMessage the error/failure message
   */
  private void failStart(final Future<Void> startFuture, final String errorMessage) {
    final String reason = String.format("unable to register '%s' executor: %s", identifier(),
        errorMessage);
    LOGGER.error(reason);
    startFuture.fail(reason);
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    LOGGER.info("stopping {}..", identifier());
    consumer.ifPresent(MessageConsumer::unregister);
    stopFuture.complete();
  }
}
