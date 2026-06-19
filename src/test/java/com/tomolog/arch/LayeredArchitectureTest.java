package com.tomolog.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Enforces the 4-layer architecture (LEDGER-007): presentation → application → domain, with
 * infrastructure as the outer adapter layer depending inward only. domain depends on no other
 * layer. These are gates, not suggestions — a violating build fails (CLAUDE.md §3).
 */
class LayeredArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.tomolog");
  }

  @Test
  void layersRespectTheirDependencyDirection() {
    ArchRule rule =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Presentation")
            .definedBy("com.tomolog.presentation..")
            .layer("Application")
            .definedBy("com.tomolog.application..")
            .layer("Domain")
            .definedBy("com.tomolog.domain..")
            .layer("Infrastructure")
            .definedBy("com.tomolog.infrastructure..")
            // Who may be accessed by whom:
            .whereLayer("Presentation")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Presentation", "Infrastructure")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Presentation", "Application", "Infrastructure")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .because("presentation→application→domain; infrastructure is an inward-only adapter");
    rule.check(classes);
  }

  @Test
  void domain_dependsOnNoOtherLayer() {
    ArchRule rule =
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that()
            .resideInAPackage("com.tomolog.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.tomolog.application..",
                "com.tomolog.presentation..",
                "com.tomolog.infrastructure..")
            .because("the domain layer must not depend on any outer layer");
    rule.check(classes);
  }

  @Test
  void noFieldInjection_constructorInjectionOnly() {
    ArchRule rule =
        fields()
            .should()
            .notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("constructor injection only — no field @Autowired (§3)");
    rule.check(classes);
  }

  @Test
  void noLayerCycles() {
    ArchRule rule =
        slices()
            .matching("com.tomolog.(*)..")
            .should()
            .beFreeOfCycles()
            .because("no cyclic dependencies between layers (§3)");
    rule.check(classes);
  }
}
