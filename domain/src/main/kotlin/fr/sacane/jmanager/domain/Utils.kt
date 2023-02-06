package fr.sacane.jmanager.domain

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest


object Hash {
    private val md = MessageDigest.getInstance("SHA-512")

    init{
        md.update(salt())
    }

    fun hash(pwd: String): ByteArray{
        return md.digest(pwd.toByteArray(StandardCharsets.UTF_8))
    }

    fun contentEquals(pwd: ByteArray, other: String): Boolean{
        return pwd.contentEquals(other.toByteArray())
    }

    private fun salt(): ByteArray?{
        val path: File = Path.of(System.getProperty("user.dir").plus("/salt.txt")).toFile()
        println(path.exists())
        println("HELLO")
        return try{
            BufferedReader(FileReader(path)).use {
                it.lines().findFirst().get()
            }.toByteArray()
        }catch(e: IOException){
            null
        }
    }
}

