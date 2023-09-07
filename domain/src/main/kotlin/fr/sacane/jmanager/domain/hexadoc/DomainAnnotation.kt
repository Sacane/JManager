package fr.sacane.jmanager.domain.hexadoc

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Port(val domainSide: DomainSide = DomainSide.DATASOURCE)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DefaultSource

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DomainImplementation

enum class DomainSide{
    DATASOURCE,
    API
}