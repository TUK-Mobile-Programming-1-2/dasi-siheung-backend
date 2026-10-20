package com.dasisiheung.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/**
 * 계층형 구조 규칙을 테스트로 강제한다. 규칙을 어기는 코드는 CI 에서 실패한다.
 */
@AnalyzeClasses(packages = "com.dasisiheung", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule layersDependOnlyDownward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

    @ArchTest
    static final ArchRule entitiesDoNotDependOnUpperLayers = noClasses()
            .that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..dto..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule restControllersLiveInControllerPackage = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..")
            .andShould().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule packagesAreFreeOfCycles = slices()
            .matching("com.dasisiheung.(*)..")
            .should().beFreeOfCycles();
}
