package com.demo.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectConventionsTest {

  private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
  private static final Path MAIN_JAVA = Path.of("src/main/java");
  private static final Path OPENAPI_SPEC = Path.of("openapi/openapi.yaml");
  private static final Path FLYWAY_MIGRATIONS = MAIN_RESOURCES.resolve("db/migration");
  private static final Pattern FLYWAY_MIGRATION_NAME = Pattern.compile("V\\d+__[a-z0-9_]+\\.sql");

  @Test
  @DisplayName("uses application.yaml instead of application.properties")
  void usesApplicationYamlInsteadOfProperties() {
    assertThat(MAIN_RESOURCES.resolve("application.yaml")).exists();
    assertThat(MAIN_RESOURCES.resolve("application.properties")).doesNotExist();
  }

  @Test
  @DisplayName("defines the REST API contract before generated interfaces")
  void definesRestApiContractBeforeGeneratedInterfaces() {
    assertThat(OPENAPI_SPEC).exists();
  }

  @Test
  @DisplayName("names Flyway migrations with sequential version prefix")
  void namesFlywayMigrationsWithSequentialVersionPrefix() throws IOException {
    if (!Files.exists(FLYWAY_MIGRATIONS)) {
      return;
    }

    try (val files = Files.list(FLYWAY_MIGRATIONS)) {
      assertThat(
              files
                  .filter(Files::isRegularFile)
                  .map(Path::getFileName)
                  .map(Path::toString)
                  .toList())
          .allSatisfy(fileName -> assertThat(fileName).matches(FLYWAY_MIGRATION_NAME));
    }
  }

  @Test
  @DisplayName("adds package-info.java with NullMarked to every production package")
  void addsPackageInfoWithNullMarkedToEveryProductionPackage() throws IOException {
    try (val files = Files.walk(MAIN_JAVA)) {
      val packages =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .map(Path::getParent)
              .distinct()
              .toList();

      assertThat(packages)
          .allSatisfy(
              packagePath -> {
                val packageInfo = packagePath.resolve("package-info.java");
                assertThat(packageInfo).exists();
                assertThat(packageInfo).content().contains("@NullMarked");
              });
    }
  }

  @Test
  @DisplayName("does not use source-retained Lombok annotations banned by project conventions")
  void doesNotUseBannedLombokAnnotations() throws IOException {
    try (val files = Files.walk(MAIN_JAVA)) {
      val sourceFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .toList();

      assertThat(sourceFiles)
          .allSatisfy(
              sourceFile -> {
                assertThat(sourceFile).content().doesNotContain("@Data");
                assertThat(sourceFile).content().doesNotContain("@Setter");
              });
    }
  }

  @Test
  @DisplayName("does not log raw account numbers from production code")
  void doesNotLogRawAccountNumbers() throws IOException {
    try (val files = Files.walk(MAIN_JAVA)) {
      val sourceFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .toList();

      assertThat(sourceFiles)
          .allSatisfy(
              sourceFile ->
                  assertThat(sourceFile)
                      .content()
                      .doesNotContain(".addKeyValue(\"accountNumber\""));
    }
  }

  @Test
  @DisplayName("does not log raw nicknames from production code")
  void doesNotLogRawNicknames() throws IOException {
    try (val files = Files.walk(MAIN_JAVA)) {
      val sourceFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .toList();

      assertThat(sourceFiles)
          .allSatisfy(
              sourceFile ->
                  assertThat(sourceFile)
                      .content()
                      .doesNotContain(".addKeyValue(\"nickname\""));
    }
  }

  @Test
  @DisplayName("does not expose customer IDs as ProblemDetail properties")
  void doesNotExposeCustomerIdsAsProblemDetailProperties() throws IOException {
    try (val files = Files.walk(MAIN_JAVA)) {
      val sourceFiles =
          files
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .toList();

      assertThat(sourceFiles)
          .allSatisfy(
              sourceFile ->
                  assertThat(sourceFile)
                      .content()
                      .doesNotContain(".setProperty(\"customerId\""));
    }
  }
}
