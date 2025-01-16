package com.qlarr.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QlarrBackendApplication

fun main(args: Array<String>) {
    runApplication<QlarrBackendApplication>(*args)
}
