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
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * The hello-back service for slack
 *
 * @author david
 */
public final class InboundHttpServerVerticle extends AbstractVerticle {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(InboundHttpServerVerticle.class);

  // the http server
  private HttpServer server;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    // create the routing configuration
    final Router router = Router.router(vertx);

    // default handler
    router.route().handler(BodyHandler.create());

    // POST /command
    router.post("/command")
        .consumes("application/json")
        .produces("application/json")
        .handler(this::executeCommand);

    // create the http server options by reading the boot configuration
    final HttpServerOptions options = HttpServerConfiguration.setup(config());
    LOGGER.debug("starting with the following configuration:%n{}", config());

    // create the actual http server
    server = vertx.createHttpServer(options)
        .requestHandler(router::accept)
        .listen(deployedHandler -> {
          if (deployedHandler.succeeded()) {
            LOGGER.info(String.format("http server listening at port %s", options.getPort()));
            startFuture.complete();
          } else {
            throw new IllegalStateException("unable to start http server", deployedHandler.cause());
          }
        });
  }

  @Override
  public void stop() throws Exception {
    server.close();
  }

  /**
   * Handles the incoming hello requests
   *
   * @param context the routing context of the request
   */
  private void executeCommand(final RoutingContext context) {
    LOGGER.info("received command request");
    // create the request data from the POST request
    final Optional<SlackRequest> slackRequest = SlackRequest.parse(context);
    LOGGER.info("request data: {}", slackRequest);

    // handle the request by the command handler
    slackRequest.ifPresent(request -> {
      //TODO: handle request
      final JsonObject response = new JsonObject()
          .put("text", "PONG");

      // send the response back to the channel
      context.response().setStatusCode(200)
          .end(response.toString());
    });
  }
}
