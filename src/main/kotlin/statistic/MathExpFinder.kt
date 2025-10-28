package statistic

import genarator.Generator
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min

class MathExpFinder(val generator: Generator): PeriodProcessor {
    private var minPeriod: Int = Int.MAX_VALUE
        private set
    private var maxPeriod: Int = Int.MIN_VALUE
        private set

    /**
     * Buffer for found periods
     */
    private val foundPeriods = mutableListOf<Int>()

    /**
     * In order to economize memory usage, we save avg period instead of list of all periods
     */
    private var avgPeriod: BigDecimal = 0.toBigDecimal()
    private var countInAvg: BigInteger = 0.toBigInteger()
    private fun recalculateAvg(){
        if (foundPeriods.size == 1024){
            val countBD = countInAvg.toBigDecimal()
            val newCountBD = foundPeriods.size.toBigDecimal()
            avgPeriod =
                (avgPeriod * countBD + newCountBD * foundPeriods.average().toBigDecimal()) / (countBD + newCountBD)

            countInAvg += foundPeriods.size.toBigInteger()
            foundPeriods.clear()
        }
    }
    fun getAvgPeriod(): BigDecimal = avgPeriod


    override fun processNewPeriod(period: Int) {
        minPeriod = min(minPeriod, period)
        maxPeriod = max(maxPeriod, period)
        foundPeriods.add(period)
        recalculateAvg()
    }

    override fun stop() {
        println()
        println("*********************************************")
        println("Results for <${generator.name}> generator:")
        println("AVG erg. Period: ${getAvgPeriod()} bytes.")
        println("MIN erg. Period: $minPeriod bytes.")
        println("MAX erg. Period: $maxPeriod bytes.")
        println("*********************************************")
    }
}