package pt.davidafsilva.slacker.server;

/*
 * #%L
 * slack-hello-back
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import pt.davidafsilva.slacker.api.ResultCode;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerRequestMessageCodec;
import pt.davidafsilva.slacker.api.SlackerResponse;

/**
 * The http server that shall receive the incoming requests from the slack web-hook and
 * forward them to the slacker-server via event-bus.
 *
 * @author david
 */
final class HttpServerVerticle extends AbstractVerticle {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  // the empty buffer
  private static final Buffer EMPTY_BUF = Buffer.buffer(0);

  // the http httpServer
  private HttpServer httpServer;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    // create the routing configuration
    final Router router = Router.router(vertx);

    // default handler
    router.route().handler(BodyHandler.create());

    // POST /command
    router.post("/command")
        .consumes("application/x-www-form-urlencoded")
        .produces("application/json")
        .handler(this::executeCommand);

    // create the http httpServer options by reading the boot configuration
    final HttpServerOptions options = HttpServerConfiguration.setup(config());
    LOGGER.debug("starting with the following configuration: {0}", config());

    // create the actual http httpServer
    httpServer = vertx.createHttpServer(options)
        .requestHandler(router::accept)
        .listen(deployedHandler -> {
          if (deployedHandler.succeeded()) {
            LOGGER.info(String.format("slacker http server listening at port %d",
                options.getPort()));
            startFuture.complete();
          } else {
            startFuture.fail(deployedHandler.cause());
          }
        });
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    LOGGER.info("closing slacker http server..");
    httpServer.close(r -> {
      LOGGER.info("slacker http server closed: {0} (cause: {1})", r.succeeded(), r.cause());
      stopFuture.complete();
    });
  }

  /**
   * Handles the incoming hello requests
   *
   * @param context the routing context of the request
   */
  private void executeCommand(final RoutingContext context) {
    LOGGER.info("received command request");
    // create the request data from the POST request
    final Optional<SlackerRequest> slackRequest = HttpContextSlackerRequestParser.parse(context);
    LOGGER.debug("request data: {0}", slackRequest);

    // dispatch the request to the slacker server
    if (slackRequest.isPresent()) {
      final SlackerRequest r = slackRequest.get();
      vertx.eventBus().send(EventServerVerticle.REQ_SERVER_ADDRESS, r, new DeliveryOptions()
          .setCodecName(SlackerRequestMessageCodec.NAME), reply -> {
        LOGGER.info("received reply from slacker-server for request");
        LOGGER.debug(reply);

        if (reply.succeeded()) {
          final Object body = reply.result().body();
          if (body != null && SlackerResponse.class.isInstance(body)) {
            LOGGER.info("valid response found");
            final SlackerResponse response = (SlackerResponse) body;
            endRequest(context, response.getCode(), response.getResponse());
          } else {
            // terminate the request, it went ok even though no valid response has been received
            LOGGER.warn("no valid response object was found");
            endRequest(context, ResultCode.ERROR, Optional.empty());
          }
        } else {
          LOGGER.error("unable to process request", reply.cause());
          endRequest(context, ResultCode.ERROR, Optional.empty());
        }
      });
    } else {
      // fail silently
      endRequest(context, ResultCode.INVALID, Optional.empty());
    }
  }

  /**
   * Ends the current request identified by the given routing context with the specified result
   * code and optional response text
   *
   * @param context  the http request being handled
   * @param code     the response code, which determines the response http code
   * @param response the response text to send back to the channel, if any
   */
  private void endRequest(final RoutingContext context, final ResultCode code,
      final Optional<String> response) {
    LOGGER.info("terminating request with code {0} and message {1}", code, response);
    final Buffer message = response
        .map(m -> new JsonObject().put("text", m).toString())
        .map(Buffer::buffer)
        .orElse(EMPTY_BUF);
    context.response()
        .setStatusCode(code.getHttpStatus().code())
        .putHeader("Cache-Control", "no-store, no-cache")
        .end(message);
  }
}
