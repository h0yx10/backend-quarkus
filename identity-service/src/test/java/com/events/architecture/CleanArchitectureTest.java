package com.events.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.ws.rs.Path;

@AnalyzeClasses(packages = "com.events")
class CleanArchitectureTest {

    @ArchTest
    static final ArchRule domainIsIndependent = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..");

    // El dominio es Java puro: persistencia, CDI y validacion HTTP pertenecen a infraestructura.
    @ArchTest
    static final ArchRule domainDoesNotDependOnFrameworks = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "io.quarkus..", "jakarta..", "lombok..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..");

    // Los casos de uso no conocen Spring (ni Spring Security): hash de passwords, emision de
    // tokens y usuario actual llegan a traves de puertos de salida.
    @ArchTest
    static final ArchRule applicationDoesNotDependOnFrameworks = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "io.quarkus..", "jakarta..", "com.nimbusds..");

    @ArchTest
    static final ArchRule inputAdaptersDoNotDependOnOutputAdapters = noClasses()
            .that()
            .resideInAPackage("..infrastructure.adapter.in..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.adapter.out..");

    @ArchTest
    static final ArchRule outputAdaptersDoNotDependOnInputAdapters = noClasses()
            .that()
            .resideInAPackage("..infrastructure.adapter.out..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure.adapter.in..");

    @ArchTest
    static final ArchRule outputPortsAreInterfaces = classes()
            .that()
            .resideInAPackage("..application.port.out..")
            .should()
            .beInterfaces();

    @ArchTest
    static final ArchRule inputPortsAreInterfaces = classes()
            .that()
            .resideInAPackage("..application.port.in..")
            .and()
            .areTopLevelClasses()
            .and()
            .doNotHaveSimpleName("NuevaSubtareaData")
            .and()
            .doNotHaveSimpleName("TodayGroups")
            .and()
            .doNotHaveSimpleName("OverloadCheckResult")
            .and()
            .doNotHaveSimpleName("EventoProgress")
            .and()
            .doNotHaveSimpleName("AuthResult")
            .should()
            .beInterfaces();

    @ArchTest
    static final ArchRule inputAdaptersDependOnPortsInsteadOfUseCaseImplementations = noClasses()
            .that()
            .resideInAPackage("..infrastructure.adapter.in..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..application.usecase..");

    @ArchTest
    static final ArchRule repositoryAdaptersResideInPersistencePackage = classes()
            .that()
            .haveSimpleNameEndingWith("PersistenceAdapter")
            .should()
            .resideInAPackage("..infrastructure.adapter.out.persistence..");

    @ArchTest
    static final ArchRule restControllersBelongToInputAdapters = classes()
            .that()
            .areAnnotatedWith(Path.class)
            .should()
            .resideInAPackage("..infrastructure.adapter.in.rest.controller..");
}
