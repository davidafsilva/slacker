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

import java.time.Instant;

/**
 * The slack request data
 *
 * @author david
 */
public interface SlackRequest {

  /**
   * Returns the API token
   *
   * @return the API token
   */
  String getToken();

  /**
   * Returns the request timestamp
   *
   * @return the request timestamp
   */
  Instant getTimestamp();

  /**
   * Returns the team identifier
   *
   * @return the team identifier
   */
  String getTeamIdentifier();

  /**
   * Returns the team domain name
   *
   * @return the domain name
   */
  String getTeamDomain();

  /**
   * Returns the channel identifier from which the message was sent
   *
   * @return the channel id
   */
  String getChannelId();

  /**
   * Returns the channel name from which the message was sent
   *
   * @return the channel name
   */
  String getChannelName();

  /**
   * The user identifier that triggered the request
   *
   * @return the user identifier
   */
  String getUserId();

  /**
   * Returns the user name that triggered the request
   *
   * @return the user name
   */
  String getUserName();

  /**
   * Returns the configured trigger that originated this request
   *
   * @return the trigger command
   */
  String getTrigger();

  /**
   * Returns the full text message
   *
   * @return the full text message
   */
  String getText();
}
