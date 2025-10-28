package statistic

import genarator.Generator
import java.io.File
import java.io.OutputStream

const val DISTRIBUTION_CSV_OUTPUT_PATH = "distribution/"

class DistributionDataProcessor(val generator: Generator) : PeriodProcessor {
    private val gCyclesCountsByPeriod: MutableMap<Int, Long> = mutableMapOf()

    override fun processNewPeriod(period: Int) {
        gCyclesCountsByPeriod[period] = gCyclesCountsByPeriod.getOrDefault(period, 0L) + 1L
    }

    override fun stop() {
        val file = File("$DISTRIBUTION_CSV_OUTPUT_PATH${generator.name}_distribution.csv")
        file.createNewFile()
        file.outputStream().use {
            it.writeCsv(gCyclesCountsByPeriod.map { Point(it.key, it.value) })
        }
    }

}

data class Point(
    val period: Int,
    val count: Long,
)

fun OutputStream.writeCsv(movies: List<Point>) {
    val writer = bufferedWriter()
    writer.write(""""period", "count"""")
    writer.newLine()
    movies.forEach {
        writer.write("${it.period}, ${it.count}")
        writer.newLine()
    }
    writer.flush()
}