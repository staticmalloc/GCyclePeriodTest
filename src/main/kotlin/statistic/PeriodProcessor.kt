package statistic

interface PeriodProcessor {
    fun processNewPeriod(period: Int)

    fun stop()
}