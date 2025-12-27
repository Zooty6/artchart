package dev.zooty.artcharts.persistence.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import lombok.NoArgsConstructor

@Entity
@NoArgsConstructor
class Tag (
    @Id val name: String,
    val category: String,
    
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "art_tag",
        joinColumns = [JoinColumn(name = "tagName")],
        inverseJoinColumns = [JoinColumn(name = "artId")]
    )
    val arts : MutableSet<Art> = mutableSetOf()
)