package com.illumined.app.ui
import java.util.Locale

internal data class RiteLibraryEntry(val id: String, val en: String, val es: String) {
    val title: String get() = if (Locale.getDefault().language == "es") es else en
}
internal data class RiteLibraryGroup(val id: String, val en: String, val es: String, val entries: List<RiteLibraryEntry>) {
    val title: String get() = if (Locale.getDefault().language == "es") es else en
}
// Names only; no generated liturgical content. Keep IDs aligned across platforms.
internal object RitePreparationLibrary {
    val groups = listOf(
        RiteLibraryGroup("catechumenate", "Catechumenate — unbaptized", "Catecumenado — no bautizados", listOf(
            RiteLibraryEntry("acceptance", "Rite of Acceptance / Entrance into the Catechumenate", "Rito de aceptación / entrada en el catecumenado"),
            RiteLibraryEntry("word", "Celebrations of the Word of God", "Celebraciones de la palabra de Dios"),
            RiteLibraryEntry("minor-exorcisms", "Minor Exorcisms", "Exorcismos menores"),
            RiteLibraryEntry("blessings", "Blessings of Catechumens", "Bendiciones de los catecúmenos"),
            RiteLibraryEntry("anointing", "Anointing with the Oil of Catechumens", "Unción con el óleo de los catecúmenos"),
            RiteLibraryEntry("dismissal", "Dismissal of Catechumens", "Despedida de los catecúmenos")
        )),
        RiteLibraryGroup("election", "Election — unbaptized", "Elección — no bautizados", listOf(
            RiteLibraryEntry("sending", "Sending Catechumens for Election", "Envío de los catecúmenos para la elección"),
            RiteLibraryEntry("election", "Election / Enrollment of Names", "Elección / inscripción del nombre")
        )),
        RiteLibraryGroup("purification", "Purification and Enlightenment — the elect", "Purificación e iluminación — los elegidos", listOf(
            RiteLibraryEntry("scrutiny-1", "First Scrutiny", "Primer escrutinio"),
            RiteLibraryEntry("scrutiny-2", "Second Scrutiny", "Segundo escrutinio"),
            RiteLibraryEntry("scrutiny-3", "Third Scrutiny", "Tercer escrutinio"),
            RiteLibraryEntry("creed", "Handing On / Presentation of the Creed", "Entrega del Símbolo (Credo)"),
            RiteLibraryEntry("lords-prayer", "Handing On / Presentation of the Lord’s Prayer", "Entrega de la Oración del Señor (Padrenuestro)")
        )),
        RiteLibraryGroup("immediate", "Immediate Preparation — before initiation", "Preparación inmediata — antes de la iniciación", listOf(
            RiteLibraryEntry("recitation", "Recitation / Giving Back of the Creed", "Recitación / devolución del Símbolo"),
            RiteLibraryEntry("ephphatha", "Ephphatha Rite", "Rito del Effetá"),
            RiteLibraryEntry("christian-name", "Choosing a Christian Name", "Elección de un nombre cristiano"),
            RiteLibraryEntry("immediate-anointing", "Anointing with the Oil of Catechumens — immediate preparation", "Unción con el óleo de los catecúmenos — preparación inmediata")
        )),
        RiteLibraryGroup("initiation", "Sacraments of Initiation", "Sacramentos de la iniciación", listOf(
            RiteLibraryEntry("initiation", "Celebration of the Sacraments of Initiation", "Celebración de los sacramentos de la iniciación"),
            RiteLibraryEntry("baptism", "Baptism", "Bautismo"),
            RiteLibraryEntry("confirmation", "Confirmation", "Confirmación"),
            RiteLibraryEntry("eucharist", "First Holy Communion", "Primera Comunión")
        )),
        RiteLibraryGroup("baptized", "Baptized Candidates — as pastorally appropriate", "Candidatos bautizados — según corresponda pastoralmente", listOf(
            RiteLibraryEntry("welcoming", "Welcoming Baptized Candidates", "Acogida de los candidatos bautizados"),
            RiteLibraryEntry("sending-candidates", "Sending Candidates for Recognition by the Bishop", "Envío de los candidatos para su reconocimiento por el obispo"),
            RiteLibraryEntry("conversion", "Call to Continuing Conversion", "Llamado a la conversión continua"),
            RiteLibraryEntry("penitential", "Penitential Rite — baptized candidates", "Rito penitencial — candidatos bautizados"),
            RiteLibraryEntry("reconciliation", "Sacrament of Reconciliation — baptized participants", "Sacramento de la Reconciliación — participantes bautizados"),
            RiteLibraryEntry("reception-mass", "Reception into Full Communion — within Mass", "Recepción en la plena comunión — dentro de la Misa"),
            RiteLibraryEntry("reception-outside", "Reception into Full Communion — outside Mass", "Recepción en la plena comunión — fuera de la Misa")
        )),
        RiteLibraryGroup("combined", "Combined Celebrations — parish and diocesan guidance", "Celebraciones conjuntas — orientación parroquial y diocesana", listOf(
            RiteLibraryEntry("combined-entrance", "Entrance into the Catechumenate and Welcoming Candidates", "Entrada en el catecumenado y acogida de candidatos"),
            RiteLibraryEntry("combined-sending", "Combined Sending of Catechumens and Candidates", "Envío conjunto de catecúmenos y candidatos"),
            RiteLibraryEntry("combined-election", "Election and Call to Continuing Conversion", "Elección y llamado a la conversión continua"),
            RiteLibraryEntry("combined-initiation", "Initiation and Reception into Full Communion", "Iniciación y recepción en la plena comunión")
        )),
        RiteLibraryGroup("children", "Children of Catechetical Age — adapted rites", "Niños en edad catequética — ritos adaptados", listOf(
            RiteLibraryEntry("child-entrance", "Entrance into the Catechumenate — children", "Entrada en el catecumenado — niños"),
            RiteLibraryEntry("child-election", "Election — children, where celebrated", "Elección — niños, donde se celebre"),
            RiteLibraryEntry("child-penitential", "Penitential Rites / Scrutinies — children", "Ritos penitenciales / escrutinios — niños"),
            RiteLibraryEntry("child-initiation", "Sacraments of Initiation — children", "Sacramentos de la iniciación — niños")
        )),
        RiteLibraryGroup("exceptional", "Exceptional Circumstances — clergy direction required", "Circunstancias excepcionales — dirección del clero requerida", listOf(
            RiteLibraryEntry("simple", "Simpler Order of Adult Initiation", "Forma simplificada de la iniciación de adultos"),
            RiteLibraryEntry("danger", "Initiation in Danger of Death", "Iniciación en peligro de muerte")
        ))
    )
}
