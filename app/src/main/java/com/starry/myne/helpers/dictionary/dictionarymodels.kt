// app/.../reader/dictionary/DictionaryModels.kt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DictionaryEntry(
    val word: String,
    val phonetic: String? = null,
    val phonetics: List<Phonetic> = emptyList(),
    val meanings: List<Meaning> = emptyList()
)

@Serializable
data class Phonetic(val text: String? = null, val audio: String? = null)

@Serializable
data class Meaning(
    @SerialName("partOfSpeech") val partOfSpeech: String? = null,
    val definitions: List<Definition> = emptyList()
)

@Serializable
data class Definition(
    val definition: String,
    val example: String? = null
)
