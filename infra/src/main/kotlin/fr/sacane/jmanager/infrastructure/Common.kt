package fr.sacane.jmanager.infrastructure

fun extractToken(authorization: String): String = authorization.replace("Bearer ", "")