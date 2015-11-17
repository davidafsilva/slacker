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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the {@link SlackerRequestBuilder}.
 *
 * @author david
 */
public class SlackerRequestBuilderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_invalidTimestamp() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("timestamp");
    new SlackerRequestBuilder()
        //.timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidChannelId() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("channel identifier");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        //.channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidChannelName() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("channel name");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        //.channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidUserId() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("user identifier");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        //.userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidUserName() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("user name");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        //.userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidTeamDomain() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("team domain");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        //.teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidTeamIdentifier() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("team identifier");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        //.teamIdentifier("davidafsilva")
        .command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_invalidCommand() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("command");
    new SlackerRequestBuilder()
        .timestamp(Instant.now())
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        //.command("test")
        .args("123 456")
        .build();
  }

  @Test
  public void test_success() {
    testWithArgs(Optional.of("123, 456"));
  }

  @Test
  public void test_success_noArgs() {
    testWithArgs(Optional.of(""));
    testWithArgs(Optional.empty());
  }

  private void testWithArgs(final Optional<String> args) {
    final Instant now = Instant.now();
    final SlackerRequestBuilder builder = new SlackerRequestBuilder()
        .timestamp(now)
        .channelId("12345")
        .channelName("#dope")
        .userId("6789")
        .userName("david")
        .teamDomain("slack.davidafsilva.pt")
        .teamIdentifier("davidafsilva")
        .command("test");
    args.ifPresent(builder::args);
    final SlackerRequest request = builder.build();
    assertNotNull(request);
    assertEquals(now, request.getTimestamp());
    assertEquals("12345", request.getChannelId());
    assertEquals("#dope", request.getChannelName());
    assertEquals("6789", request.getUserId());
    assertEquals("david", request.getUserName());
    assertEquals("slack.davidafsilva.pt", request.getTeamDomain());
    assertEquals("davidafsilva", request.getTeamIdentifier());
    assertEquals("test", request.getCommand());
    assertEquals(args.filter(s -> !s.isEmpty()).isPresent(), request.getArguments().isPresent());
    request.getArguments().ifPresent(a -> assertEquals(args.get(), a));
    assertNotNull(request.toString());
  }
}
