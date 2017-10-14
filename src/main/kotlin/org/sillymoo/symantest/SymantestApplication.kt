package org.sillymoo.symantest

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class SymantestApplication

fun main(args: Array<String>) {
    SpringApplication.run(SymantestApplication::class.java, *args)
}

