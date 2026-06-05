package com.demo.skills;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "com.demo.skills", importOptions = DoNotIncludeTests.class)
class ArchitectureRulesTest {

  @ArchTest
  static final ArchRule applicationClassesStayInBasePackage =
      classes().should().resideInAPackage("com.demo.skills..");

  @ArchTest
  static final ArchRule fieldInjectionIsNotAllowed =
      noFields().should().beAnnotatedWith(Autowired.class).allowEmptyShould(true);

  @ArchTest
  static final ArchRule repositoriesDoNotDependOnServicesOrControllers =
      noClasses()
          .that()
          .areAnnotatedWith(Repository.class)
          .should()
          .dependOnClassesThat()
          .areAnnotatedWith(Service.class)
          .orShould()
          .dependOnClassesThat()
          .areAnnotatedWith(RestController.class)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule servicesDoNotDependOnControllers =
      noClasses()
          .that()
          .areAnnotatedWith(Service.class)
          .should()
          .dependOnClassesThat()
          .areAnnotatedWith(RestController.class)
          .allowEmptyShould(true);
}
