package fr.sacane.jmanager.domain.hexadoc

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Port(val domainSide: DomainSide = DomainSide.UNSPECIFIED)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DefaultSource

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DomainImplementation

annotation class Adapter(val domainSide: DomainSide = DomainSide.UNSPECIFIED)

enum class DomainSide{
    DATASOURCE,
    API,
    UNSPECIFIED
}