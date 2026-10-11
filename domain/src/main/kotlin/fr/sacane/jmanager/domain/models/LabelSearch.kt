package fr.sacane.jmanager.domain.models

import java.text.Normalizer

/**
 * A free-text fragment looked for in transaction labels, regardless of case and accents:
 * "peage" finds "Péage autoroute". The fragment is matched as a whole, spaces included.
 * A blank fragment matches every label.
 */
class LabelSearch private constructor(private val fragment: String) {

    val isEmpty: Boolean get() = fragment.isEmpty()

    fun matches(label: String): Boolean = isEmpty || normalise(label).contains(fragment)

    companion object {
        private val COMBINING_MARKS = Regex("""\p{M}+""")

        fun of(fragment: String?): LabelSearch = LabelSearch(normalise(fragment.orEmpty().trim()))

        private fun normalise(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(COMBINING_MARKS, "").lowercase()
    }
}
