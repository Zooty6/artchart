package dev.zooty.artcharts.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import org.hibernate.annotations.Type

@Entity
@Table(name = "wip")
class Wip(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    var otherCharacters: String?,
    @NotNull
    var type: String,
    var quality: String?,
    @NotNull
    var species: String,
    var orderedDate: String?,
    var payedDate: String?,
    var deadlineDate: String?,
    @NotNull
    @Type(value = PriceType::class)
    var price: Price,
    var note: String?,
    @NotNull
    @ManyToOne
    @JoinColumn(name = "artistId")
    var artist: Artist,
    var isNsfw: Boolean,
)