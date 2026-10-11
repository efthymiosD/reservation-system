package com.decoupledx.reservation.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.decoupledx.reservation.shared.DomainEventListener;
import com.decoupledx.reservation.shared.OnDomainEvent;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.events.ApplicationModuleListener;

class DomainPurityTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.reservation");

    // package-info classes carry only structural metadata (e.g. Modulith named interfaces)
    // and contain no domain logic, so they are excluded from the purity rules below.
    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES = DescribedPredicate
            .describe("domain classes other than package-info", candidate ->
                    candidate.getPackageName().contains(".domain")
                            && !candidate.getSimpleName().equals("package-info")
                            && !candidate.getSimpleName().contains("Config"));

    @Test
    void domainPackagesDoNotDependOnSpringOrPersistenceFrameworks() {
        ArchRule rule = noClasses()
                .that(DOMAIN_CLASSES)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..");
        rule.check(CLASSES);
    }

    @Test
    void domainPackagesDoNotDependOnWebOrSecurityFrameworks() {
        ArchRule rule = noClasses()
                .that(DOMAIN_CLASSES)
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..", "org.springframework.security..");
        rule.check(CLASSES);
    }

    // ADR-0004: the platform listener annotation is an adapter concern — the
    // domain communicates events only through the shared-kernel contracts.
    private static final DescribedPredicate<JavaClass> CARRIES_REACTION_METHOD = DescribedPredicate
            .describe("carries an @DomainEventReaction method", type -> type.getMethods().stream()
                    .anyMatch(method -> method.isAnnotatedWith(OnDomainEvent.class)));

    @Test
    void reactionHandlersLiveOnlyInAdapterListenerPackages() {
        List<JavaClass> violations = CLASSES.stream()
                .filter(CARRIES_REACTION_METHOD)
                .filter(type -> !type.getPackageName().matches(".+[.]adapter[.]listeners"))
                .toList();
        assertThat(violations)
                .as("@DomainEventReaction handlers live in the owning module's adapter/listeners/ package")
                .isEmpty();
    }

    @Test
    void onlyAdapterListenerPackagesDeclareReactions() {
        // Platform annotations must not be applied directly anywhere else.
        List<JavaClass> platformUsage = CLASSES.stream()
                .filter(type -> !type.getPackageName().matches(".+[.]adapter[.]listeners"))
                .filter(type -> type.getAllMethods().stream().anyMatch(method ->
                        method.isAnnotatedWith(ApplicationModuleListener.class)))
                .toList();
        assertThat(platformUsage)
                .as("Spring Modulith's listener annotation is composed in @DomainEventReaction only")
                .isEmpty();
    }

    @Test
    void listenersImplementTheSharedContract() {
        ArchRule rule = classes()
                .that(CARRIES_REACTION_METHOD)
                .should().implement(DomainEventListener.class)
                .because("listener classes conform to the per-module event contract (ADR-0004)");
        rule.check(CLASSES);
    }
}
