package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Wip
import dev.zooty.artcharts.services.api.WipService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/wip")
class WipController(val wipService: WipService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getWips(): List<Wip> = wipService.getWips()

    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getWip(@PathVariable("id") id: Long): Wip = wipService.getWip(id)

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun createWip(@Valid @RequestBody createWipRequest: CreateWipRequest): ResponseEntity<Wip> =
        ResponseEntity.status(HttpStatus.CREATED).body(wipService.create(createWipRequest))

    @PostMapping("/{id}")
    fun updateWip(
        @PathVariable id: Long,
        @Valid @RequestBody updateWipRequest: UpdateWipRequest
    ): ResponseEntity<Void> {
        wipService.update(id, updateWipRequest)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deliverArt(
        @PathVariable("id") wipId: Long,
        @Valid @RequestBody request: DeliverArtRequest
    ): Art = wipService.deliverArt(wipId, request, emptyList())

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun deliverArt(
        @PathVariable("id") wipId: Long,
        @Valid @RequestPart("request") request: DeliverArtRequest,
        @RequestPart("file", required = false) files: List<MultipartFile>?
    ): Art = wipService.deliverArt(wipId, request, files.orEmpty())
}