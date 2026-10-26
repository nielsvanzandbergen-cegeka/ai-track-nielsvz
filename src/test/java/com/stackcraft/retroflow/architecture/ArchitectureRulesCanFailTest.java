package com.stackcraft.retroflow.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the rules in {@link ArchitectureRules} are not vacuous: each rule must reject the
 * deliberately violating fixtures in {@code com.stackcraft.archfixtures}.
 */
class ArchitectureRulesCanFailTest {

    private static final String FIXTURE_PACKAGE = "com.stackcraft.archfixtures";
    private static final String PRODUCTION_PACKAGE = "com.stackcraft.retroflow";

    private static JavaClasses fixtures;
    private static JavaClasses production;

    @BeforeAll
    static void importClasses() {
        fixtures = new ClassFileImporter().importPackages(FIXTURE_PACKAGE);
        production = new ClassFileImporter()
            .withImportOption(new com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests())
            .importPackages(PRODUCTION_PACKAGE);
    }

    @Test
    void controllersMustNotUseRepositories_whenControllerInjectsRepository_fails() {
        // Arrange
        ArchRule rule = ArchitectureRules.CONTROLLERS_MUST_NOT_USE_REPOSITORIES;

        // Act & Assert
        assertThatThrownBy(() -> rule.check(fixtures))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("RepositoryLeakingController")
            .hasMessageContaining("TeamRepository");
    }

    @Test
    void controllersMustNotReturnEntities_whenControllerReturnsEntity_failsForPlainAndWrappedReturnTypes() {
        // Arrange
        ArchRule rule = ArchitectureRules.CONTROLLERS_MUST_NOT_RETURN_ENTITIES;

        // Act & Assert
        assertThatThrownBy(() -> rule.check(fixtures))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("RepositoryLeakingController.getTeam(java.lang.Long) returns entity")
            .hasMessageContaining("RepositoryLeakingController.listTeams() returns entity");
    }

    @Test
    void servicesMustNotDependOnControllers_whenServiceCallsController_fails() {
        // Arrange
        ArchRule rule = ArchitectureRules.SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS;

        // Act & Assert
        assertThatThrownBy(() -> rule.check(fixtures))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("ControllerCallingService")
            .hasMessageContaining("TeamController");
    }

    @Test
    void entitiesMustNotDependOnUpperLayers_whenEntityUsesDto_fails() {
        // Arrange
        ArchRule rule = ArchitectureRules.ENTITIES_MUST_NOT_DEPEND_ON_UPPER_LAYERS;

        // Act & Assert
        assertThatThrownBy(() -> rule.check(fixtures))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("DtoAwareEntity")
            .hasMessageContaining("TeamResponse");
    }

    @Test
    void allRules_whenCheckedAgainstProductionCode_pass() {
        // Act & Assert
        assertThatCode(() -> ArchitectureRules.CONTROLLERS_MUST_NOT_USE_REPOSITORIES.check(production))
            .doesNotThrowAnyException();
        assertThatCode(() -> ArchitectureRules.CONTROLLERS_MUST_NOT_RETURN_ENTITIES.check(production))
            .doesNotThrowAnyException();
        assertThatCode(() -> ArchitectureRules.SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS.check(production))
            .doesNotThrowAnyException();
        assertThatCode(() -> ArchitectureRules.ENTITIES_MUST_NOT_DEPEND_ON_UPPER_LAYERS.check(production))
            .doesNotThrowAnyException();
    }
}
