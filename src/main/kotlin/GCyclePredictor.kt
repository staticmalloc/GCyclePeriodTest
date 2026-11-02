import genarator.MB
import genarator.TEXT_FILE_SIZE_MB
import java.math.BigInteger


class GCyclePredictor(
    private val finder: GCycleFinder,
    val params: PredictorParameters,
) {
    /**
     * Количество пропущенных
     */
    private var skipped: BigInteger = BigInteger.ZERO

    /**
     * Количество промахов
     */
    private var missed: BigInteger = BigInteger.ZERO

    /**
     * Ассоциативный массив количества попаданий
     * где ключ - это размер подмножества алфавита, в который действительно попал неизвестный следующий символ
     */
    private var matched: MutableMap<Long, BigInteger> = mutableMapOf()

    /**
     * @param trueValue - реальный следующий символ из выходной последовательности генераторов
     * @param notPresentSize - размер подмножества символов алфавита, которые встречаются не чаще чем threshold
     * @param trueValueIsNotPresent - попал ли trueValue в это подмножество
     */
    fun predict(trueValue: Long, notPresentSize: Long, trueValueIsNotPresent: Boolean) {
        val period = finder.period
        if (finder.alphabetSize - notPresentSize < params.periodMathesCount || period < params.firstPartMinSize) {
            skipped++
            return
        }

        if (trueValueIsNotPresent) {
            // попадание в подмножество
            matched[notPresentSize] = matched.getOrDefault(notPresentSize, BigInteger.ZERO) + BigInteger.ONE
        } else {
            // промах
            missed++
        }
    }

    fun getSuccessPredictRatio(): Double {
        val r = PredictionResults(skipped.toLong(), missed.toLong(), matched)
        return r.getSuccessPredictRatio()
    }

    fun showResults(prngName: String) {
        val randomRatio = 1.0 / finder.alphabetSize
        val results = listOf(PredictionResults(skipped.toLong(), missed.toLong(), matched))

        results.forEach { r ->
            val ratio = r.getSuccessPredictRatio()
            //println("Генератор, Размер МБ, S, L, F, Пропущено, Промахов, Общее число прогнозов, Точность, % отклонения от 1/256")
            listOf<String>(
                prngName,
                TEXT_FILE_SIZE_MB.toString(),
                params.sensitiveThreshold.toString(),
                params.firstPartMinSize.toString(),
                params.periodMathesCount.toString(),
                r.skip.toString(),
                r.miss.toString(),
                String.format("%,3f",(TEXT_FILE_SIZE_MB * MB * (8.0 / finder.bits) - r.skip)),
                String.format("%.12f", ratio),
                String.format("%,12f", (ratio - randomRatio) * finder.alphabetSize * 100) + "%"
            ).joinToString(" ").let {
                println(it)
            }
            //print(prngName)
            //print(TEXT_FILE_SIZE_MB)
            //println("S: ${params.sensitiveThreshold}")
            //println("L: ${params.firstPartMinSize}")
            //println("F: ${params.periodMathesCount}")
            //println("Пропущено: ${r.skip}")
            //println("Промахов: ${r.miss}")
            //println("Общее число прогнозов: ${TEXT_FILE_SIZE_MB * MB - r.skip}")
            //println("Точность: ${String.format("%,f", ratio)}")
            //println("% отклонения от 1/256 (0,00390625): ${String.format("%,12f", (ratio - randomRatio)*256*100)}%")
        }
    }
}

data class PredictionResults(
    val skip: Long,
    val miss: Long,
    // Кол-во попаданий, каждое из которых поделено на кол-во символов, участвовавших в конкретном предсказании
    val matchRatio: Double,
    val matchCount: BigInteger,
) {
    constructor(skip: Long, miss: Long, matched: Map<Long, BigInteger>) : this(
        skip,
        miss,
        matched.map { it.value.toDouble() / it.key }.sum(),
        matched.values.sumOf { it }
    )

    fun getSuccessPredictRatio(): Double {
        return matchRatio / (miss + matchCount.toLong())
    }
}

data class PredictorParameters(
    /**
     * Min length of not full g-cycle for making prediction
     */
    val firstPartMinSize: Int,

    /**
     * Min count of numbers from alphabet already present(by threshold) in previous subsequence for making prediction.
     * Минимум 2; верхняя граница ограничена размером алфавита и проверяется при использовании.
     */
    val periodMathesCount: Int,

    /**
     * 0 - 15
     * Уровень количества появлений какого-либо символа в подпоследовательности,
     * по которому мы будем брать или отсеивать эти символы для формирования подмножества предсказания
     * Например, если = 3, то мы возьмем все символы (В подмножество алфавита для предсказания), которые встречаются 3 и меньше раз в текущем неполном периоде
     * и далее будем говорить, что следущий байт скорее всего из этого подмножества
     */
    val sensitiveThreshold: Int,
) {
    init {
        require(periodMathesCount >= 2) { throw IllegalArgumentException() }
        require(sensitiveThreshold in 0..127) { throw IllegalArgumentException() }
    }
}