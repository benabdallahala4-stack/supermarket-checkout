package com.example.supermarket.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.example.supermarket",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule domainUsesOnlyJdkAndDomain =
      classes()
          .that()
          .resideInAnyPackage("..catalog.domain..", "..checkout.domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java..",
              "com.example.supermarket.catalog.domain..",
              "com.example.supermarket.checkout.domain..");

  @ArchTest
  static final ArchRule catalogDoesNotDependOnCheckout =
      noClasses()
          .that()
          .resideInAPackage("..catalog..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..checkout..");

  @ArchTest
  static final ArchRule applicationDoesNotDependOnAdapters =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..persistence..", "..api..", "..generated..");

  @ArchTest
  static final ArchRule jdbcIsConfinedToPersistence =
      noClasses()
          .that()
          .resideOutsideOfPackage("..persistence..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.jdbc..", "java.sql..", "javax.sql..");
}
