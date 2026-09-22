package com.reztek.whatifportfolio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_simulations")
data class SavedSimulationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val initialInvestment: Double,
    val monthlyContribution: Double,
    val returnRate: Double,
    val inflationRate: Double,
    val years: Int,
    val finalNominalValue: Double,
    val finalRealValue: Double,
    val createdAt: Long = System.currentTimeMillis()
)