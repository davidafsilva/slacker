package pt.davidafsilva.slacker.api;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;

/**
 * The slack request data
 *
 * @author David Silva
 */
public final class SlackRequest {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(SlackRequest.class);

  // the supplier for the exception thrown when a POST request field is missing
  private static final Function<String, Supplier<RuntimeException>> NO_VALUE_EXCEPTION = field ->
      () -> new NoSuchElementException("required request field is missing: " + field);

  // the request parameters
  static final String REQUEST_TOKEN = "token";
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

  // ---------------
  // JSON properties
  // ---------------

  // the API token
  private String token;
  // the timestamp of the request
  private Instant timestamp;

  // team data
  private String teamIdentifier;
  private String teamDomain;

  // channel data
  private String channelId;
  private String channelName;

  // user data
  private String userId;
  private String userName;

  // actual request data
  private String trigger;
  private String text;

  // private constructor
  private SlackRequest() {}

  /**
   * Creates a slack request from the given POST request context.
   * If any error occurs, i.e. there is a missing field from the request, an {@link
   * Optional#empty()} is returned.
   *
   * @param context the routing context
   * @return the optional with the slack request, if successfully parsed
   */
  public static Optional<SlackRequest> parse(final RoutingContext context) {
    Optional<SlackRequest> optionalRequest;
    try {
      final SlackRequest request = new SlackRequest();
      request.token = getPostValue(context, REQUEST_TOKEN);
      request.timestamp = Instant.from(
          TIMESTAMP_FORMATTER.parse(getPostValue(context, REQUEST_TIMESTAMP)));
      request.teamIdentifier = getPostValue(context, REQUEST_TEAM_ID);
      request.teamDomain = getPostValue(context, REQUEST_TEAM_DOMAIN);
      request.channelId = getPostValue(context, REQUEST_CHANNEL_ID);
      request.channelName = getPostValue(context, REQUEST_CHANNEL_NAME);
      request.userId = getPostValue(context, REQUEST_USER_ID);
      request.userName = getPostValue(context, REQUEST_USER_NAME);
      request.trigger = getPostValue(context, REQUEST_TRIGGER_WORD);
      request.text = getPostValue(context, REQUEST_TEXT);
      optionalRequest = Optional.of(request);
    } catch (final Exception e) {
      LOGGER.error("unable to parse request", e);
      optionalRequest = Optional.empty();
    }

    return optionalRequest;
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

  /**
   * Returns the API token
   *
   * @return the API token
   */
  public String getToken() {
    return token;
  }

  /**
   * Returns the request timestamp
   *
   * @return the request timestamp
   */
  @JsonSerialize(using = ToStringSerializer.class)
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Returns the team identifier
   *
   * @return the team identifier
   */
  public String getTeamIdentifier() {
    return teamIdentifier;
  }

  /**
   * Returns the team domain name
   *
   * @return the domain name
   */
  public String getTeamDomain() {
    return teamDomain;
  }

  /**
   * Returns the channel identifier from which the message was sent
   *
   * @return the channel id
   */
  public String getChannelId() {
    return channelId;
  }

  /**
   * Returns the channel name from which the message was sent
   *
   * @return the channel name
   */
  public String getChannelName() {
    return channelName;
  }

  /**
   * The user identifier that triggered the request
   *
   * @return the user identifier
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the user name that triggered the request
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Returns the configured trigger that originated this request
   *
   * @return the trigger command
   */
  public String getTrigger() {
    return trigger;
  }

  /**
   * Returns the full text message
   *
   * @return the full text message
   */
  public String getText() {
    return text;
  }

  @Override
  public String toString() {
    return Json.encodePrettily(this);
  }
}
