package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Wip
import org.springframework.data.jpa.repository.JpaRepository

interface WipRepository : JpaRepository<Wip, Long> {
}