import statistic.PeriodProcessor
import java.util.*

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
    private val firstPartSizes = (3000..4000 step 100)
    private val periodMatches = listOf(254)
    private val predictors = firstPartSizes.flatMap { fps ->
        periodMatches.map { period ->
            GCyclePredictor(
                this,
                PredictorParameters(
                    firstPartMinSize = fps,
                    periodMathesCount = period
                )
            )
        }
    }

    private fun getCurrentNotPresentAlphabetNumbers() =
        countsOfNumbers.mapIndexedNotNull { index, i -> if (i == 0) index else null }.toHashSet()

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

    fun append(byte: UByte) {
        sequenceOfCurrentPeriod.add(byte)
        val int = byte.toInt()
        val notPresent = getCurrentNotPresentAlphabetNumbers()
        predictors.forEach { p -> p.predict(int, notPresent) }
        countsOfNumbers[byte.toInt()]++
        period++
        if (checkAllValuesIsPresent()) {
            processNewPeriod()
            checkSubSequences()
        }
    }

    fun showPredictorsResults() {
        println()
        println("___________________________________________")
        println("___________________________________________")
        //predictors.sortedByDescending { it.getSuccessPredictRatio() }.forEach { p -> p.showResults() }
        predictors.maxBy { it.getSuccessPredictRatio() }.showResults()
        println("___________________________________________")
        println("___________________________________________")
        println()
    }
}