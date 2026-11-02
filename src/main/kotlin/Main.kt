import genarator.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import statistic.PeriodProcessor
import java.io.File
import java.util.concurrent.Executors

private const val BUFFER_SIZE = 8 * 1024 * 1024

/**
 * Мощность алфавита в битах (2..32). Размер алфавита = 2^ALPHABET_BITS.
 * При значении 8 поведение эквивалентно прежнему (1 байт = 1 символ).
 */
private const val ALPHABET_BITS = 8

fun main() {

    // Generates pseudo random sequences within each Generator and writes them to files
    // By default it will generate 1GB files for by each Generator

    val currentGenTypes = listOf(Generator.ISAAC) // specified
//        Generator.entries // - all
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


private fun calculateGCycles(
    type: Generator,
    processors: List<PeriodProcessor>,
    bits: Int = ALPHABET_BITS,
): GCycleFinder {
    val iStream = File(RANDOM_SEQUENCE_PATH + type.name + SUFFIX).inputStream()
    val gCycleFinder = GCycleFinder(processors, bits)
    val mask = (1L shl bits) - 1
    // Аккумулятор битов для нарезки байтового потока на N-битные символы.
    var acc = 0L
    var nbits = 0
    var buffer = iStream.readNBytes(BUFFER_SIZE)
    while (buffer.isNotEmpty()) {
        buffer.forEach { byte ->
            acc = (acc shl 8) or (byte.toLong() and 0xFF)
            nbits += 8
            while (nbits >= bits) {
                nbits -= bits
                gCycleFinder.append((acc ushr nbits) and mask)
            }
        }
        buffer = iStream.readNBytes(BUFFER_SIZE)
    }
    iStream.close()
    return gCycleFinder
}

suspend fun calculateStatistics(type: Generator, bits: Int = ALPHABET_BITS): List<PeriodProcessor> {
    println("Statistics calculations for ${type.name} started...")
    //val mef = MathExpFinder(type)
    //val distributionProc = DistributionDataProcessor(type)
    val finder = calculateGCycles(type, listOf(), bits)
    finder.showPredictorsResults(type.name)
    //val stdDevFinder = StandardDeviationFinder(type, mef.getAvgPeriod())
    //calculateGCycles(type, listOf(stdDevFinder))
    return listOf()
}
