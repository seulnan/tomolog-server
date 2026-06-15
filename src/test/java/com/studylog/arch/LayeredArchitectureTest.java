package com.studylog.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Enforces the layering and injection rules from CLAUDE.md §3. These are gates, not suggestions: a
 * build that violates them is a failed build. Kept dependency-light so it runs in the unit phase
 * without a Spring context.
 */
class LayeredArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.studylog");
  }

  @Test
  void controllers_dependOnServicesOnly_notRepositories() {
    ArchRule rule =
        noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Repository")
            .because("controllers must delegate to services, never touch repositories (§3)")
            .allowEmptyShould(true);
    rule.check(classes);
  }

  @Test
  void domainAndService_doNotDependOnWebLayer() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage("..service..", "..domain..", "..concurrency..", "..gamification..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("jakarta.servlet..", "..controller..")
            .because("domain/service must not depend on the web layer (§3)")
            .allowEmptyShould(true);
    rule.check(classes);
  }

  @Test
  void noFieldInjection_constructorInjectionOnly() {
    ArchRule rule =
        fields()
            .should()
            .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection only — no field @Autowired (§3)")
            // Before any beans exist (early M0) there are no fields to match; don't fail
            // vacuously and wedge the loop's first gate run. Enforced once code lands.
            .allowEmptyShould(true);
    rule.check(classes);
  }

  @Test
  void noPackageCycles() {
    ArchRule rule =
        slices()
            .matching("com.studylog.(*)..")
            .should()
            .beFreeOfCycles()
            .because("no cyclic dependencies between feature packages (§3)")
            .allowEmptyShould(true);
    rule.check(classes);
  }
}
