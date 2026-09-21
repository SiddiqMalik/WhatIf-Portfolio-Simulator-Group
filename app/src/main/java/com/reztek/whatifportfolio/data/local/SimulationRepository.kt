package com.reztek.whatifportfolio.data.local

import kotlinx.coroutines.flow.Flow

class SimulationRepository(private val simulationDao: SimulationDao) {

    val allSimulations: Flow<List<SavedSimulationEntity>> = simulationDao.getAllSimulations()

    suspend fun getSimulationById(id: Long): SavedSimulationEntity? {
        return simulationDao.getSimulationById(id)
    }

    suspend fun saveSimulation(simulation: SavedSimulationEntity): Long {
        return simulationDao.insertSimulation(simulation)
    }

    suspend fun deleteSimulation(simulation: SavedSimulationEntity) {
        simulationDao.deleteSimulation(simulation)
    }

    suspend fun deleteSimulationById(id: Long) {
        simulationDao.deleteSimulationById(id)
    }
}