package dev.zooty.artcharts

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ArtChartJob

fun main(args: Array<String>) {
    SpringApplication.run(ArtChartJob::class.java, *args)
}
