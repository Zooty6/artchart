package dev.zooty.artcharts.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity
@Table(name = "art")
@Suppress("unused")
class Art(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var otherCharacters: String?,
    var type: String,
    var quality: String?,
    var species: String,
    var orderedDate: String?,
    var payedDate: String?,
    var deliveredDate: String,
    var fileName: String,
    @Type(value = PriceType::class)
    var price: Price,
    var note: String?,
    @ManyToOne
    @JoinColumn(name = "artistId")
    var artist: Artist,
    var isNsfw: Boolean,
    var link: String?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "art_tag",
        joinColumns = [JoinColumn(name = "artId")],
        inverseJoinColumns = [JoinColumn(name = "tagName")]
    )
    var tags: MutableSet<Tag> = mutableSetOf(),
)
