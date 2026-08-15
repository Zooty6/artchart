package dev.zooty.artcharts.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SiteController {
    @GetMapping("/", "/site", "/site/")
    fun site(): String = "site/index"
}
