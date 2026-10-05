import Foundation

struct RiteLibraryEntry: Identifiable {
    let id: String
    let en: String
    let es: String
    var title: String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
}
struct RiteLibraryGroup: Identifiable {
    let id: String
    let en: String
    let es: String
    let overviewEn: String
    let overviewEs: String
    let entries: [RiteLibraryEntry]
    var title: String { Locale.current.language.languageCode?.identifier == "es" ? es : en }
    var overview: String { Locale.current.language.languageCode?.identifier == "es" ? overviewEs : overviewEn }
}
// Keep stage order, rite IDs, and bilingual overviews aligned with the web catalog.
enum RitePreparationLibrary {
    static let groups: [RiteLibraryGroup] = [
        RiteLibraryGroup(
            id: "precatechumenate", en: "Period: Precatechumenate", es: "Período: Precatecumenado",
            overviewEn: "A time of welcome, inquiry, and first conversion. Help inquirers encounter Jesus Christ, ask questions freely, and begin sharing in parish life.\n\nThe parish offers a welcoming place to explore faith without pressure to move at a fixed pace. Simple proclamation of the Gospel, conversation, prayer, and relationships help the inquirer recognize the beginnings of faith and repentance. The instructor listens to questions and helps the person find an appropriate sponsor. This period has no rite to publish here; readiness for the next step is discerned with the parish team.",
            overviewEs: "Un tiempo de acogida, búsqueda y conversión inicial. Ayuda a los interesados a encontrar a Jesucristo, hacer preguntas con libertad y comenzar a participar en la vida parroquial.\n\nLa parroquia ofrece un espacio acogedor para explorar la fe sin presionar a seguir un ritmo fijo. El anuncio sencillo del Evangelio, el diálogo, la oración y las relaciones ayudan a reconocer los comienzos de la fe y del arrepentimiento. El instructor escucha y ayuda a encontrar un acompañante apropiado. Aquí no hay un rito para publicar; el siguiente paso se discierne con el equipo parroquial.",
            entries: [

            ]),
        RiteLibraryGroup(
            id: "acceptance-transition", en: "Rite of Acceptance & Welcoming", es: "Rito de aceptación y acogida",
            overviewEn: "Acceptance marks entrance into the catechumenate for the unbaptized. Welcoming concerns baptized candidates; choose the appropriate template with your parish clergy.\n\nThis public step expresses the participant’s desire to continue the journey and the community’s commitment to accompany them. It should not be treated simply as completing a course. Prepare participants and sponsors for their roles, and confirm the particular celebration with clergy. Acceptance and welcoming are not interchangeable: select the template that respects each person’s baptismal status.",
            overviewEs: "La aceptación marca la entrada en el catecumenado para los no bautizados. La acogida corresponde a candidatos bautizados; elige la plantilla apropiada con el clero parroquial.\n\nEste paso público expresa el deseo de continuar el camino y el compromiso de la comunidad de acompañar. No debe entenderse simplemente como terminar un curso. Prepara a participantes y acompañantes para sus funciones y confirma la celebración con el clero. La aceptación y la acogida no son intercambiables: selecciona la plantilla que respete la situación bautismal de cada persona.",
            entries: [
                RiteLibraryEntry(id: "acceptance", en: "Rite of Acceptance / Entrance into the Catechumenate", es: "Rito de aceptación / entrada en el catecumenado"),
                RiteLibraryEntry(id: "welcoming", en: "Welcoming Baptized Candidates", es: "Acogida de los candidatos bautizados"),
                RiteLibraryEntry(id: "child-entrance", en: "Entrance into the Catechumenate — children", es: "Entrada en el catecumenado — niños"),
                RiteLibraryEntry(id: "combined-entrance", en: "Entrance into the Catechumenate and Welcoming Candidates", es: "Entrada en el catecumenado y acogida de candidatos")
            ]),
        RiteLibraryGroup(
            id: "catechumenate", en: "Period: Catechumenate", es: "Período: Catecumenado",
            overviewEn: "A sustained period of formation in faith and Christian living. Scripture, prayer, community, service, and the minor rites accompany growing conversion.\n\nFormation is more than learning information: it involves practicing the Christian life with the community. The instructor connects teaching with prayer, worship, relationships, and service, while sponsors offer personal accompaniment. The minor rites support this journey and are arranged with clergy according to pastoral need. Open the rite cards below to prepare material; these are not a list of requirements that every participant must complete.",
            overviewEs: "Un período de formación en la fe y la vida cristiana. La Escritura, la oración, la comunidad, el servicio y los ritos menores acompañan la conversión.\n\nLa formación va más allá de aprender información: implica practicar la vida cristiana con la comunidad. El instructor relaciona la enseñanza con la oración, el culto, las relaciones y el servicio; los acompañantes ofrecen apoyo personal. Los ritos menores sostienen este camino y se organizan con el clero según la necesidad pastoral. Las tarjetas siguientes permiten preparar material; no son requisitos que todos deban completar.",
            entries: [
                RiteLibraryEntry(id: "word", en: "Celebrations of the Word of God", es: "Celebraciones de la palabra de Dios"),
                RiteLibraryEntry(id: "minor-exorcisms", en: "Minor Exorcisms", es: "Exorcismos menores"),
                RiteLibraryEntry(id: "blessings", en: "Blessings of Catechumens", es: "Bendiciones de los catecúmenos"),
                RiteLibraryEntry(id: "anointing", en: "Anointing with the Oil of Catechumens", es: "Unción con el óleo de los catecúmenos"),
                RiteLibraryEntry(id: "dismissal", en: "Dismissal of Catechumens", es: "Despedida de los catecúmenos"),
                RiteLibraryEntry(id: "sending", en: "Sending Catechumens for Election", es: "Envío de los catecúmenos para la elección"),
                RiteLibraryEntry(id: "combined-sending", en: "Combined Sending of Catechumens and Candidates", es: "Envío conjunto de catecúmenos y candidatos")
            ]),
        RiteLibraryGroup(
            id: "election-transition", en: "Rite of Election & Call to Continuing Conversion", es: "Rito de elección y llamado a la conversión continua",
            overviewEn: "A transition toward final preparation for initiation. Election concerns catechumens; already baptized candidates follow their appropriate celebration.\n\nThe parish works with diocesan guidance to prepare those approaching initiation. Sponsors and the community accompany this discernment, rather than relying on attendance or lesson completion alone. Explain the meaning of the celebration and practical arrangements beforehand. Baptized candidates are not elected as catechumens; the appropriate celebration recognizes their distinct journey.",
            overviewEs: "Una transición hacia la preparación final para la iniciación. La elección corresponde a los catecúmenos; los candidatos bautizados siguen su celebración apropiada.\n\nLa parroquia sigue la orientación diocesana para preparar a quienes se acercan a la iniciación. Los acompañantes y la comunidad participan en el discernimiento, sin basarse solo en asistencia o lecciones completadas. Explica previamente el significado y los detalles prácticos. Los candidatos bautizados no son elegidos como catecúmenos; su celebración reconoce un camino distinto.",
            entries: [
                RiteLibraryEntry(id: "election", en: "Election / Enrollment of Names", es: "Elección / inscripción del nombre"),
                RiteLibraryEntry(id: "sending-candidates", en: "Sending Candidates for Recognition by the Bishop", es: "Envío de los candidatos para su reconocimiento por el obispo"),
                RiteLibraryEntry(id: "conversion", en: "Call to Continuing Conversion", es: "Llamado a la conversión continua"),
                RiteLibraryEntry(id: "child-election", en: "Election — children, where celebrated", es: "Elección — niños, donde se celebre"),
                RiteLibraryEntry(id: "combined-election", en: "Election and Call to Continuing Conversion", es: "Elección y llamado a la conversión continua")
            ]),
        RiteLibraryGroup(
            id: "purification", en: "Period: Purification and Enlightenment", es: "Período: Purificación e iluminación",
            overviewEn: "A period of spiritual preparation, ordinarily during Lent, emphasizing prayer and conversion before initiation. Select rites according to baptismal status and parish guidance.\n\nThe emphasis shifts from broad instruction toward deeper prayer and preparation of the heart. Help participants reflect on conversion, trust in Christ, and the life they are preparing to embrace. Coordinate the scrutinies, presentations, and immediate preparation with clergy, explaining what participants will experience without turning the rites into a performance. The preparation of already baptized participants must respect their different circumstances.",
            overviewEs: "Un período de preparación espiritual, normalmente durante la Cuaresma, que destaca la oración y la conversión antes de la iniciación. Selecciona los ritos según la situación bautismal y la orientación parroquial.\n\nEl énfasis pasa de la instrucción amplia a una oración más profunda y a la preparación del corazón. Ayuda a reflexionar sobre la conversión, la confianza en Cristo y la vida que se disponen a abrazar. Coordina con el clero los escrutinios, las entregas y la preparación inmediata, explicando lo que vivirán sin convertir los ritos en una actuación. La preparación de los bautizados debe respetar sus circunstancias distintas.",
            entries: [
                RiteLibraryEntry(id: "scrutiny-1", en: "First Scrutiny", es: "Primer escrutinio"),
                RiteLibraryEntry(id: "scrutiny-2", en: "Second Scrutiny", es: "Segundo escrutinio"),
                RiteLibraryEntry(id: "scrutiny-3", en: "Third Scrutiny", es: "Tercer escrutinio"),
                RiteLibraryEntry(id: "creed", en: "Handing On / Presentation of the Creed", es: "Entrega del Símbolo (Credo)"),
                RiteLibraryEntry(id: "lords-prayer", en: "Handing On / Presentation of the Lord’s Prayer", es: "Entrega de la Oración del Señor (Padrenuestro)"),
                RiteLibraryEntry(id: "recitation", en: "Recitation / Giving Back of the Creed", es: "Recitación / devolución del Símbolo"),
                RiteLibraryEntry(id: "ephphatha", en: "Ephphatha Rite", es: "Rito del Effetá"),
                RiteLibraryEntry(id: "christian-name", en: "Choosing a Christian Name", es: "Elección de un nombre cristiano"),
                RiteLibraryEntry(id: "immediate-anointing", en: "Anointing with the Oil of Catechumens — immediate preparation", es: "Unción con el óleo de los catecúmenos — preparación inmediata"),
                RiteLibraryEntry(id: "penitential", en: "Penitential Rite — baptized candidates", es: "Rito penitencial — candidatos bautizados"),
                RiteLibraryEntry(id: "reconciliation", en: "Sacrament of Reconciliation — baptized participants", es: "Sacramento de la Reconciliación — participantes bautizados"),
                RiteLibraryEntry(id: "child-penitential", en: "Penitential Rites / Scrutinies — children", es: "Ritos penitenciales / escrutinios — niños")
            ]),
        RiteLibraryGroup(
            id: "initiation", en: "Celebration: Sacraments of Initiation", es: "Celebración: Sacramentos de la iniciación",
            overviewEn: "Prepare for the celebration of initiation with the parish clergy. The unbaptized and those already baptized follow distinct sacramental paths.\n\nThe celebration welcomes participants into a fuller sharing in the sacramental life of the Church. Work with clergy to determine the proper rites for each person; someone already baptized is not baptized again. Clear explanations of actions, responses, sponsors’ roles, and practical arrangements can ease uncertainty and support prayerful participation. Plan continued accompaniment beyond the celebration, not merely the event itself.",
            overviewEs: "Prepara la celebración de la iniciación con el clero parroquial. Los no bautizados y quienes ya están bautizados siguen caminos sacramentales distintos.\n\nLa celebración introduce a los participantes en una participación más plena en la vida sacramental de la Iglesia. Determina con el clero los ritos apropiados para cada persona; quien ya está bautizado no vuelve a bautizarse. Explicar las acciones, respuestas, funciones de los padrinos y detalles prácticos ayuda a participar con serenidad y oración. Planifica el acompañamiento después de la celebración, no solo el evento.",
            entries: [
                RiteLibraryEntry(id: "initiation", en: "Celebration of the Sacraments of Initiation", es: "Celebración de los sacramentos de la iniciación"),
                RiteLibraryEntry(id: "baptism", en: "Baptism", es: "Bautismo"),
                RiteLibraryEntry(id: "confirmation", en: "Confirmation", es: "Confirmación"),
                RiteLibraryEntry(id: "eucharist", en: "First Holy Communion", es: "Primera Comunión"),
                RiteLibraryEntry(id: "reception-mass", en: "Reception into Full Communion — within Mass", es: "Recepción en la plena comunión — dentro de la Misa"),
                RiteLibraryEntry(id: "reception-outside", en: "Reception into Full Communion — outside Mass", es: "Recepción en la plena comunión — fuera de la Misa"),
                RiteLibraryEntry(id: "combined-initiation", en: "Initiation and Reception into Full Communion", es: "Iniciación y recepción en la plena comunión"),
                RiteLibraryEntry(id: "child-initiation", en: "Sacraments of Initiation — children", es: "Sacramentos de la iniciación — niños"),
                RiteLibraryEntry(id: "simple", en: "Simpler Order of Adult Initiation", es: "Forma simplificada de la iniciación de adultos"),
                RiteLibraryEntry(id: "danger", en: "Initiation in Danger of Death", es: "Iniciación en peligro de muerte")
            ]),
        RiteLibraryGroup(
            id: "neophyte-year", en: "Period: Neophyte Year", es: "Período: Año de los neófitos",
            overviewEn: "Continue accompaniment after initiation through mystagogy: reflection on the sacraments, participation in parish life, and growth in Christian discipleship.\n\nInitiation is a beginning, not a graduation from parish life. Provide opportunities to reflect on the sacramental experience in light of Scripture and everyday life. Continue contact with sponsors and help new members build lasting relationships, participate in worship, and discover ways to serve. The year offers space for new questions and steady support as faith becomes more deeply integrated into daily living.",
            overviewEs: "Continúa el acompañamiento después de la iniciación mediante la mistagogía: reflexión sobre los sacramentos, participación parroquial y crecimiento en el discipulado cristiano.\n\nLa iniciación es un comienzo, no una graduación de la vida parroquial. Ofrece ocasiones para reflexionar sobre la experiencia sacramental a la luz de la Escritura y de la vida cotidiana. Mantén el contacto con los padrinos y ayuda a crear relaciones duraderas, participar en el culto y descubrir formas de servir. El año ofrece espacio para nuevas preguntas y apoyo constante al integrar la fe en la vida diaria.",
            entries: [

            ])
    ]
}
