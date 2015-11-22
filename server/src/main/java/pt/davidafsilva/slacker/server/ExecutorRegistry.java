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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The registry where incoming register executor requests are processed (validated a registered)
 * and lookups can be issued for a particular executor.
 *
 * @author david
 */
final class ExecutorRegistry {

  // the logger
  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorRegistry.class);

  // the default description
  static final String DEFAULT_DESCRIPTION = "Description not available";

  // the message to be included when an invalid request is received
  static final String INVALID_REQUEST_MSG = "missing required fields";

  // the message to be included when an invalid version is received
  static final String INCOMPATIBLE_VERSION_FORMAT = "incompatible version, found %s, expected %s " +
      "or greater";

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
    final Version version = Version.valueOf(request.getString("v"));
    final String description = request.getString("d", DEFAULT_DESCRIPTION);

    // check if there's an executor, if so validate the version
    ExecutorEntry executorEntry = executors.get(id);
    if (executorEntry != null) {
      // validate the version
      if (version.lessThan(executorEntry.getVersion())) {
        // incompatible version
        errorHandler.handle(String.format(INCOMPATIBLE_VERSION_FORMAT, version,
            executorEntry.getVersion()));
        return;
      }
    } else {
      // generate address
      final String address = randomAddress();

      // add the executor
      executors.put(id, executorEntry = new ExecutorEntry(id, version, description, address));
    }

    //TODO: monitor instances
    // - at least one must be up! otherwise remove it from the map
    // - keep the "lower" version of them all

    // report the success
    successHandler.handle(executorEntry.address);
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

  /**
   * Returns a stream with the current registered/available executors
   *
   * @return the registered executor
   */
  Stream<ExecutorEntry> executors() {
    return executors.values().stream();
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
    // validate the identifier and the version
    return basicValidation(request, "i") &&
        basicValidation(request, "v") &&
        validateVersionFormat(request.getString("v"));
  }

  /**
   * Performs a basic validation of the specified field at the executor registry request.
   * This method only validates if the field is present and it's not empty.
   *
   * @param request the executor registry request.
   * @param field   the field to be validated
   * @return {@code true} if the field is valid according the above rules, {@code false} otherwise.
   */
  private boolean basicValidation(final JsonObject request, final String field) {
    final String value = request.getString(field);
    return value != null && !value.isEmpty();
  }

  /**
   * Validates the specified version string against the Semantic Versioning Specification v2, which
   * is the required format for the executors version.
   *
   * @param version the version string
   * @return {@code true} if the version is according to the SemVer specification, {@code false}
   * otherwise.
   */
  private boolean validateVersionFormat(final String version) {
    boolean valid = false;
    try {
      Version.valueOf(version);
      valid = true;
    } catch (final ParseException e) {
      LOGGER.info("invalid version received: {}", e, version);
    }
    return valid;
  }

  // the executor map entry utility class
  static class ExecutorEntry {

    // properties
    private final String id;
    private final Version version;
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
    private ExecutorEntry(final String id, final Version version, final String description,
        final String address) {
      this.id = id;
      this.version = version;
      this.description = description;
      this.address = address;
    }

    /**
     * Returns the executor command identifier
     *
     * @return the executor command identifier
     */
    String getId() {
      return id;
    }

    /**
     * Returns the registered version of the executor
     *
     * @return the executor version
     */
    Version getVersion() {
      return version;
    }

    /**
     * Returns the description for the executor
     *
     * @return the executor description
     */
    String getDescription() {
      return description;
    }
  }
}
