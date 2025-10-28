package statistic

import genarator.Generator
import java.math.BigDecimal
import java.math.MathContext

private const val STUDENT_95_ACCURACY_COEFFICIENT = 2

class StandardDeviationFinder(val generator: Generator, val mathExpectation: BigDecimal) : PeriodProcessor {

    private var summ = 0.toBigDecimal()
    private var count = 0.toBigInteger()

    fun getDispersion() = (summ / count.toBigDecimal())

    fun getStandardDeviation() = getDispersion().sqrt(MathContext.DECIMAL32)

    fun getConfidenceIntervalDelta() = getStandardDeviation() / count.toBigDecimal()
        .sqrt(MathContext.DECIMAL32) * STUDENT_95_ACCURACY_COEFFICIENT.toBigDecimal()

    fun getConfidenceIntervalMin() = mathExpectation - getConfidenceIntervalDelta()
    fun getConfidenceIntervalMax() = mathExpectation + getConfidenceIntervalDelta()

    fun getConfidenceInterval(): String = "[${getConfidenceIntervalMin()} <= ME <= ${getConfidenceIntervalMax()}]"


    override fun processNewPeriod(period: Int) {
        summ += (period.toBigDecimal() - mathExpectation).pow(2)
        count++
    }

    override fun stop() {
        println()
        println("*********************************************")
        println("Results for <${generator.name}> generator:")
        println("Standard deviation of erg. Period: ${getStandardDeviation()} bytes.")
        println("Math. expectation confidence interval: ${getConfidenceInterval()} bytes.")
        println("ME CI power: ${getConfidenceIntervalMax() - getConfidenceIntervalMin()} bytes")
        println("*********************************************")
    }
}