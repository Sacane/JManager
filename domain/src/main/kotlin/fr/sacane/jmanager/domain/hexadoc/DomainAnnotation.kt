package fr.sacane.jmanager.domain.hexadoc

import java.lang.annotation.Inherited

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Port(val domainSide: Side = Side.UNSPECIFIED)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DefaultSource

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class DomainService

annotation class Adapter(val domainSide: Side = Side.UNSPECIFIED)

enum class Side{
    DATASOURCE,
    API,
    UNSPECIFIED
}