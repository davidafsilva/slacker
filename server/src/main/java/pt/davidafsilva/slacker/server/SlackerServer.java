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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import pt.davidafsilva.slacker.api.SlackerRequestMessageCodec;
import pt.davidafsilva.slacker.api.SlackerResponseMessageCodec;

/**
 * This verticle is the main, root verticle for the slacker application.
 * It is responsible for the deployment of both {@link HttpServerVerticle} and {@link
 * EventServerVerticle}, which will handle the slacker HTTP requests and Event based communication,
 * respectively.
 *
 * The order in which the inner verticles are deployed are:
 * <ol>
 * <li>{@link HttpServerVerticle}</li>
 * <li>{@link EventServerVerticle}</li>
 * </ol>
 * The un-deploy is done in reverse order.
 *
 * @author david
 */
public final class SlackerServer extends AbstractVerticle {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(SlackerServer.class);

  // the event verticle deployment id
  private volatile String eventVerticleId;

  // the http verticle deployment id
  private volatile String httpVerticleId;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {
    // register the shared codecs
    vertx.eventBus()
        .registerCodec(new SlackerRequestMessageCodec())
        .registerCodec(new SlackerResponseMessageCodec());

    // deploy the event server first
    deployVerticle(new EventServerVerticle(), eid -> {
      eventVerticleId = eid;
      // then deploy the http server
      deployVerticle(new HttpServerVerticle(), hid -> {
        httpVerticleId = hid;
        LOGGER.info("successfully completed the base slacker server deployment");
        startFuture.complete();
      }, v -> startFuture.fail("failed to deploy http verticle"));
    }, v -> startFuture.fail("failed to deploy event verticle"));
  }

  @Override
  public void stop(final Future<Void> stopFuture) throws Exception {
    // un-deploy the http server first
    vertx.undeploy(httpVerticleId, er ->
        // then the event server
        vertx.undeploy(eventVerticleId, hr -> {
          LOGGER.info("un-deployment complete.");
          stopFuture.complete();
        }));
  }

  /**
   * Deploys the specified verticle and executes the success or failure handler accordingly.
   *
   * @param v       the verticle to be deployed
   * @param success the success handler which will receive the deployment id has argument
   * @param failure the failure handler
   */
  private void deployVerticle(final Verticle v, final Handler<String> success,
      final Handler<Void> failure) {
    vertx.deployVerticle(v, res -> {
      if (res.succeeded()) {
        LOGGER.info("successfully deployed {}", v.getClass().getSimpleName());
        success.handle(res.result());
      } else {
        LOGGER.error("failed to deploy {}", res.cause(), v.getClass().getSimpleName());
        failure.handle(null);
      }
    });
  }
}
