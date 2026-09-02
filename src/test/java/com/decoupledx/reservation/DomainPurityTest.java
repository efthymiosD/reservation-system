package com.decoupledx.reservation;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

class DomainPurityTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.reservation");

    // package-info classes carry only structural metadata (e.g. Modulith named interfaces)
    // and contain no domain logic, so they are excluded from the purity rules below.
    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES = DescribedPredicate
            .describe("domain classes other than package-info", candidate ->
                    candidate.getPackageName().contains(".domain")
                            && !candidate.getSimpleName().equals("package-info"));

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
}
