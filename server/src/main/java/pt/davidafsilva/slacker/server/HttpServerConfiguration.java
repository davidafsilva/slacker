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
public final class HttpServerConfiguration {

  // private constructor
  private HttpServerConfiguration() {}

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
        .setPort(config.getInteger(ConfigurationVariable.HTTP_PORT.name(), 8080))
        .setSsl(config.getBoolean(ConfigurationVariable.USE_SSL.name(), false));

    // setup the required SSL parameters
    if (options.isSsl()) {
      // validate the configuration
      validateOptions(config, ConfigurationVariable.KEY_STORE_FILE,
          ConfigurationVariable.KEY_STORE_PASS);

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
      throw new IllegalStateException(
          "some required properties are missing: " + Arrays.toString(missing));
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
  private enum ConfigurationVariable {
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
