package pt.davidafsilva.slacker.server;

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
import org.junit.runners.Parameterized;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SlackRequest} object.
 *
 * @author david
 */
@RunWith(Parameterized.class)
public class SlackRequestTest {

  // test parameters values
  private static final String TOKEN_VALUE = "token";
  private static final long TIMESTAMP_SECONDS_VALUE = 1355517523;
  private static final long TIMESTAMP_NANOS_VALUE = 5000;
  private static final String TIMESTAMP_VALUE = "1355517523.000005";
  private static final String TEAM_ID_VALUE = "12345";
  private static final String TEAM_DOMAIN_VALUE = "domain 1";
  private static final String CHANNEL_ID_VALUE = "channel 1";
  private static final String CHANNEL_NAME_VALUE = "#boo";
  private static final String USER_ID_VALUE = "1";
  private static final String USER_NAME_VALUE = "david";
  private static final String TRIGGER_WORD_VALUE = "!boo";
  private static final String TEXT_VALUE = "!boo woop woop";
  private static final String REQUEST_JSON = "{\n" +
      "  \"token\" : \"token\",\n" +
      "  \"timestamp\" : \"2012-12-14T20:38:43.000005Z\",\n" +
      "  \"teamIdentifier\" : \"12345\",\n" +
      "  \"teamDomain\" : \"domain 1\",\n" +
      "  \"channelId\" : \"channel 1\",\n" +
      "  \"channelName\" : \"#boo\",\n" +
      "  \"userId\" : \"1\",\n" +
      "  \"userName\" : \"david\",\n" +
      "  \"trigger\" : \"!boo\",\n" +
      "  \"text\" : \"!boo woop woop\"\n" +
      "}";

  @Parameterized.Parameters
  public static Iterable<Object[]> testDataSupplier() {
    return Arrays.asList(new Object[][]{
            {create(Optional.of(SlackRequest.REQUEST_TOKEN)), false},
            {create(Optional.of(SlackRequest.REQUEST_TIMESTAMP)), false},
            {create(Optional.of(SlackRequest.REQUEST_TEAM_ID)), false},
            {create(Optional.of(SlackRequest.REQUEST_TEAM_DOMAIN)), false},
            {create(Optional.of(SlackRequest.REQUEST_CHANNEL_ID)), false},
            {create(Optional.of(SlackRequest.REQUEST_CHANNEL_NAME)), false},
            {create(Optional.of(SlackRequest.REQUEST_USER_ID)), false},
            {create(Optional.of(SlackRequest.REQUEST_USER_NAME)), false},
            {create(Optional.of(SlackRequest.REQUEST_TRIGGER_WORD)), false},
            {create(Optional.of(SlackRequest.REQUEST_TEXT)), false},
            {create(Optional.empty()), true},
        }
    );
  }

  /**
   * Creates the base routing context for the test execution with all of the attributes filled
   *
   * @param excluded the excluded request attribute, if any
   * @return the test routing context
   */
  private static RoutingContext create(final Optional<String> excluded) {
    final RoutingContext context = mock(RoutingContext.class);
    final HttpServerRequest request = mock(HttpServerRequest.class);
    final MultiMap attributes = mock(MultiMap.class);
    addAttribute(attributes, SlackRequest.REQUEST_TOKEN, TOKEN_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_TIMESTAMP, TIMESTAMP_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_TEAM_ID, TEAM_ID_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_TEAM_DOMAIN, TEAM_DOMAIN_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_CHANNEL_ID, CHANNEL_ID_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_CHANNEL_NAME, CHANNEL_NAME_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_USER_ID, USER_ID_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_USER_NAME, USER_NAME_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_TRIGGER_WORD, TRIGGER_WORD_VALUE, excluded);
    addAttribute(attributes, SlackRequest.REQUEST_TEXT, TEXT_VALUE, excluded);
    when(request.formAttributes()).thenReturn(attributes);
    when(context.request()).thenReturn(request);
    return context;
  }

  /**
   * Adds the given attribute-value to the attributes map if it is not equal to {@code excluded}.
   *
   * @param attributes the attributes map
   * @param key        the attribute key
   * @param value      the attribute value
   * @param excluded   the attribute to be excluded, if any
   */
  private static void addAttribute(final MultiMap attributes, final String key, final String value,
      final Optional<String> excluded) {
    if (!excluded.map(e -> e.equals(key)).orElse(false)) {
      when(attributes.get(key)).thenReturn(value);
    }
  }

  // test run parameters
  private final RoutingContext context;
  private final boolean isPresent;

  /**
   * Default test class constructor
   *
   * @param context   the test run routing context
   * @param isPresent if the expected output has or not a request present
   */
  public SlackRequestTest(final RoutingContext context, final boolean isPresent) {
    this.context = context;
    this.isPresent = isPresent;
  }

  @Test
  public void parseContext() {
    final Optional<SlackRequest> requestOptional = SlackRequest.parse(context);
    assertEquals(isPresent, requestOptional.isPresent());
    requestOptional.ifPresent(request -> {
      assertEquals(TOKEN_VALUE, request.getToken());
      assertEquals(Instant.ofEpochSecond(TIMESTAMP_SECONDS_VALUE, TIMESTAMP_NANOS_VALUE),
          request.getTimestamp());
      assertEquals(TEAM_ID_VALUE, request.getTeamIdentifier());
      assertEquals(TEAM_DOMAIN_VALUE, request.getTeamDomain());
      assertEquals(CHANNEL_ID_VALUE, request.getChannelId());
      assertEquals(CHANNEL_NAME_VALUE, request.getChannelName());
      assertEquals(USER_ID_VALUE, request.getUserId());
      assertEquals(USER_NAME_VALUE, request.getUserName());
      assertEquals(TRIGGER_WORD_VALUE, request.getTrigger());
      assertEquals(TEXT_VALUE, request.getText());
      assertEquals(REQUEST_JSON, request.toString());
    });
  }
}
