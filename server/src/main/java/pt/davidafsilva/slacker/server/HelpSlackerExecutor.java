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

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.vertx.core.Future;
import pt.davidafsilva.slacker.api.AbstractSlackerExecutor;
import pt.davidafsilva.slacker.api.SlackerRequest;
import pt.davidafsilva.slacker.api.SlackerResponse;

/**
 * <p>
 * This help executor is part of the slacker server core. It serves the help message for each
 * available and registered executor.
 * </p>
 *
 * Available commands:
 * <pre>
 * !help - shows the help message for each executor
 * !help &lt;command&gt; - shows the help message for the particular executor
 * </pre>
 *
 * @author david
 */
final class HelpSlackerExecutor extends AbstractSlackerExecutor {

  // the help message formatter for each executor
  private static final String HELP_MSG_FORMAT = "%-10s %s";

  // the default filter predicate - which allows every entry
  private static final Predicate<ExecutorRegistry.ExecutorEntry> DEFAULT_PREDICATE = e -> true;

  // the filter predicate with executor identifier support
  private static final Function<String, Predicate<ExecutorRegistry.ExecutorEntry>> EXEC_PREDICATE
      = s -> e -> e.getId().equalsIgnoreCase(s);

  // the executor identifier
  static final String IDENTIFIER = "help";

  // the executor description
  static final String DESCRIPTION = "shows the help message for all the available commands";

  // the message to display when no executors are available
  // -> should not happen though, as the help executor should be always available
  static final String NO_EXECUTORS_MSG = "There are no commands available at the moment.";

  // the message to display when no executors are available with argument filtering
  static final String NO_EXECUTORS_MATCH = "No help available for the command '%s'";

  // the executors supplier
  private final Supplier<Stream<ExecutorRegistry.ExecutorEntry>> executorsSupplier;

  /**
   * Constructs the help slacker executor with the given executor supplier which will supply
   * the current registered/available executors at a given time.
   *
   * @param executorsSupplier the executor registry instance
   */
  HelpSlackerExecutor(final Supplier<Stream<ExecutorRegistry.ExecutorEntry>> executorsSupplier) {
    this.executorsSupplier = executorsSupplier;
  }

  @Override
  public String identifier() {
    return IDENTIFIER;
  }

  @Override
  public String description() {
    return DESCRIPTION;
  }

  @Override
  public String version() {
    return getClass().getPackage().getImplementationVersion();
  }

  @Override
  public void execute(final SlackerRequest request, final Future<SlackerResponse> result) {
    // create the filter for the executors
    final Predicate<ExecutorRegistry.ExecutorEntry> filter = request.getArguments()
        .map(EXEC_PREDICATE::apply)
        .orElse(DEFAULT_PREDICATE);

    // build the help message
    final String helpMessage = executorsSupplier.get()
        .filter(filter)
        .map(e -> String.format(HELP_MSG_FORMAT, e.getId(), e.getDescription()))
        .map(StringBuilder::new)
        .reduce((sb1, sb2) -> sb1.append(System.lineSeparator()).append(sb2))
        .map(StringBuilder::toString)
        .orElse(filter == DEFAULT_PREDICATE ? NO_EXECUTORS_MSG
            : String.format(NO_EXECUTORS_MATCH, request.getArguments().get()));

    // post the result
    result.complete(success(helpMessage));
  }
}
