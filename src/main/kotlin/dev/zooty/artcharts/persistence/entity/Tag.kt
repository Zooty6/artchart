package dev.zooty.artcharts.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import lombok.ToString

@Entity
@Table(name = "tag")
@Suppress("unused")
class Tag (
    @Id var name: String,
    var category: String,

    @ToString.Exclude
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "art_tag",
        joinColumns = [JoinColumn(name = "tagName")],
        inverseJoinColumns = [JoinColumn(name = "artId")]
    )
    var arts : MutableSet<Art> = mutableSetOf()
)