package com.frankie.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FrankieBackendApplication

fun main(args: Array<String>) {
    runApplication<FrankieBackendApplication>(*args)
}
