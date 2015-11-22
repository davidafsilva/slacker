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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.vertx.core.Future;
import pt.davidafsilva.slacker.api.ResultCode;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerRequestBuilder;
import pt.davidafsilva.slacker.api.SlackerResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link HelpSlackerExecutor} object.
 *
 * @author david
 */
@RunWith(MockitoJUnitRunner.class)
public class HelpSlackerExecutorTest {

  // the request used for the test execution
  private final SlackerRequestBuilder BASE_REQUEST = new SlackerRequestBuilder()
      .timestamp(Instant.now())
      .channelId("12345")
      .channelName("#dope")
      .userId("6789")
      .userName("david")
      .teamDomain("slack.davidafsilva.pt")
      .teamIdentifier("davidafsilva")
      .command("help");

  @Mock
  private Future<SlackerResponse> future;

  @Captor
  private ArgumentCaptor<SlackerResponse> captor;

  @Test
  public void test_staticDefinitions() {
    final HelpSlackerExecutor executor = new HelpSlackerExecutor(Stream::empty);
    assertEquals(HelpSlackerExecutor.IDENTIFIER, executor.identifier());
    assertEquals(HelpSlackerExecutor.DESCRIPTION, executor.description());
    // version is not available at test phase
    assertNull(executor.version());
  }

  @Test
  public void test_request_noExecutors() {
    final String response = issueRequest(BASE_REQUEST.build(), Stream::empty);
    assertEquals(HelpSlackerExecutor.NO_EXECUTORS_MSG, response);
  }

  @Test
  public void test_request_withExecutors_noArgs() {
    final String response = issueRequest(BASE_REQUEST.build(), () -> Stream.of(
        entry("woop", "woop woop!"), entry("bla", "bleh")
    ));
    assertEquals("woop       woop woop!" + System.lineSeparator() + "bla        bleh", response);
  }

  @Test
  public void test_request_withExecutors_withArgs() {
    final String response = issueRequest(BASE_REQUEST.args("woop").build(), () -> Stream.of(
        entry("woop", "woop woop!"), entry("bla", "bleh")
    ));
    assertEquals("woop       woop woop!", response);
  }

  @Test
  public void test_request_withExecutors_withInvalidArgs() {
    final String response = issueRequest(BASE_REQUEST.args("woopx").build(), () -> Stream.of(
        entry("woop", "woop woop!"), entry("bla", "bleh")
    ));
    assertEquals(String.format(HelpSlackerExecutor.NO_EXECUTORS_MATCH, "woopx"), response);
  }

  private String issueRequest(final SlackerRequest request,
      final Supplier<Stream<ExecutorRegistry.ExecutorEntry>> supplier) {
    final HelpSlackerExecutor executor = new HelpSlackerExecutor(supplier);
    executor.execute(request, future);
    verify(future, never()).fail(anyString());
    verify(future, never()).fail(any(Throwable.class));
    verify(future, never()).complete();
    verify(future, times(1)).complete(captor.capture());
    final SlackerResponse response = captor.getValue();
    assertNotNull(response);
    assertEquals(ResultCode.OK, response.getCode());
    assertTrue(response.getResponse().isPresent());
    return response.getResponse().get();
  }

  private ExecutorRegistry.ExecutorEntry entry(final String id, final String description) {
    final ExecutorRegistry.ExecutorEntry entry = mock(ExecutorRegistry.ExecutorEntry.class);
    when(entry.getId()).thenReturn(id);
    when(entry.getDescription()).thenReturn(description);
    return entry;
  }
}
