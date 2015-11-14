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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the {@link HttpServerConfiguration} object.
 *
 * @author david
 */
public class HttpServerConfigurationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_constructor() throws Exception {
    thrown.expect(InvocationTargetException.class);
    Constructor<HttpServerConfiguration> c = HttpServerConfiguration.class.getDeclaredConstructor();
    c.setAccessible(true);
    c.newInstance();
  }

  @Test
  public void test_configuration_default() {
    final HttpServerOptions options = HttpServerConfiguration.setup(new JsonObject());
    assertNotNull(options);
    assertEquals(HttpServerConfiguration.DEFAULT_HTTP_PORT, options.getPort());
    assertEquals(HttpServerConfiguration.DEFAULT_USE_SSL, options.isSsl());
  }

  @Test
  public void test_configuration_httpPort() {
    final HttpServerOptions options = HttpServerConfiguration.setup(new JsonObject()
        .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 1234));
    assertNotNull(options);
    assertEquals(1234, options.getPort());
    assertEquals(HttpServerConfiguration.DEFAULT_USE_SSL, options.isSsl());
  }

  @Test
  public void test_configuration_sslConfig_noKeyStore_noKeyStorePass() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(String.format(HttpServerConfiguration.MISSING_PROPERTIES,
        "[KEY_STORE_FILE, KEY_STORE_PASS]"));
    HttpServerConfiguration.setup(new JsonObject()
        .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 8443)
        .put(HttpServerConfiguration.ConfigurationVariable.USE_SSL.name(), true)
    );
  }

  @Test
  public void test_configuration_sslConfig_noKeyStorePass() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(String.format(HttpServerConfiguration.MISSING_PROPERTIES,
        "[KEY_STORE_PASS]"));
    HttpServerConfiguration.setup(new JsonObject()
        .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 8443)
        .put(HttpServerConfiguration.ConfigurationVariable.USE_SSL.name(), true)
        .put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_FILE.name(), "xpto.jks")
    );
  }

  @Test
  public void test_configuration_sslConfig_noKeyStoreFile() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage(String.format(HttpServerConfiguration.MISSING_PROPERTIES,
        "[KEY_STORE_FILE]"));
    HttpServerConfiguration.setup(new JsonObject()
        .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 8443)
        .put(HttpServerConfiguration.ConfigurationVariable.USE_SSL.name(), true)
        .put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_PASS.name(), "xpto")
    );
  }

  @Test
  public void test_configuration_sslConfig() {
    final HttpServerOptions options = HttpServerConfiguration.setup(new JsonObject()
        .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 8443)
        .put(HttpServerConfiguration.ConfigurationVariable.USE_SSL.name(), true)
        .put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_FILE.name(), "file")
        .put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_PASS.name(), "xpto")
    );
    assertNotNull(options);
    assertEquals(8443, options.getPort());
    assertEquals(true, options.isSsl());
    assertEquals(HttpServerConfiguration.CIPHER_SUITES.size(),
        options.getEnabledCipherSuites().size());
    assertThat(options.getEnabledCipherSuites(), hasItems(HttpServerConfiguration.CIPHER_SUITES
        .toArray(new String[HttpServerConfiguration.CIPHER_SUITES.size()])));
    final KeyCertOptions keyCertOptions = options.getKeyCertOptions();
    assertNotNull(keyCertOptions);
    assertTrue(JksOptions.class.isInstance(keyCertOptions));
    final JksOptions jksOptions = (JksOptions) keyCertOptions;
    assertEquals("file", jksOptions.getPath());
    assertEquals("xpto", jksOptions.getPassword());
  }

  @Test
  public void test_configuration_herokuPortIntegration() throws Exception {
    // update environment variables
    final Map<String, String> originalEnv = System.getenv();
    final Map<String, String> env = getEnvWritableMap();
    try {
      env.put("PORT", "10000");

      final HttpServerOptions options = HttpServerConfiguration.setup(new JsonObject()
          .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 1234));
      assertNotNull(options);
      assertEquals(10000, options.getPort());
      assertEquals(HttpServerConfiguration.DEFAULT_USE_SSL, options.isSsl());
    } finally {
      env.clear();
      env.putAll(originalEnv);
    }
  }

  @Test
  public void test_configuration_envOverride() throws Exception {
    // update environment variables
    final Map<String, String> originalEnv = System.getenv();
    final Map<String, String> env = getEnvWritableMap();
    try {
      env.put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.environmentName(), "10000");
      env.put(HttpServerConfiguration.ConfigurationVariable.USE_SSL.environmentName(), "true");
      env.put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_FILE.environmentName(),
          "file");
      env.put(HttpServerConfiguration.ConfigurationVariable.KEY_STORE_PASS.environmentName(),
          "xpto");

      final HttpServerOptions options = HttpServerConfiguration.setup(new JsonObject()
          .put(HttpServerConfiguration.ConfigurationVariable.HTTP_PORT.name(), 1234));
      assertNotNull(options);
      assertEquals(10000, options.getPort());
      assertEquals(true, options.isSsl());
      final KeyCertOptions keyCertOptions = options.getKeyCertOptions();
      assertNotNull(keyCertOptions);
      assertTrue(JksOptions.class.isInstance(keyCertOptions));
      final JksOptions jksOptions = (JksOptions) keyCertOptions;
      assertEquals("file", jksOptions.getPath());
      assertEquals("xpto", jksOptions.getPassword());
    } finally {
      env.clear();
      env.putAll(originalEnv);
    }
  }

  @Test
  public void test_configurationVariablesEnvPrefix() {
    assertTrue(Arrays.stream(HttpServerConfiguration.ConfigurationVariable.values())
        // map to name and back to enum to avoid coverage "missing valueOf()" - stoopid
        .map(HttpServerConfiguration.ConfigurationVariable::name)
        .map(HttpServerConfiguration.ConfigurationVariable::valueOf)
        .map(HttpServerConfiguration.ConfigurationVariable::environmentName)
        .allMatch(env -> env.startsWith("SLACKER_")));
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> getEnvWritableMap() throws Exception {
    final Class[] classes = Collections.class.getDeclaredClasses();
    final Map<String, String> env = System.getenv();
    for (final Class cl : classes) {
      if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
        final Field field = cl.getDeclaredField("m");
        field.setAccessible(true);
        return (Map<String, String>) field.get(env);
      }
    }

    throw new AssertionError("unable to read ENV map");
  }
}
