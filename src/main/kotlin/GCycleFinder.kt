import statistic.PeriodProcessor
import java.util.*

/**
 * Класс для нахождения g-цикла, которых также хранит его подпоследовательность и количества появлений каждого байта
 *
 * @param bits мощность алфавита в битах (2..32). Размер алфавита = 2^bits.
 */
class GCycleFinder(
    private val periodProcessors: List<PeriodProcessor>,
    val bits: Int = 8,
) {

    constructor(processor: PeriodProcessor, bits: Int = 8) : this(listOf(processor), bits)

    init {
        require(bits in 2..32) { "bits must be in 2..32, but was $bits" }
    }

    /**
     * Размер алфавита (количество различных символов) = 2^bits
     */
    val alphabetSize: Long = 1L shl bits

    /**
     * Subsequence of origin sequence that participates in the calculation of the current period
     */
    private val sequenceOfCurrentPeriod: LinkedList<Long> = LinkedList<Long>()

    /**
     * Разреженный счётчик появлений каждого символа в текущей подпоследовательности.
     * Символы с нулевым количеством в карте не хранятся.
     */
    private val counts: HashMap<Long, Int> = HashMap()

    /**
     * Количество различных символов, присутствующих в текущей подпоследовательности (count >= 1).
     * Совпадает с counts.size, хранится отдельно как Long для сравнения с alphabetSize.
     */
    private var distinctPresent: Long = 0

    /**
     * Гистограмма: сколько символов имеют ровно k появлений (k >= 1).
     * Позволяет быстро вычислять количество символов с count в диапазоне 1..t.
     */
    private val countOfCounts: HashMap<Int, Long> = HashMap()
    //private val firstPartSizes = (0..0 step 50)
    //private val periodMatches = listOf(131)

    private val sensitivities = listOf(4,8,12,15)
    private val predictors = sensitivities.map { s ->
            GCyclePredictor(
                this,
                PredictorParameters(
                    firstPartMinSize = 0,
                    periodMathesCount = 2,
                    s,
                )
            )
        }


    /**
     * Количество символов алфавита, отсутствующих в подпоследовательности при заданном пороге,
     * т.е. символов, встречающихся не более чем threshold раз.
     * Вычисляется как размер алфавита минус количество символов с count > threshold.
     */
    private fun notPresentSizeByThreshold(threshold: Int): Long =
        alphabetSize - (distinctPresent - symbolsWithCountIn1To(threshold))

    /**
     * Количество символов, встречающихся в подпоследовательности от 1 до t раз включительно.
     */
    private fun symbolsWithCountIn1To(t: Int): Long {
        var sum = 0L
        for (c in 1..t) {
            sum += countOfCounts[c] ?: 0L
        }
        return sum
    }

    /**
     * Увеличивает счётчик появлений символа на 1, поддерживая distinctPresent и countOfCounts.
     */
    private fun incrementCount(symbol: Long) {
        val old = counts[symbol] ?: 0
        counts[symbol] = old + 1
        if (old == 0) {
            distinctPresent++
        } else {
            decrementCountOfCounts(old)
        }
        incrementCountOfCounts(old + 1)
    }

    /**
     * Уменьшает счётчик появлений символа на 1, поддерживая distinctPresent и countOfCounts.
     * @return новое количество появлений символа.
     */
    private fun decrementCount(symbol: Long): Int {
        val old = counts[symbol] ?: 0
        if (old == 0) return 0
        decrementCountOfCounts(old)
        val new = old - 1
        if (new == 0) {
            counts.remove(symbol)
            distinctPresent--
        } else {
            counts[symbol] = new
            incrementCountOfCounts(new)
        }
        return new
    }

    private fun incrementCountOfCounts(k: Int) {
        if (k <= 0) return
        countOfCounts[k] = (countOfCounts[k] ?: 0L) + 1L
    }

    private fun decrementCountOfCounts(k: Int) {
        if (k <= 0) return
        val v = (countOfCounts[k] ?: 0L) - 1L
        if (v <= 0L) countOfCounts.remove(k) else countOfCounts[k] = v
    }

    var period: Int = 0
        private set

    /**
     * Checks that all values of alphabet is present in current subsequence
     */
    private fun checkAllValuesIsPresent(): Boolean {
        return distinctPresent == alphabetSize
    }

    /**
     * Drops the oldest value in sequenceOfCurrentPeriod, decrement count of this value and decrement period
     * @return true if there is still another appearance of first value in sequenceOfCurrentPeriod.
     * false, if it was last appearance of first value sequenceOfCurrentPeriod
     */
    private fun dropOldest(): Boolean {
        val first = sequenceOfCurrentPeriod.removeFirst()
        val newCountsOfFirstValue = decrementCount(first)
        period--
        return newCountsOfFirstValue != 0
    }

    // Отправляет длину нового g-периода статистическим утилитам обработчикам
    // Например поиск мат. ожидания и т.п.
    private fun processNewPeriod() {
        periodProcessors.forEach { periodProcessor ->
            periodProcessor.processNewPeriod(period)
        }
    }


    /**
     * Checks each continuous subsequence, if it has all values of alphabet or not
     */
    private fun checkSubSequences() {
        if (sequenceOfCurrentPeriod.size.toLong() < alphabetSize)
            return

        while (dropOldest() && sequenceOfCurrentPeriod.size.toLong() > alphabetSize) {
            processNewPeriod()
        }
    }

    /**
     * Добавляет следующий символ из текущей псевдослучайной последовательности.
     * Значение символа должно быть в диапазоне [0, alphabetSize).
     */
    fun append(symbol: Long) {
        require(symbol in 0 until alphabetSize) { "symbol $symbol out of alphabet range [0, $alphabetSize)" }
        sequenceOfCurrentPeriod.add(symbol)
        // Сначала пробуем предсказать (Отправляем предсказателям правильный следующий символ)
        predictors.forEach { p ->
            val t = p.params.sensitiveThreshold
            val notPresentSize = notPresentSizeByThreshold(t)
            val isNotPresent = (counts[symbol] ?: 0) <= t
            p.predict(symbol, notPresentSize, isNotPresent)
        }
        // Затем инкрементируем количество добавленного символа
        incrementCount(symbol)
        // Увеличиваем длину периода
        period++
        // Если после добавления подпоследовательность содержит все символы
        // то значит мы нашли очередной g-период и отправляем его на обработку статистическим утилитам
        if (checkAllValuesIsPresent()) {
            processNewPeriod()
            checkSubSequences()
        }
    }

    fun showPredictorsResults(prngName: String) {
        println()
        println("___________________________________________")
        println("___________________________________________")
        println("Генератор, Размер МБ, S, L, F, Пропущено, Промахов, Верно предсказано*, Точность, % отклонения от 1/$alphabetSize")
        predictors.sortedByDescending { it.getSuccessPredictRatio() }.forEach { p -> p.showResults(prngName) }
        //predictors.maxBy { it.getSuccessPredictRatio() }.showResults()
        println("___________________________________________")
        println("___________________________________________")
        println()
    }
}