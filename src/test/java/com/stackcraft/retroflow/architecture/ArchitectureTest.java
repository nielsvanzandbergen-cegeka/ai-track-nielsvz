package com.stackcraft.retroflow.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.stackcraft.retroflow", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersMustNotUseRepositories = ArchitectureRules.CONTROLLERS_MUST_NOT_USE_REPOSITORIES;

    @ArchTest
    static final ArchRule controllersMustNotReturnEntities = ArchitectureRules.CONTROLLERS_MUST_NOT_RETURN_ENTITIES;

    @ArchTest
    static final ArchRule servicesMustNotDependOnControllers = ArchitectureRules.SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS;

    @ArchTest
    static final ArchRule entitiesMustNotDependOnUpperLayers = ArchitectureRules.ENTITIES_MUST_NOT_DEPEND_ON_UPPER_LAYERS;
}
