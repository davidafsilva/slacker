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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * The registry where incoming register executor requests are processed (validated a registered)
 * and lookups can be issued for a particular executor.
 *
 * @author david
 */
final class ExecutorRegistry {

  // the default description
  static final String DEFAULT_DESCRIPTION = "Description not available";

  // the message to be included when an invalid request is received
  static final String INVALID_REQUEST_MSG = "missing required fields";

  // the address format
  private static final String ADDRESS_FORMAT = "%032x.slacker-executor";

  // the secure random generator for the address assignment
  private final SecureRandom random = new SecureRandom();

  // the in-memory mapping of executors by his identifier
  private final Map<String, ExecutorEntry> executors = new HashMap<>();

  /**
   * Tries to register the executor that is defined within the given JSON request object.
   * The registering of the executor will succeed if, and only if one of the two rules is
   * fulfilled:
   * <ol>
   * <li>there are not executors registered with the same identifier</li>
   * <li>if there are executors registers, they must share the same version number</li>
   * </ol>
   *
   * @param request        the request with executor information
   * @param successHandler the handler to be handle with the assigned address if the register
   *                       succeeds
   * @param errorHandler   the error handler that shall be called whenever the register fails
   */
  void register(final JsonObject request, final Handler<String> successHandler,
      final Handler<String> errorHandler) {
    // validate the request
    if (!isRequestValid(request)) {
      errorHandler.handle(INVALID_REQUEST_MSG);
      return;
    }

    // extract the data from the request
    final String id = request.getString("i");
    final String version = request.getString("v");
    final String description = request.getString("d", DEFAULT_DESCRIPTION);

    // check if there's an executor, if so validate the version
    ExecutorEntry executorEntry = executors.get(id);
    if (executorEntry != null) {
      //TODO: validate the version
    } else {
      // generate address
      final String address = randomAddress();

      // add the executor
      executors.put(id, executorEntry = new ExecutorEntry(id, version, description, address));

      //TODO: monitor instances - at least one must be up! otherwise remove it from the map
    }

    // report the success
    successHandler.handle(executorEntry.address);
  }

  /**
   * Returns a random address to be assigned to a newly registered executor.
   *
   * The address has the following format:
   * <ul>
   * <li>&lt;16 random characters&gt;.slacker-executor</li>
   * </ul>
   *
   * @return the generated address
   */
  private String randomAddress() {
    return String.format(ADDRESS_FORMAT, new BigInteger(128, random));
  }

  /**
   * Validates the received executor registration request
   *
   * @param request the executor registration request
   * @return {@code true} if the request is valid, {@code false} otherwise.
   */
  private boolean isRequestValid(final JsonObject request) {
    return Arrays.asList("i", "v").stream()
        .map(request::getString)
        .noneMatch(v -> v == null || v.isEmpty());
  }

  /**
   * Looks up a previously registered executor by his identifier
   *
   * @param id             the executor identifier to lookup
   * @param addressHandler the handler that shall be called with the executor reachable address
   * @param errorHandler   the handler that shall be called if the executor does not exist and/or
   *                       its not registered
   */
  void lookup(final String id, final Handler<String> addressHandler,
      final Handler<Void> errorHandler) {
    // get the executor entry
    final ExecutorEntry executorEntry = executors.get(id);
    if (executorEntry != null) {
      addressHandler.handle(executorEntry.address);
    } else {
      errorHandler.handle(null);
    }
  }

  // the executor map entry utility class
  static final class ExecutorEntry {

    // properties
    private final String id;
    private final String version;
    private final String description;
    private final String address;

    /**
     * Constructs the entry with the executor configuration
     *
     * @param id          the executor identifier
     * @param version     the executor version
     * @param description the executor description
     * @param address     the assigned address to the executor
     */
    private ExecutorEntry(final String id, final String version, final String description,
        final String address) {
      this.id = id;
      this.version = version;
      this.description = description;
      this.address = address;
    }
  }
}