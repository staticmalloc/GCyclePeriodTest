import java.math.BigInteger


class GCyclePredictor(
    private val finder: GCycleFinder,
    private val params: PredictorParameters,
) {
    private var skipped: BigInteger = BigInteger.ZERO
    private var missed: BigInteger = BigInteger.ZERO
    private var matched: MutableMap<Int, BigInteger> = mutableMapOf()

    fun predict(trueValue: Int, notPresent: HashSet<Int>) {
        val notPresentSize = notPresent.size
        val period = finder.period
        if (255 - notPresentSize < params.periodMathesCount || period < params.firstPartMinSize) {
            skipped++
            return
        }
        if (trueValue in notPresent) {
            if (notPresentSize > 1)
                println()
            matched[notPresentSize] = matched.getOrDefault(notPresentSize, BigInteger.ZERO) + BigInteger.ONE
        } else {
            missed++
        }
    }

    fun getSuccessPredictRatio(): Double {
        val r = PredictionResults(skipped.toLong(), missed.toLong(), matched)
        return r.match / (r.miss + r.match)
    }

    fun showResults() {
        println("_____________PREDICTION RESULTS_____________")
        println("SKIPPED COUNT: $skipped")
        println("MISSED COUNT : $missed")
        matched.forEach {
            println(
                "MATCHED with ${String.format("%.2f", (1.0 / it.key))}: ${it.value}"
            )
        }

        val randomRatio = 1.0 / 256
        val results = listOf(PredictionResults(skipped.toLong(), missed.toLong(), matched))
        results.forEach { r ->
            val total = r.skip + r.miss + r.match
            println("Обработано мегабайт: ${total / 1024 / 1024}")
            println("Минимальный размер первой части g-цикла: ${params.firstPartMinSize} байт")
            println("Минимальное кол-во символов в подпоследовательности: ${params.periodMathesCount} байт")
            println("Доля попаданий от всей последовательности: ${r.match / total}")
            val ratio = r.match / (r.miss + r.match)
            println("Доля попаданий от кол-ва попыток: $ratio")
            println("Отклонение от 1/256 (0.00390625): ${String.format("%.12f", ratio - randomRatio)}")
            println("Процент приведенный к битам: ${String.format("%.10f", ratio.times(12800))}%")
        }
    }
}

data class PredictionResults(
    val skip: Long,
    val miss: Long,
    // Кол-во попаданий, каждое из которых поделено на кол-во символов, участвовавших в конкретном предсказании
    val match: Double
) {
    constructor(skip: Long, miss: Long, matched: Map<Int, BigInteger>): this(skip, miss, matched.map { it.value.toDouble() / it.key }.sum())
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