package dev.zooty.artcharts

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class ArtChartsApplication

fun main(args: Array<String>) {
    runApplication<ArtChartsApplication>(*args)
}
