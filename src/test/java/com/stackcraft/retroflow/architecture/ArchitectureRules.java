package com.stackcraft.retroflow.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture rules shared by {@link ArchitectureTest} (real code) and
 * {@link ArchitectureRulesCanFailTest} (deliberately violating fixtures).
 */
final class ArchitectureRules {

    static final ArchRule CONTROLLERS_MUST_NOT_USE_REPOSITORIES = noClasses()
        .that().areAnnotatedWith(RestController.class)
        .should().dependOnClassesThat().resideInAPackage("..repository..")
        .because("controllers must go through the service layer, where the business rules live");

    static final ArchRule CONTROLLERS_MUST_NOT_RETURN_ENTITIES = methods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
        .and().arePublic()
        .should(notExposeEntitiesInReturnType())
        .because("JPA entities must be mapped to DTOs before leaving the API");

    static final ArchRule SERVICES_MUST_NOT_DEPEND_ON_CONTROLLERS = noClasses()
        .that().areAnnotatedWith(Service.class)
        .should().dependOnClassesThat().areAnnotatedWith(RestController.class)
        .orShould().dependOnClassesThat().resideInAPackage("..controller..")
        .because("services must not know about the HTTP layer");

    static final ArchRule ENTITIES_MUST_NOT_DEPEND_ON_UPPER_LAYERS = noClasses()
        .that().areAnnotatedWith(Entity.class)
        .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..dto..")
        .because("the domain model is the innermost layer");

    private ArchitectureRules() {
    }

    private static ArchCondition<JavaMethod> notExposeEntitiesInReturnType() {
        DescribedPredicate<JavaClass> entity = DescribedPredicate.describe(
            "JPA entity", type -> type.isAnnotatedWith(Entity.class));
        return new ArchCondition<>("not return a JPA entity, also not wrapped in a generic type") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                method.getReturnType().getAllInvolvedRawTypes().stream()
                    .filter(entity)
                    .forEach(type -> events.add(SimpleConditionEvent.violated(method,
                        method.getFullName() + " returns entity " + type.getName())));
            }
        };
    }
}
