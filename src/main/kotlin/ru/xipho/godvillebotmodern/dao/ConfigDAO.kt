package ru.xipho.godvillebotmodern.dao

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class ConfigDAO(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long? = null,
    val checkPeriodSeconds: Int = 30,
    val checkHealth: Boolean = true,
    val checkPet: Boolean = true,
    val healthLowWarningThreshold: Int = 150,
    val allowPranaExtract: Boolean = true,
    val maxPranaExtractionsPerDay: Int = 10,
    val maxPranaExtractionsPerHour: Int = 2,
)