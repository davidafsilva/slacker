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

import java.time.Instant;
import java.util.Objects;

/**
 * A builder for a fluent creation of {@link SlackerRequest} messages.
 *
 * @author david
 */
public final class SlackerRequestBuilder {

  // properties
  private Instant timestamp;
  private String teamIdentifier;
  private String teamDomain;
  private String channelId;
  private String channelName;
  private String userId;
  private String userName;
  private String trigger;
  private String text;

  public SlackerRequestBuilder timestamp(final Instant timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public SlackerRequestBuilder teamIdentifier(final String teamIdentifier) {
    this.teamIdentifier = teamIdentifier;
    return this;
  }

  public SlackerRequestBuilder teamDomain(final String teamDomain) {
    this.teamDomain = teamDomain;
    return this;
  }

  public SlackerRequestBuilder channelId(final String channelId) {
    this.channelId = channelId;
    return this;
  }

  public SlackerRequestBuilder channelName(final String channelName) {
    this.channelName = channelName;
    return this;
  }

  public SlackerRequestBuilder userId(final String userId) {
    this.userId = userId;
    return this;
  }

  public SlackerRequestBuilder userName(final String userName) {
    this.userName = userName;
    return this;
  }

  public SlackerRequestBuilder trigger(final String trigger) {
    this.trigger = trigger;
    return this;
  }

  public SlackerRequestBuilder text(final String text) {
    this.text = text;
    return this;
  }

  /**
   * Builds a slacker request with the current state of the builder.
   *
   * @return a new instance of {@link SlackerRequest} with the specified parameters
   * @throws NullPointerException if the builder state is incomplete and unable to create the
   *                              proper request message
   */
  public SlackerRequest build() {
    return new SlackerRequestImpl(
        Objects.requireNonNull(timestamp, "timestamp"),
        Objects.requireNonNull(teamIdentifier, "team identifier"),
        Objects.requireNonNull(teamDomain, "team domain"),
        Objects.requireNonNull(channelId, "channel identifier"),
        Objects.requireNonNull(channelName, "channel name"),
        Objects.requireNonNull(userId, "user identifier"),
        Objects.requireNonNull(userName, "user name"),
        Objects.requireNonNull(trigger, "trigger"),
        Objects.requireNonNull(text, "text"));
  }

  // straight-forward implementation of the slacker request
  static final class SlackerRequestImpl implements SlackerRequest {

    // the request timestamp
    private final Instant timestamp;
    // the team identifier
    private final String teamIdentifier;
    // the team domain
    private final String teamDomain;
    // the channel id
    private final String channelId;
    // the channel name
    private final String channelName;
    // the user identifier
    private final String userId;
    // the user name
    private final String userName;
    // the trigger
    private final String trigger;
    // the complete text (incl. trigger)
    private final String text;

    /**
     * Default constructor
     *
     * @param timestamp      the request timestamp
     * @param teamIdentifier the team identifier
     * @param teamDomain     the team domain
     * @param channelId      the channel id
     * @param channelName    the channel name
     * @param userId         the user identifier
     * @param userName       the user name
     * @param trigger        the trigger
     * @param text           the complete text (incl. trigger)
     */
    private SlackerRequestImpl(final Instant timestamp, final String teamIdentifier,
        final String teamDomain, final String channelId, final String channelName,
        final String userId, final String userName, final String trigger, final String text) {
      this.timestamp = timestamp;
      this.teamIdentifier = teamIdentifier;
      this.teamDomain = teamDomain;
      this.channelId = channelId;
      this.channelName = channelName;
      this.userId = userId;
      this.userName = userName;
      this.trigger = trigger;
      this.text = text;
    }


    @Override
    public Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public String getTeamIdentifier() {
      return teamIdentifier;
    }

    @Override
    public String getTeamDomain() {
      return teamDomain;
    }

    @Override
    public String getChannelId() {
      return channelId;
    }

    @Override
    public String getChannelName() {
      return channelName;
    }

    @Override
    public String getUserId() {
      return userId;
    }

    @Override
    public String getUserName() {
      return userName;
    }

    @Override
    public String getTrigger() {
      return trigger;
    }

    @Override
    public String getText() {
      return text;
    }
  }
}
