import genarator.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import statistic.DistributionDataProcessor
import statistic.MathExpFinder
import statistic.PeriodProcessor
import statistic.StandardDeviationFinder
import java.io.File
import java.util.concurrent.Executors


fun main() {

    // Generates pseudo random sequences within each Generator and writes them to files
    // By default it will generate 1GB files for by each Generator

    val currentGenTypes = //listOf(Generator.SHA1, Generator.DRBG) // specified
        Generator.entries // - all
//
//    currentGenTypes
//        .map { PseudoRandomGeneratorFactory.buildGenerator(it, DEFAULT_SEED, DEFAULT_SIZE_MB) }
//        .forEach { gen ->
//            gen.writeToFile()
//        }

    val pool = Executors.newFixedThreadPool(Generator.entries.size)
    val dispatcher = pool.asCoroutineDispatcher()
    val coroutines = runBlocking {
        currentGenTypes
            .map { type ->
                async(dispatcher) {
                    calculateStatistics(type)
                }
            }
    }

    runBlocking {
        coroutines.awaitAll()
    }.forEach {
        it.forEach { it.stop() }
    }

    pool.shutdown()
}


private fun calculateGCycles(type: Generator, processors: List<PeriodProcessor>): GCycleFinder {
    val iStream = File(RANDOM_SEQUENCE_PATH + type.name + SUFFIX).inputStream()
    val erg = GCycleFinder(processors)
    var buffer = iStream.readNBytes(MB)
    while (buffer.isNotEmpty()) {
        buffer.forEach { byte ->
            erg.append(byte.toUByte())
        }
        buffer = iStream.readNBytes(MB)
    }
    iStream.close()
    return erg
}

suspend fun calculateStatistics(type: Generator): List<PeriodProcessor> {
    println("Statistics calculations for ${type.name} started...")
    val mef = MathExpFinder(type)
    val distributionProc = DistributionDataProcessor(type)
    val finder = calculateGCycles(type, listOf(mef, distributionProc))
    println(type.name)
    finder.showPredictorsResults()
    //val stdDevFinder = StandardDeviationFinder(type, mef.getAvgPeriod())
    //calculateGCycles(type, listOf(stdDevFinder))
    return listOf(mef, distributionProc)
}
