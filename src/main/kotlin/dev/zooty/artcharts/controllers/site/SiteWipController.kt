package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.WipService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Controller
@RequestMapping("/site/wips")
class SiteWipController(private val wipService: WipService) {
    companion object {
        private const val VIEW_WIP_FORM = "site/wips/new"
        private const val VIEW_ART_FORM = "site/arts/new"
    }

    @ModelAttribute("currencies")
    fun currencies() = Currency.entries

    @GetMapping
    fun wips(model: Model): String {
        val wips = wipService.getWips()
        val today = LocalDate.now()
        model.addAttribute("wips", wips)
        model.addAttribute("overdueWipIds", wips.filter { wip ->
            val deadline = if (wip.deadlineDate.isNullOrBlank()) {
                parseDate(wip.orderedDate)?.plusMonths(5)
            } else {
                parseDate(wip.deadlineDate)
            }
            deadline != null && today.isAfter(deadline)
        }.map { it.id }.toSet())
        return "site/wips/list"
    }

    private fun parseDate(value: String?): LocalDate? = try {
        value?.trim()?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) {
        null
    }

    @GetMapping("/new")
    fun newWip(model: Model): String {
        model.addAttribute("wip", CreateWipRequest())
        return VIEW_WIP_FORM
    }

    @PostMapping
    fun createWip(
        @Valid @ModelAttribute("wip") wip: CreateWipRequest,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes,
    ): String {
        if (bindingResult.hasErrors()) return VIEW_WIP_FORM

        try {
            wipService.create(wip)
        } catch (_: ResourceNotFoundException) {
            bindingResult.rejectValue("artistName", "artist.notFound", "No artist with this name exists")
            return VIEW_WIP_FORM
        }

        redirectAttributes.addFlashAttribute("wipSaved", true)
        return "redirect:/site/wips"
    }

    @GetMapping("/{id}/edit")
    fun editWip(@PathVariable id: Long, model: Model): String {
        val wip = wipService.getWip(id)
        model.addAttribute("editingWip", wip)
        model.addAttribute("wip", UpdateWipRequest(
            type = wip.type,
            otherCharacters = wip.otherCharacters,
            quality = wip.quality,
            species = wip.species,
            orderedDate = wip.orderedDate,
            payedDate = wip.payedDate,
            deadlineDate = wip.deadlineDate,
            currency = wip.price.currency,
            amount = wip.price.amount,
            note = wip.note,
            isNsfw = wip.isNsfw,
        ))
        return VIEW_WIP_FORM
    }

    @PostMapping("/{id}/edit")
    fun updateWip(
        @PathVariable id: Long,
        @Valid @ModelAttribute("wip") update: UpdateWipRequest,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editingWip", wipService.getWip(id))
            return VIEW_WIP_FORM
        }
        wipService.update(id, update)
        return "redirect:/site/wips"
    }

    @GetMapping("/{id}/deliver")
    fun deliveryForm(@PathVariable id: Long, model: Model): String {
        val wip = wipService.getWip(id)
        model.addAttribute("deliveringWip", wip)
        model.addAttribute("art", DeliverArtRequest(
            type = wip.type,
            otherCharacters = wip.otherCharacters,
            quality = wip.quality,
            species = wip.species,
            currency = wip.price.currency,
            amount = wip.price.amount,
            note = wip.note,
            isNsfw = wip.isNsfw,
        ))
        return VIEW_ART_FORM
    }

    @PostMapping("/{id}/deliver")
    fun deliverWip(
        @PathVariable id: Long,
        @Valid @ModelAttribute("art") art: DeliverArtRequest,
        bindingResult: BindingResult,
        model: Model,
        @RequestParam("file", required = false) files: List<MultipartFile>?,
    ): String {
        if (art.type?.isBlank() == true) bindingResult.rejectValue("type", "NotBlank", "Type is required")
        if (art.species?.isBlank() == true) bindingResult.rejectValue("species", "NotBlank", "Species is required")
        if (bindingResult.hasErrors()) {
            model.addAttribute("deliveringWip", wipService.getWip(id))
            return VIEW_ART_FORM
        }
        val delivered = wipService.deliverArt(id, art, files.orEmpty())
        return "redirect:/site/arts/${delivered.id}"
    }
}
