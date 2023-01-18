package fr.sacane.jmanager.common

import com.toxicbakery.bcrypt.Bcrypt
import fr.sacane.jmanager.domain.model.Account
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest


class Hash() {
    private val md = MessageDigest.getInstance("SHA-512")

    init{
        md.update(salt())
    }

    fun hash(pwd: String): ByteArray{
        return md.digest(pwd.toByteArray(StandardCharsets.UTF_8))
    }

    private fun salt(): ByteArray?{
        val path: File = Path.of(System.getProperty("user.dir").plus("/salt.txt")).toFile()
        return try{
            BufferedReader(FileReader(path)).use {
                it.lines().findFirst().get()
            }.toByteArray()
        }catch(e: IOException){
            null
        }
    }

    companion object {

        fun hash(pass:String): ByteArray{
            return Bcrypt.hash(pass, Constants.CODE)
        }
        fun verify(given: String, expected: String): Boolean{
            return Bcrypt.verify(given, Bcrypt.hash(expected, Constants.CODE))
        }
        fun verify(given: String, expected: ByteArray): Boolean{
            return Bcrypt.verify(given, expected)
        }
    }
}

