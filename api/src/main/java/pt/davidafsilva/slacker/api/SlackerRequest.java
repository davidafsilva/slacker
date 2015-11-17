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
import java.util.Optional;

/**
 * The request information for the (remote) slacker command execution.
 *
 * @author david
 * @since 1.0
 */
public interface SlackerRequest {

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
   * Returns the command that was issued from the channel
   *
   * @return the issued command
   */
  String getCommand();

  /**
   * Returns the arguments for the command if any is available.
   * This is essentially the full-text message without the trigger and command bits.
   *
   * @return the command arguments
   */
  Optional<String> getArguments();
}
