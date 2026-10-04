package com.decoupledx.reservation.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * ADR-0003: every record lives in its own top-level source file. Enforced
 * project-wide for main sources - no nested record/class bodies are allowed
 * (synthetic compiler artifacts, e.g. switch maps, are not classes imported
 * here and never matched; anonymous artifacts would surface and must be
 * removed as well).
 */
class RecordPlacementTest {

    private static final JavaClasses CLASSES =
            new ClassFileImporter().importPackages("com.decoupledx.reservation");

    /**
     * Nested types with hand-written names violate the rule. Compiler-generated
     * artifacts are exempt: Lombok's {@code OuterXBuilder} inner classes and
     * anonymous artifacts {@code Outer$1..$n}.
     */
    private static final DescribedPredicate<JavaClass> HAND_WRITTEN_NESTED =
            new DescribedPredicate<>("hand-written nested types") {
                @Override
                public boolean test(JavaClass input) {
                    String name = input.getName();
                    return name.contains("$")
                            && !name.matches(".*\\$\\w*Builder$")
                            && !name.matches(".*\\$\\d+$");
                }
            };

    @Test
    void everyTypeIsATopLevelType() {
        ArchRule rule = noClasses().that(HAND_WRITTEN_NESTED).should().beNestedClasses()
                .because("nested records hurt discoverability (ADR-0003): "
                        + "every record gets its own top-level file");
        rule.allowEmptyShould(true);
        rule.check(CLASSES);
    }
}