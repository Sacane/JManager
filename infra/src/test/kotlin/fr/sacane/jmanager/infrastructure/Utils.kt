package fr.sacane.jmanager.infrastructure

import io.restassured.http.Cookie

fun generateCookie(token: String): Cookie = Cookie.Builder("token", token)
    .setPath("/")
    .setDomain("localhost")
    .setHttpOnly(true)
    .setComment("JSESSIONID")
    .setMaxAge(3600)
    .build()