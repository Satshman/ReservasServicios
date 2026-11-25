package co.reservas.aceptacion;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Pruebas de aceptación del backend: ejecuta con Cucumber los escenarios Gherkin de {@code src/test/resources/features}.
 * Surefire la incluye en {@code ./mvnw verify} por terminar en {@code Test}; para correr solo estas:
 * {@code ./mvnw test -Dtest=AceptacionTest}.
 */
@Suite
@SuiteDisplayName("Aceptación del backend (Gherkin)")
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "co.reservas.aceptacion")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber/aceptacion.html, json:target/cucumber/aceptacion.json")
class AceptacionTest {
}
