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
     * где ключ - это размер подмножества алфавита, в который действительно попал неизвестный следующий байт
     */
    private var matched: MutableMap<Int, BigInteger> = mutableMapOf()

    /**
     * @param trueValue - реальный следующий символ из выходной последовательности генераторов
     * @param notPresentByThreshold - подмножество символов алфавита, которые встречаются реже чем threshold
     */
    fun predict(trueValue: Int, notPresentByThreshold: HashSet<Int>) {
        val notPresentSize = notPresentByThreshold.size
        val period = finder.period
        if (256 - notPresentSize < params.periodMathesCount || period < params.firstPartMinSize) {
            skipped++
            return
        }

        if (trueValue in notPresentByThreshold) {
            // попадание в подмножество
            matched[notPresentSize] = matched.getOrDefault(notPresentSize, BigInteger.ZERO) + BigInteger.ONE
        } else {
            // промах
            missed++
        }
    }

    fun getSuccessPredictRatio(): Double {
        val r = PredictionResults(skipped.toLong(), missed.toLong(), matched)
        return r.match / (r.miss + r.match)
    }

    fun showResults(prngName: String) {
        val randomRatio = 1.0 / 256
        val results = listOf(PredictionResults(skipped.toLong(), missed.toLong(), matched))

        results.forEach { r ->
            val ratio = r.match / (r.miss + r.match)
            //println("Генератор, Размер МБ, S, L, F, Пропущено, Промахов, Верно предсказано*, Точность, % отклонения от 1/256")
            listOf<String>(
                prngName,
                TEXT_FILE_SIZE_MB.toString(),
                params.sensitiveThreshold.toString(),
                params.firstPartMinSize.toString(),
                params.periodMathesCount.toString(),
                r.skip.toString(),
                r.miss.toString(),
                "${TEXT_FILE_SIZE_MB * MB - r.skip}",
                String.format("%,f", ratio),
                String.format("%,12f", (ratio - randomRatio)*256*100) + "%"
            ).joinToString(" ").let { println(it) }
            //print(prngName)
            //print(TEXT_FILE_SIZE_MB)
            //println("S: ${params.sensitiveThreshold}")
            //println("L: ${params.firstPartMinSize}")
            //println("F: ${params.periodMathesCount}")
            //println("Пропущено: ${r.skip}")
            //println("Промахов: ${r.miss}")
            //println("Верно предсказано*: ${TEXT_FILE_SIZE_MB * MB - r.skip}")
            //println("Точность: ${String.format("%,f", ratio)}")
            //println("% отклонения от 1/256 (0,00390625): ${String.format("%,12f", (ratio - randomRatio)*256*100)}%")
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
     * Min count of numbers from alphabet already present(by threshold) in previous subsequence for making prediction
     * 2 - 255
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
        require(periodMathesCount in 2..255) { throw IllegalArgumentException() }
        require(sensitiveThreshold in 0..127) { throw IllegalArgumentException() }
    }
}