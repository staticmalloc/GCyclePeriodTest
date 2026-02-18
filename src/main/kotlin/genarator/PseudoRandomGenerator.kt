package genarator

import java.io.File

const val DEFAULT_SEED =
    "Мороз и солнце, день чудесный! " +
            "Еще ты дремлешь друг прелестный! " +
            "Пора, красавица, проснись! " +
            "Открой сомкнуты негой взоры, " +
            "На встречу северной авроры, " +
            "Звездою севера, явись!"

const val DEFAULT_SIZE_MB = 1024
const val RANDOM_SEQUENCE_PATH = "sequences/"
const val SUFFIX = "_100MB.txt"
const val MB = 1024 * 1024


enum class Generator {
    TEST,
    LCG,
    MERSENNE,
    ISAAC,
    WELL1024A,
    WELL44497B,
    SHA1,
    DRBG
}


object PseudoRandomGeneratorFactory {

    fun buildGenerators(seed: String = DEFAULT_SEED, sizeMB: Int = DEFAULT_SIZE_MB): List<PseudoRandomGenerator> =
        Generator.entries.map { buildGenerator(it, seed, sizeMB) }

    fun buildGenerator(
        type: Generator,
        seed: String = DEFAULT_SEED,
        sizeMB: Int = DEFAULT_SIZE_MB
    ): PseudoRandomGenerator = when (type) {
        Generator.TEST -> TestGenerator()
        Generator.LCG -> LCG()
        Generator.MERSENNE -> Mersenne()
        Generator.ISAAC -> ISAAC()
        Generator.WELL1024A -> WELL1024A()
        Generator.WELL44497B -> WELL44497B()
        Generator.SHA1 -> SHA1()
        Generator.DRBG -> DRBG()
    }
        .setSeed(seed)
        .setSize(sizeMB)
}


sealed class PseudoRandomGenerator(
    val type: Generator,
    private var sizeMB: Int = DEFAULT_SIZE_MB
) {

    fun setSize(sizeMB: Int): PseudoRandomGenerator {
        this.sizeMB = sizeMB
        return this
    }

    abstract fun setSeed(seed: String): PseudoRandomGenerator

    abstract fun generateMB(): ByteArray

    fun writeToFile(): File {
        val file = File(RANDOM_SEQUENCE_PATH + type.name + SUFFIX)
        file.writeBytes(byteArrayOf())
        for (i in 0..<sizeMB)
            file.appendBytes(generateMB())
        return file
    }

}