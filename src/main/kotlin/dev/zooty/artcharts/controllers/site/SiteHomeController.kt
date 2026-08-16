package dev.zooty.artcharts.controllers.site

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SiteHomeController {
    companion object {
        private const val VIEW_SITE_INDEX = "site/index"
    }

    @GetMapping("/", "/site", "/site/")
    fun site(): String = VIEW_SITE_INDEX
}
