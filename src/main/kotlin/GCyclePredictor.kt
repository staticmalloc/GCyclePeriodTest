import java.math.BigInteger


class GCyclePredictor(
    private val finder: GCycleFinder,
    private val params: PredictorParameters,
) {
    private var skipped: BigInteger = BigInteger.ZERO
    private var missed: BigInteger = BigInteger.ZERO
    private var matched: MutableMap<Int, BigInteger> = mutableMapOf()

    fun predict(trueValue: Int, notPresent: List<Int>) {
        val notPresentSize = notPresent.size
        if (255 - notPresentSize < params.periodMathesCount || finder.period < params.firstPartMinSize) {
            skipped++
            return
        }
        if (trueValue in notPresent) {
            matched[notPresentSize] = matched.getOrDefault(notPresentSize, BigInteger.ZERO) + BigInteger.ONE
        } else {
            missed++
        }
    }

    fun showResults() {
        println("_____________PREDICTION RESULTS_____________")
        println(params)
        println("SKIPPED COUNT: $skipped")
        println("MISSED COUNT : $missed")
        matched.forEach {
            println(
                "MATCHED with ${String.format("%.2f", (1.0 / it.key))}: ${it.value}"
            )
        }
    }
}

data class PredictorParameters(
    /**
     * Min length of not full g-cycle for making prediction
     */
    val firstPartMinSize: Int,
    /**
     * Min count of numbers from alphabet already present in previous subsequence for making prediction
     * 0 - 254
     */
    val periodMathesCount: Int,
) {
    init {
        require(periodMathesCount in 0..254) { throw IllegalArgumentException() }
    }
}