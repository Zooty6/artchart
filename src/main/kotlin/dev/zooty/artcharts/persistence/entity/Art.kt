package dev.zooty.artcharts.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Type

@Entity
@Suppress("unused", "JpaDataSourceORMInspection")
class Art(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    val otherCharacters: String?,
    val type: String,
    val quality: String?,
    var species: String,
    val orderedDate: String?,
    val payedDate: String?,
    val deliveredDate: String,
    val fileName: String,
    @Type(value = PriceType::class)
    val price: Price,
    val note: String?,
    @ManyToOne
    @JoinColumn(name = "artistId")
    val artist: Artist,
    val isNsfw: Boolean,
    val link: String,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "art_tag",
        joinColumns = [JoinColumn(name = "artId")],
        inverseJoinColumns = [JoinColumn(name = "tagName")]
    )
    val tags: MutableSet<Tag> = mutableSetOf(),
)