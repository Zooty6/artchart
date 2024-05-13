package dev.zooty.artcharts.persistence.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import lombok.NoArgsConstructor
import org.hibernate.annotations.Type

@Entity
@NoArgsConstructor
class Art(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    val otherCharacters: String?,
    val type: String?,
    val quality: String?,
    val species: String?,
    val orderedDate: String?,
    val payedDate: String?,
    val deliveredDate: String?,
    val fileName: String,
    @Type(value = PriceType::class)
    val price: Price,
    val note: String?,
    @ManyToOne
    @JoinColumn(name = "artistId")
    val artist: Artist,
    val isNsfw: Boolean,
    val link: String
)