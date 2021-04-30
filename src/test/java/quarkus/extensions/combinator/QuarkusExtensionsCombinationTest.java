package quarkus.extensions.combinator;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import quarkus.extensions.combinator.extensions.ExtensionsProvider;
import quarkus.extensions.combinator.extensions.RecordFailedScenariosTestQuarkusCombinationExtension;
import quarkus.extensions.combinator.maven.MavenGenerator;
import quarkus.extensions.combinator.maven.MavenProject;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(RecordFailedScenariosTestQuarkusCombinationExtension.class)
class QuarkusExtensionsCombinationTest {

    @ParameterizedTest(name = "#{index} - With {0}")
    @ArgumentsSource(ExtensionsProvider.class)
    void testExtensions(Set<String> extensions) {
        MavenProject project = MavenGenerator.withExtensions(extensions)
                .generate()
                .compile()
                .verify();

        if (Configuration.VERIFY_DEV_MODE.getAsBoolean()) {
            project.devMode();
            assertApplicationInDevMode(project);
        }

        if (Configuration.VERIFY_NATIVE_MODE.getAsBoolean()) {
            project.nativeMode();
        }

        project.delete();
    }

    private void assertApplicationInDevMode(MavenProject project) {
        try {
            assertTrue(project.getCurrentProcess().isAlive(), "Process is not alive!");

            await().pollInterval(1, TimeUnit.SECONDS)
                    .atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                String output = FileUtils.readFileToString(project.getOutput(), Charset.defaultCharset());
                assertTrue(output.contains("Profile dev activated"), "No found dev profile. Output: " + output);
                assertTrue(output.contains("Installed features"), "No found features. Output: " + output);
            });

            project.getCurrentProcess().destroyForcibly();
        } catch (Exception ignored) {

        }
    }
}
