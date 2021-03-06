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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;

/**
 * <p>Initializes the http server options from the available configuration.
 * Note that environmental variables may overwrite the configuration specified via the regular
 * json file. These env. variables are prefixed with <strong>SLACKER_*</strong>.</p>
 *
 * The supported SLACKER environment variables are:
 * <table summary="SLACK Variables">
 * <tr>
 * <td><strong>Variable</strong></td>
 * <td><strong>Description</strong></td>
 * </tr>
 * <tr>
 * <td>SLACKER_HTTP_PORT</td>
 * <td>The port for underlying HTTP server</td>
 * </tr>
 * <tr>
 * <td>PORT</td>
 * <td>The port for underlying HTTP server - required for heroku deployments</td>
 * </tr>
 * <tr>
 * <td>SLACKER_USE_SSL</td>
 * <td>Enables HTTPS instead of HTTP for underlying server</td>
 * </tr>
 * <tr>
 * <td>SLACKER_KEYSTORE_FILE</td>
 * <td>The keystore file location for server private-public key pair (required when SSL is
 * enabled)</td>
 * </tr>
 * <tr>
 * <td>SLACKER_KEYSTORE_PASS</td>
 * <td>The password for the specified keystore</td>
 * </tr>
 * </table>
 *
 * @author david
 */
final class HttpServerConfiguration {

  // the default value for HTTP port
  static final int DEFAULT_HTTP_PORT = 8080;
  // the default value for the use SSL flag
  static final boolean DEFAULT_USE_SSL = false;
  // the idle timeout for the connection (in seconds)
  static final int IDLE_TIMEOUT = 60;

  // the cipher suites to enable for SSL
  static final Collection<String> CIPHER_SUITES = Collections.unmodifiableList(Arrays.asList(
      "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
      "TLS_RSA_WITH_AES_128_CBC_SHA"
  ));

  // invalid configuration message
  static final String MISSING_PROPERTIES = "some required properties are missing: %s";

  // private constructor
  private HttpServerConfiguration() {
    throw new UnsupportedOperationException("no no no");
  }

  /**
   * Sets up the http server configuration based on the available environment variables (SLACK_*)
   * and current configuration via the json configuration file.
   *
   * @param config the current configuration
   * @return the configured server options
   */
  static HttpServerOptions setup(final JsonObject config) {
    // evaluate the environment variables
    evaluateEnvironmentVariables(config);

    // create the http server options
    final HttpServerOptions options = new HttpServerOptions()
        .setIdleTimeout(IDLE_TIMEOUT)
        .setPort(config.getInteger(ConfigurationVariable.HTTP_PORT.name(), DEFAULT_HTTP_PORT))
        .setSsl(config.getBoolean(ConfigurationVariable.USE_SSL.name(), DEFAULT_USE_SSL));

    // setup the required SSL parameters
    if (options.isSsl()) {
      // validate the configuration
      validateOptions(config, ConfigurationVariable.KEY_STORE_FILE,
          ConfigurationVariable.KEY_STORE_PASS);

      // add the enabled cipher suites
      CIPHER_SUITES.stream().forEach(options::addEnabledCipherSuite);

      // set the both keystore location and keystore password
      options.setKeyStoreOptions(new JksOptions()
          .setPath(config.getString(ConfigurationVariable.KEY_STORE_FILE.name()))
          .setPassword(config.getString(ConfigurationVariable.KEY_STORE_PASS.name())));
    }

    return options;
  }

  /**
   * Validates the options for runtime and if there are missing options, fails the start of this
   * verticle.
   *
   * @param config   the current configuration
   * @param required the required properties
   * @throws IllegalStateException if the provided configuration is invalid
   */
  private static void validateOptions(final JsonObject config,
      final ConfigurationVariable... required) {
    final String[] missing = Arrays.stream(required)
        .map(ConfigurationVariable::name)
        .filter(prop -> !config.containsKey(prop))
        .toArray(String[]::new);
    if (missing.length > 0) {
      throw new IllegalStateException(String.format(MISSING_PROPERTIES, Arrays.toString(missing)));
    }
  }

  /**
   * Reads and overwrites the necessary configuration based on the available environment variables
   *
   * @param config the current configuration
   */
  private static void evaluateEnvironmentVariables(final JsonObject config) {
    // iterate over each configuration variable and read the environment variables in order
    // to overwrite the necessary configurations
    Arrays.stream(ConfigurationVariable.values()).forEach(variable ->
        Optional.ofNullable(System.getenv(variable.environmentName()))
            .ifPresent(p -> config.put(variable.name(), variable.transformValue(p))));

    // special support for Heroku deployment
    Optional.ofNullable(System.getenv("PORT"))
        .map(ConfigurationVariable.HTTP_PORT::transformValue)
        .ifPresent(p -> config.put(ConfigurationVariable.HTTP_PORT.name(), p));
  }

  // the enumeration of the support configuration variables
  enum ConfigurationVariable {
    HTTP_PORT(Integer::valueOf),
    USE_SSL(Boolean::valueOf),
    KEY_STORE_FILE(Objects::toString),
    KEY_STORE_PASS(Objects::toString);

    // the value transformer
    private final Function<String, Object> transformer;

    /**
     * Creates the configuration variable with the specified value transformer
     *
     * @param transformer the value transformer
     */
    ConfigurationVariable(final Function<String, Object> transformer) {
      this.transformer = transformer;
    }

    /**
     * Returns the name of the environment variable that links to this configuration
     *
     * @return the environment variable name
     */
    String environmentName() {
      return "SLACKER_" + name();
    }

    /**
     * Transformer the read property value into a configuration-typed value
     *
     * @param propertyValue the read property value
     * @return the transformed property value
     */
    Object transformValue(final String propertyValue) {
      return transformer.apply(propertyValue);
    }
  }
}
