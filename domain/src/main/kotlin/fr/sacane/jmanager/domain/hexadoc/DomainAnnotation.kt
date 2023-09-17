package fr.sacane.jmanager.domain.hexadoc

import java.lang.annotation.Inherited

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Port(val domainSide: DomainSide = DomainSide.UNSPECIFIED)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DefaultSource

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class DomainImplementation

annotation class Adapter(val domainSide: DomainSide = DomainSide.UNSPECIFIED)

enum class DomainSide{
    DATASOURCE,
    API,
    UNSPECIFIED
}