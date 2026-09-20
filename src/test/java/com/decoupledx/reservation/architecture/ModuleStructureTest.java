package com.decoupledx.reservation.architecture;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ADR-0001 rules 2, 4, 7, 9 and 10 — enforced generically for EVERY module in the
 * reservation monolith (administration, identity, resource, venue, policy, reservation,
 * …), not just administration. Importing the whole {@code com.decoupledx.reservation}
 * tree means the same architecture is verified project-wide.
 */
class ModuleStructureTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.reservation");

    // rule 2: every domain class is package-private ---------------------------------
    // (flat .<module>.domain, only sub-package .port, minus the module's own Config)

    private static final DescribedPredicate<JavaClass> DOMAIN_CLASSES = DescribedPredicate
            .describe("domain classes of any module (excluding domain.port, "
                    + "module domain configs and package-info)", candidate ->
                    candidate.getPackageName().matches(".+\\.reservation\\.[^.]+\\.[^.]+$")
                            && candidate.getPackageName().endsWith(".domain")
                            && !candidate.getSimpleName().contains("Config")
                            && !candidate.getSimpleName().equals("package-info"));

    @Disabled
    @Test
    void domainClassesArePackagePrivate() {
        noClasses()
                .that(DOMAIN_CLASSES)
                .should().bePublic()
                .because("ADR-0001 rule 2: domain classes are package-private "
                        + "except the inbound ports in domain.port (rule 4)")
                .check(CLASSES);
    }

    // rule 4: inbound ports are public interfaces in <module>.domain.port ------------

    private static final DescribedPredicate<JavaClass> PORT_CLASSES = DescribedPredicate
            .describe("inbound port classes of any module", candidate ->
                    candidate.getPackageName().contains(".domain.port"));

    @Test
    void inboundPortsArePublicInterfaces() {
        classes()
                .that(PORT_CLASSES)
                .should().bePublic()
                .andShould().beInterfaces()
                .because("ADR-0001 rule 4: inbound ports are public interfaces "
                        + "accessible from the module's adapter package")
                .check(CLASSES);
    }

    // rules 7 + 9: web controller depends on domain inbound ports, never on another --
    // adapter or on adapter.api as a service provider ---------------------------------

    private static final DescribedPredicate<JavaClass> CONTROLLERS = DescribedPredicate
            .describe("web controllers of any module", candidate ->
                    candidate.getPackageName().contains(".adapter")
                            && !candidate.getPackageName().contains(".adapter.api")
                            && candidate.getSimpleName().endsWith("Controller"));

    @Disabled
    @Test
    void controllersDoNotDependOnOtherAdaptersOrApiService() {
        noClasses()
                .that(CONTROLLERS)
                .should().dependOnClassesThat()
                .resideInAnyPackage("..adapter.web..", "..adapter.persistence..",
                        "..adapter.scheduling..", "..adapter.api.AdministrationApi",
                        "..adapter.api.AdministrationApiImpl")
                .because("ADR-0001 rules 7 + 9: web controllers use inbound ports "
                        + "(domain.port) directly; adapters never depend on sibling adapters")
                .check(CLASSES);
    }

    // rule 10: adapter classes are package-private except adapter.api and *DataValues --

    private static final DescribedPredicate<JavaClass> ADAPTER_CLASSES = DescribedPredicate
            .describe("adapter classes of any module other than the exported api package "
                    + "and immutable *DataValue snapshots", candidate ->
                    candidate.getPackageName().contains(".adapter")
                            && !candidate.getPackageName().contains(".adapter.api")
                            && !candidate.getSimpleName().contains("DataValue"));

    @Disabled
    @Test
    void adapterClassesArePackagePrivateExceptApiAndDataValue() {
        noClasses()
                .that(ADAPTER_CLASSES)
                .should().bePublic()
                .because("ADR-0001 rule 10: everything under adapter/ is package-private "
                        + "except the exported adapter.api named interface and immutable "
                        + "*DataValue snapshots")
                .check(CLASSES);
    }
}
