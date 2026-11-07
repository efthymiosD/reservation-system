package com.decoupledx.reservation.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/**
 * ADR-0001 rules 2, 4, 7+9 and 10 — enforced generically for EVERY module in the
 * reservation monolith (administration, identity, resource, venue, policy, pricing,
 * content, availability, reservation, …), not just administration. Importing the whole
 * {@code com.decoupledx.reservation} tree means the same architecture is verified project-wide.
 *
 * <p>All three previously-{@code @Disabled} rules were re-enabled (2026-11-07) after the
 * codebase was brought into conformance and the predicates were made precise:
 * rules 2 and 10 now hold outright; rule 7+9's predicate was narrowed to the true
 * sibling-adapter hazards ({@code ..adapter.persistence..} + {@code ..adapter.scheduling..}),
 * so a controller's OWN request/response/view types in its {@code .adapter.web} package
 * do not register as false "sibling" dependencies.</p>
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
    // adapter -----------------------------------------------------------------------

    private static final DescribedPredicate<JavaClass> CONTROLLERS = DescribedPredicate
            .describe("web controllers of any module", candidate ->
                    candidate.getPackageName().contains(".adapter")
                            && !candidate.getPackageName().contains(".adapter.api")
                            && candidate.getSimpleName().endsWith("Controller"));

    @Test
    void controllersDoNotDependOnOtherAdaptersOrApiService() {
        noClasses()
                .that(CONTROLLERS)
                .should().dependOnClassesThat()
                .resideInAnyPackage("..adapter.persistence..", "..adapter.scheduling..")
                .because("ADR-0001 rules 7 + 9: web controllers use inbound ports "
                        + "(domain.port) directly; adapters never depend on sibling "
                        + "adapters. (A controller's OWN request/response/view types in its "
                        + ".adapter.web package are deliberately NOT flagged — they are the "
                        + "one web layer it composes; a cross-package web reference is "
                        + "already blocked by rule 10's package-private visibility.)")
                .check(CLASSES);
    }

    // rule 10: adapter classes are package-private except adapter.api and *DataValues --

    private static final DescribedPredicate<JavaClass> ADAPTER_CLASSES = DescribedPredicate
            .describe("adapter classes of any module other than the exported api package "
                    + "and immutable *DataValue snapshots", candidate ->
                    candidate.getPackageName().contains(".adapter")
                            && !candidate.getPackageName().contains(".adapter.api")
                            && !candidate.getSimpleName().contains("DataValue"));

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
