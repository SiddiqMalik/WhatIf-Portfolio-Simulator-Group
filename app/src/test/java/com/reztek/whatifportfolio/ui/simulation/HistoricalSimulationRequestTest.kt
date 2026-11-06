package com.reztek.whatifportfolio.ui.simulation

import com.google.gson.Gson
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationListResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalSimulationRequestTest {

    private fun state(
        allocations: List<AllocationRow> = listOf(
            AllocationRow("AAPL", "equity", "Apple", "60"),
            AllocationRow("BTC", "crypto", "Bitcoin", "40")
        )
    ) = SimulationBuilderUiState(
        name = "  Tech mix  ",
        initialInvestment = "10000",
        recurringContribution = "500",
        frequency = "quarterly",
        startDate = "2021-01-01",
        endDate = "2024-01-01",
        allocations = allocations
    )

    @Test
    fun requestCarriesTrimmedNameDatesAmountsFrequencyAndAllocations() {
        val request = SimulationBuilderViewModel.buildRequest(state())

        assertEquals("Tech mix", request.name)
        assertEquals("2021-01-01", request.startDate)
        assertEquals("2024-01-01", request.endDate)
        assertEquals(10000.0, request.initialInvestment, 0.0)
        assertEquals(500.0, request.recurringContribution, 0.0)
        assertEquals("quarterly", request.frequency)
        assertEquals(listOf("AAPL", "BTC"), request.allocations.map { it.symbol })
        assertEquals(listOf("equity", "crypto"), request.allocations.map { it.type })
        assertEquals(listOf(60.0, 40.0), request.allocations.map { it.percent })
    }

    @Test
    fun requestSerialisesToTheFieldNamesTheBackendReads() {
        val json = Gson().toJsonTree(SimulationBuilderViewModel.buildRequest(state())).asJsonObject

        listOf(
            "name", "startDate", "endDate", "initialInvestment",
            "recurringContribution", "frequency", "allocations"
        ).forEach { assertTrue("missing $it", json.has(it)) }
        val first = json.getAsJsonArray("allocations")[0].asJsonObject
        assertEquals("AAPL", first.get("symbol").asString)
        assertEquals("equity", first.get("type").asString)
        assertEquals(60.0, first.get("percent").asDouble, 0.0)
    }

    @Test
    fun allocationTotalMustBeOneHundredToBeValid() {
        assertEquals(100.0, state().allocationTotal, 0.0)
        val short = state(listOf(AllocationRow("AAPL", "equity", "Apple", "60")))
        assertEquals(60.0, short.allocationTotal, 0.0)
        assertFalse(short.copy(allocationError = "Allocations must total 100%").isValid)
    }

    @Test
    fun parsesTheBackendListResponseAndMapsItForComparison() {
        val json = """
            {"data":[{
              "id":"sim-1","name":"Tech mix","startDate":"2021-01-01","endDate":"2024-01-01",
              "initialInvestment":10000,"recurringContribution":500,"frequency":"quarterly",
              "status":"saved","totalContributed":16000,"finalValue":21000.5,"profitLoss":5000.5,
              "percentReturn":31.25,"realValue":null,"realProfitLoss":null,"realReturnPct":null,
              "allocations":[{"symbol":"AAPL","type":"equity","percent":100}],
              "perAsset":[{"symbol":"AAPL","type":"equity","percent":100,"units":120.5,"contributed":16000}],
              "timeSeries":[
                {"date":"2021-01-01","contributed":10000,"nominalValue":10000,"realValue":null},
                {"date":"2021-04-01","contributed":10500,"nominalValue":11000,"realValue":null}
              ],
              "createdAt":"2026-01-01T00:00:00.000Z","updatedAt":"2026-01-01T00:00:00.000Z"
            }]}
        """.trimIndent()

        val dto: SimulationDto = Gson().fromJson(json, SimulationListResponse::class.java).data.single()
        val mapped = ComparisonSimulationMapper.fromRemote(dto)

        assertEquals("remote-sim-1", mapped.id)
        assertEquals(21000.5, mapped.finalValue, 0.0)
        assertEquals(31.25, mapped.percentReturn!!, 0.0)
        assertNull(mapped.realValue)
        assertEquals(2, mapped.chartPoints.size)
        assertEquals("2021-04-01", mapped.chartPoints[1].date)
        assertEquals(11000.0, mapped.chartPoints[1].nominalValue, 0.0)
    }

    @Test
    fun parsesAServerErrorBodyShape() {
        val body = Gson().fromJson(
            """{"error":{"code":"invalid_request","message":"BTC only has price history from 2025-06-01"}}""",
            com.google.gson.JsonObject::class.java
        )

        assertEquals(
            "BTC only has price history from 2025-06-01",
            body.getAsJsonObject("error").get("message").asString
        )
    }
}
