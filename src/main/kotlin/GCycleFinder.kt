import statistic.PeriodProcessor
import java.util.*

/**
 * Класс для нахождения g-цикла, которых также хранит его подпоследовательность и количества появлений каждого байта
 */
class GCycleFinder(private val periodProcessors: List<PeriodProcessor>) {

    constructor(processor: PeriodProcessor) : this(listOf(processor))

    /**
     * Subsequence of origin sequence that participates in the calculation of the current period
     */
    private val sequenceOfCurrentPeriod: LinkedList<UByte> = LinkedList<UByte>()

    /**
     * An alphabet-sized array for counting the number of occurrences of each number in the current subsequence
     */
    private val countsOfNumbers: Array<Int> = Array(256) { 0 }
    //private val firstPartSizes = (0..0 step 50)
    //private val periodMatches = listOf(131)

    private val sensitivities = listOf(4,8,12,15)
    private val predictors = sensitivities.map { s ->
            GCyclePredictor(
                this,
                PredictorParameters(
                    firstPartMinSize = 0,
                    periodMathesCount = 129,
                    s,
                )
            )
        }


    /**
     * Получает подмножество символов алфавита, который меньше threshold в текущей подпоследовательности
     */
    private fun getCurrentNotPresentAlphabetNumbers(threshold: Int = 0) =
        countsOfNumbers.mapIndexedNotNull { index, i -> if (i <= threshold) index else null }.toHashSet()

    var period: Int = 0
        private set

    /**
     * Checks that all values of alphabet is present in current subsequence
     */
    private fun checkAllValuesIsPresent(): Boolean {
        return countsOfNumbers.all { count -> count > 0 }
    }

    /**
     * Drops the oldest value in sequenceOfCurrentPeriod, decrement count of this value and decrement period
     * @return true if there is still another appearance of first value in sequenceOfCurrentPeriod.
     * false, if it was last appearance of first value sequenceOfCurrentPeriod
     */
    private fun dropOldest(): Boolean {
        val first = sequenceOfCurrentPeriod.removeFirst().toInt()
        val newCountsOfFirstValue = (countsOfNumbers[first] - 1).coerceAtLeast(0)
        countsOfNumbers[first] = newCountsOfFirstValue
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
        if (sequenceOfCurrentPeriod.size < 256)
            return

        while (dropOldest() && sequenceOfCurrentPeriod.size > 256) {
            processNewPeriod()
        }
    }

    /**
     * Добавляет следующий байт из текущей псевдослучайной последовательности
     */
    fun append(byte: UByte) {
        sequenceOfCurrentPeriod.add(byte)
        val int = byte.toInt()
        // Сначала пробуем предсказать (Отправляем предсказателям правильный следующий байт)
        predictors.forEach { p -> p.predict(int, getCurrentNotPresentAlphabetNumbers(p.params.sensitiveThreshold)) }
        // Затем инкрементируем количество добавленного символа
        countsOfNumbers[byte.toInt()]++
        // Увеличиваем длину периода
        period++
        // Если после добавления подпоследовательность содержит все символы
        // то значит мы нашли очередной g-период и отправляем его на обработку статистическим утилитам
        if (checkAllValuesIsPresent()) {
            processNewPeriod()
            checkSubSequences()
        }
    }

    fun showPredictorsResults() {
        println()
        println("___________________________________________")
        println("___________________________________________")
        predictors.sortedByDescending { it.getSuccessPredictRatio() }.forEach { p -> p.showResults() }
        //predictors.maxBy { it.getSuccessPredictRatio() }.showResults()
        println("___________________________________________")
        println("___________________________________________")
        println()
    }
}