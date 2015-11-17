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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerRequestBuilder;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;

/**
 * The http-server based slacker request
 *
 * @author david
 */
final class HttpContextSlackerRequestParser {

  // the logger
  private static final Logger
      LOGGER =
      LoggerFactory.getLogger(HttpContextSlackerRequestParser.class);

  // the supplier for the exception thrown when a POST request field is missing
  private static final Function<String, Supplier<RuntimeException>> NO_VALUE_EXCEPTION = field ->
      () -> new NoSuchElementException("required request field is missing: " + field);

  // the request parameters
  //static final String REQUEST_TOKEN = "token";
  static final String REQUEST_TIMESTAMP = "timestamp";
  static final String REQUEST_TEAM_ID = "team_id";
  static final String REQUEST_TEAM_DOMAIN = "team_domain";
  static final String REQUEST_CHANNEL_ID = "channel_id";
  static final String REQUEST_CHANNEL_NAME = "channel_name";
  static final String REQUEST_USER_ID = "user_id";
  static final String REQUEST_USER_NAME = "user_name";
  static final String REQUEST_TRIGGER_WORD = "trigger_word";
  static final String REQUEST_TEXT = "text";

  // the timestamp formatter
  private static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
      .appendValue(INSTANT_SECONDS, 10)
      .appendLiteral('.')
      .appendFraction(NANO_OF_SECOND, 0, 9, false)
      .toFormatter();

  // private constructor
  private HttpContextSlackerRequestParser() {
    throw new UnsupportedOperationException("no no no");
  }

  /**
   * Creates a slacker request from the given POST request context.
   * If any error occurs, i.e. there is a missing field from the request, an {@link
   * Optional#empty()} is returned.
   *
   * @param context the routing context
   * @return the optional with the slacker request, if successfully parsed
   */
  public static Optional<SlackerRequest> parse(final RoutingContext context) {
    Optional<SlackerRequest> optionalRequest;
    try {
      final SlackerRequestBuilder builder = new SlackerRequestBuilder();
      //getPostValue(context, REQUEST_TOKEN);
      builder.timestamp(Instant.from(TIMESTAMP_FORMATTER.parse(getPostValue(context,
          REQUEST_TIMESTAMP))));
      builder.teamIdentifier(getPostValue(context, REQUEST_TEAM_ID));
      builder.teamDomain(getPostValue(context, REQUEST_TEAM_DOMAIN));
      builder.channelId(getPostValue(context, REQUEST_CHANNEL_ID));
      builder.channelName(getPostValue(context, REQUEST_CHANNEL_NAME));
      builder.userId(getPostValue(context, REQUEST_USER_ID));
      builder.userName(getPostValue(context, REQUEST_USER_NAME));
      final String[] split = splitCommandAndArguments(getPostValue(context, REQUEST_TRIGGER_WORD),
          getPostValue(context, REQUEST_TEXT));
      builder.command(split[0]);
      builder.args(split.length > 1 ? split[1] : null);
      optionalRequest = Optional.of(builder.build());
    } catch (final Exception e) {
      LOGGER.error("unable to parse request", e);
      optionalRequest = Optional.empty();
    }

    return optionalRequest;
  }

  /**
   * Returns a split of the command and the respective arguments that were issued from the channel
   *
   * @param trigger the trigger of the message
   * @param text    the complete text of the command
   * @return the first position of array contains the command, the second is the argument
   */
  private static String[] splitCommandAndArguments(final String trigger, final String text) {
    return text.replaceFirst(trigger, "").split("\\s", 2);
  }

  /**
   * Returns the POST property value if it's available, otherwise an {@link NoSuchElementException}
   * is thrown.
   *
   * @param context  the routing context with the request data
   * @param property the desired POST property
   * @return the property value
   */
  private static String getPostValue(final RoutingContext context, final String property) {
    return Optional.ofNullable(context.request().formAttributes().get(property))
        .orElseThrow(NO_VALUE_EXCEPTION.apply(property));
  }
}
