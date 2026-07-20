package com.medipro.manager.core.search

/**
 * Pharmacy synonym groups for POS search expansion.
 * Groups are bidirectional — any member expands to all siblings.
 */
object SynonymDictionary {

    private val GROUPS: List<Set<String>> = listOf(
        setOf("paracetamol", "parasetamol", "pcm", "acetaminophen", "para"),
        setOf("ibuprofen", "ibu", "brufen"),
        setOf("amoxicillin", "amox", "amoxycillin", "amoxicilin"),
        setOf("azithromycin", "azithro", "azithromysin", "azee"),
        setOf("cetirizine", "citrizine", "cetrizine", "citerizine"),
        setOf("omeprazole", "ome", "omez"),
        setOf("pantoprazole", "pantop", "pantocid", "panto"),
        setOf("metformin", "glucophage", "met"),
        setOf("atorvastatin", "atorva", "statin"),
        setOf("amlodipine", "amlo"),
        setOf("losartan", "los"),
        setOf("diclofenac", "diclo", "voveran"),
        setOf("aceclofenac", "aceclo"),
        setOf("levocetirizine", "levocet", "lcz"),
        setOf("montelukast", "montel", "montair"),
        setOf("salbutamol", "salb", "asthalin", "ventolin"),
        setOf("prednisolone", "predni", "pred"),
        setOf("dexamethasone", "dexa"),
        setOf("hydrochlorothiazide", "hctz", "hydro"),
        setOf("clopidogrel", "clopi"),
        setOf("ranitidine", "rani"),
        setOf("domperidone", "domper", "dom"),
        setOf("ondansetron", "ondans", "emeset"),
        setOf("metronidazole", "metro", "flagyl"),
        setOf("ciprofloxacin", "cipro"),
        setOf("cefixime", "cefi"),
        setOf("cefuroxime", "cefuro"),
        setOf("doxycycline", "doxy"),
        setOf("fluconazole", "fluc"),
        setOf("clotrimazole", "clotri"),
        setOf("betamethasone", "beta"),
        setOf("insulin", "ins"),
        setOf("glibenclamide", "glib"),
        setOf("gliclazide", "glic"),
        setOf("amlodipine", "amlodipin"),
        setOf("syrup", "syp", "sirap", "syr"),
        setOf("tablet", "tab", "tabs"),
        setOf("capsule", "cap", "caps"),
        setOf("injection", "inj", "inject"),
        setOf("suspension", "susp"),
        setOf("cream", "crm"),
        setOf("ointment", "oint"),
    )

    private val TERM_TO_GROUP: Map<String, Set<String>> = buildMap {
        GROUPS.forEach { group ->
            group.forEach { term ->
                put(term.lowercase(), group)
            }
        }
    }

    fun expandTerm(term: String): Set<String> {
        val normalized = term.lowercase().trim()
        if (normalized.isBlank()) return emptySet()
        return TERM_TO_GROUP[normalized] ?: setOf(normalized)
    }

    fun expandQuery(query: String): List<String> {
        val tokens = query.lowercase().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val expandedTokenSets = tokens.map { expandTerm(it) }
        val primary = tokens.joinToString(" ")
        val fullyExpanded = expandedTokenSets
            .map { group -> group.sortedBy { it.length }.firstOrNull() ?: "" }
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val synonymQueries = expandedTokenSets.flatten().distinct()
            .filter { it !in tokens }
            .map { synonym ->
                tokens.mapIndexed { index, token ->
                    if (token in expandTerm(synonym)) synonym else tokens[index]
                }.joinToString(" ")
            }

        return (listOf(primary, fullyExpanded) + synonymQueries)
            .distinct()
            .filter { it.isNotBlank() }
    }
}
