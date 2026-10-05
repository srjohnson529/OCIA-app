package com.illumined.app.ui

internal data class MassPrayerOption(
    val id: String,
    val title: String,
    val summary: String,
    val fullText: String,
    val note: String? = null,
    val textNote: String? = null,
) {
    val localizedTitle get() = if (java.util.Locale.getDefault().language == "es") when (id) {
        "confiteor" -> "Acto penitencial: Yo confieso"; "dialogue" -> "Acto penitencial: Diálogo"
        "tropes" -> "Acto penitencial: Invocaciones con el Kyrie"; "sprinkling" -> "Rito de la aspersión"
        "gloria" -> "Gloria"; "collect" -> "Oración colecta"; "nicene" -> "Credo niceno-constantinopolitano"
        "apostles" -> "Credo de los Apóstoles"; "universal-prayer" -> "Oración universal"
        "presentation-gifts" -> "Preparación de los dones"; "prayer-over-offerings" -> "Oración sobre las ofrendas"
        "preface-dialogue" -> "Diálogo del prefacio"; "ep1" -> "Plegaria eucarística I: Canon romano"
        "ep2" -> "Plegaria eucarística II"; "ep3" -> "Plegaria eucarística III"; "ep4" -> "Plegaria eucarística IV"
        "sanctus" -> "Santo, Santo, Santo"; "memorial-acclamation" -> "Aclamaciones memoriales"
        "great-amen" -> "Gran Amén"; "lords-prayer" -> "Padre nuestro"; "agnus-dei" -> "Cordero de Dios"
        "communion-invitation" -> "Invitación a la Comunión"; "prayer-after-communion" -> "Oración después de la Comunión"
        "final-blessing" -> "Bendición final"; "dismissal" -> "Despedida"; else -> title
    } else title
    val localizedSummary get() = if (java.util.Locale.getDefault().language == "es") when (id) {
        "confiteor" -> "El pueblo confiesa unido sus pecados, reconoce a los santos y a la comunidad, y pide oración y misericordia."
        "dialogue" -> "El sacerdote guía breves invocaciones y el pueblo pide al Señor misericordia y salvación."
        "tropes" -> "Se invoca a Cristo con breves títulos y el pueblo responde pidiendo misericordia."
        "sprinkling" -> "Especialmente en Pascua, el sacerdote puede bendecir y asperjar al pueblo con agua bendita como recuerdo del Bautismo."
        "gloria" -> "Himno de alabanza que se canta normalmente los domingos fuera de Adviento y Cuaresma, y en solemnidades y fiestas."
        "collect" -> "Oración propia del día que reúne la oración de la Iglesia y la dirige a Dios."
        "nicene" -> "Profesión de fe dominical que proclama la Trinidad, la Encarnación, la Iglesia, el Bautismo, la Resurrección y la vida eterna."
        "apostles" -> "Credo bautismal más breve que puede usarse en determinados tiempos, especialmente Cuaresma y Pascua."
        "universal-prayer" -> "Peticiones después del Credo, también llamadas Oración de los fieles."
        "presentation-gifts" -> "Se preparan el pan y el vino, y la ofrenda del pueblo se une al sacrificio de Cristo."
        "prayer-over-offerings" -> "El sacerdote pide que Dios reciba y santifique los dones preparados para la Eucaristía."
        "preface-dialogue" -> "El sacerdote invita al pueblo a elevar el corazón y dar gracias al Señor."
        "ep1" -> "El antiguo Canon romano, de carácter solemne, con amplias conmemoraciones e intercesiones."
        "ep2" -> "Plegaria eucarística concisa con acción de gracias, epíclesis, relato de la institución, memorial, ofrenda e intercesión."
        "ep3" -> "Plegaria frecuente en domingos y fiestas que destaca el sacrificio de Cristo y la unidad de los fieles."
        "ep4" -> "Plegaria con prefacio propio que recorre la historia de la salvación desde la creación hasta Cristo y el Espíritu."
        "sanctus" -> "Aclamación anterior a la Plegaria eucarística que une la alabanza de ángeles y santos."
        "memorial-acclamation" -> "El pueblo aclama el misterio de la fe después de la consagración."
        "great-amen" -> "El pueblo confirma solemnemente la Plegaria eucarística al concluir."
        "lords-prayer" -> "La oración enseñada por Jesús, rezada por toda la Iglesia en el Rito de la Comunión."
        "agnus-dei" -> "Letanía cantada o recitada durante la fracción del pan antes de la Comunión."
        "communion-invitation" -> "El sacerdote muestra la Eucaristía e invita a los fieles a la cena del Cordero."
        "prayer-after-communion" -> "El sacerdote pide que el sacramento recibido dé fruto en la vida de los fieles."
        "final-blessing" -> "El sacerdote bendice a los fieles antes de enviarlos."
        "dismissal" -> "El pueblo es enviado a vivir el misterio que ha celebrado."; else -> summary
    } else summary
    val localizedNote get() = if (java.util.Locale.getDefault().language == "es" && note != null) "Consulta el misal parroquial o el subsidio litúrgico aprobado." else note
    val localizedTextNote get() = if (java.util.Locale.getDefault().language == "es") "Para el texto litúrgico oficial en español, usa un Misal Romano o subsidio aprobado." else textNote
    val localizedTextHeading: String get() = if (java.util.Locale.getDefault().language == "es") "Texto y guía" else if (fullText.startsWith("Full official text")) "Text Placeholder" else "Prayer Text"
    val textHeading: String get() = if (fullText.startsWith("Full official text")) "Text Placeholder" else "Prayer Text"
}

internal data class MassGuideRow(
    val title: String,
    val detail: String,
    val posture: String? = null,
    val response: String? = null,
    val prayerIds: List<String> = emptyList(),
) {
    val localizedTitle get() = if (java.util.Locale.getDefault().language == "es") when (title) {
        "Entrance" -> "Entrada"; "Sign of the Cross and Greeting" -> "Señal de la cruz y saludo"
        "Penitential Act" -> "Acto penitencial"; "Gloria" -> "Gloria"; "Collect" -> "Oración colecta"
        "First Reading" -> "Primera lectura"; "Responsorial Psalm" -> "Salmo responsorial"
        "Second Reading" -> "Segunda lectura"; "Gospel Acclamation and Gospel" -> "Aclamación al Evangelio y Evangelio"
        "Homily" -> "Homilía"; "Profession of Faith" -> "Profesión de fe"; "Universal Prayer" -> "Oración universal"
        "Preparation of the Gifts" -> "Preparación de los dones"
        "Prayer over the Offerings" -> "Oración sobre las ofrendas"
        "Preface Dialogue" -> "Diálogo del prefacio"
        "Eucharistic Prayer" -> "Plegaria eucarística"
        "Holy, Holy, Holy" -> "Santo, Santo, Santo"
        "Institution Narrative and Consecration" -> "Relato de la institución y consagración"
        "Memorial Acclamation" -> "Aclamación memorial"
        "Great Amen" -> "Gran Amén"
        "Lord’s Prayer" -> "Padre nuestro"
        "Sign of Peace" -> "Rito de la paz"
        "Lamb of God" -> "Cordero de Dios"
        "Holy Communion" -> "Sagrada Comunión"
        "Prayer after Communion" -> "Oración después de la Comunión"
        "Announcements" -> "Avisos"
        "Blessing" -> "Bendición"
        "Dismissal" -> "Despedida"
        "Recessional" -> "Procesión de salida"
        else -> title
    } else title
    val localizedDetail get() = if (java.util.Locale.getDefault().language == "es") when (title) {
        "Entrance" -> "La entrada es la procesión y rito inicial de la Misa católica. El sacerdote, el diácono y los servidores del altar avanzan hacia el altar. Esto simboliza nuestro camino hacia el cielo. Un canto de entrada une a la asamblea en la alabanza."
        "Sign of the Cross and Greeting" -> "La Misa comienza en el nombre del Padre, y del Hijo, y del Espíritu Santo."
        "Penitential Act" -> "El Acto penitencial tiene lugar al comienzo de la Misa y prepara a los fieles para celebrar dignamente los sagrados misterios, reconociendo sus pecados y pidiendo la misericordia de Dios. Puede adoptar varias formas: el Yo confieso, un diálogo, invocaciones con el Kyrie eleison o la aspersión con agua."
        "Gloria" -> "El Gloria es un antiguo himno gozoso de alabanza y adoración. Glorifica a la Trinidad, une el canto de los ángeles en el nacimiento de Jesús con la acción de gracias y pide misericordia. Se canta los domingos fuera de Adviento y Cuaresma, y en solemnidades y fiestas."
        "Collect" -> "La oración colecta concluye los Ritos iniciales antes de la Liturgia de la Palabra. Reúne las oraciones e intenciones silenciosas de la asamblea en una petición unificada ofrecida a Dios."
        "First Reading" -> "Normalmente se toma del Antiguo Testamento, excepto durante la Pascua, cuando suele leerse el libro de los Hechos."
        "Responsorial Psalm" -> "El pueblo responde a la Palabra de Dios mediante una oración cantada o recitada."
        "Second Reading" -> "Los domingos y solemnidades suele tomarse de una carta apostólica o del Apocalipsis."
        "Gospel Acclamation and Gospel" -> "La asamblea se pone de pie para acoger a Cristo que habla en el Evangelio."
        "Homily" -> "La homilía es la predicación del sacerdote o diácono durante la Liturgia de la Palabra. Explica las lecturas y ayuda a la asamblea a aplicar la Palabra de Dios a la vida diaria."
        "Profession of Faith" -> "La Profesión de fe, o Credo, es la declaración solemne de las creencias fundamentales que se recita después de la homilía. Une a la asamblea en una misma fe y responde a la Palabra de Dios."
        "Universal Prayer" -> "La Oración universal, también llamada Oración de los fieles, reúne peticiones por la Iglesia, los gobernantes, los enfermos y el mundo."
        "Preparation of the Gifts" -> "El pan, el vino y la ofrenda del pueblo se llevan al altar."
        "Prayer over the Offerings" -> "El sacerdote pide que Dios reciba y santifique los dones."
        "Preface Dialogue" -> "El sacerdote invita al pueblo a elevar el corazón y dar gracias."
        "Eucharistic Prayer" -> "La Iglesia da gracias, invoca al Espíritu Santo, recuerda el sacrificio salvador de Cristo y presenta sus intercesiones."
        "Holy, Holy, Holy" -> "La Iglesia se une a los ángeles y santos en la alabanza antes de la consagración."
        "Institution Narrative and Consecration" -> "Por las palabras de Cristo y el poder del Espíritu Santo, el pan y el vino se convierten en el Cuerpo y la Sangre de Cristo."
        "Memorial Acclamation" -> "La asamblea proclama el misterio de la muerte y resurrección de Cristo."
        "Great Amen" -> "El pueblo confirma toda la Plegaria eucarística con un Amén solemne."
        "Lord’s Prayer" -> "La Iglesia reza la oración que Jesús nos enseñó."
        "Sign of Peace" -> "Los fieles expresan la paz y la caridad antes de recibir la Comunión."
        "Lamb of God" -> "La Iglesia invoca a Cristo, el Cordero que quita el pecado del mundo."
        "Holy Communion" -> "Quienes están debidamente dispuestos reciben el Cuerpo y la Sangre de Cristo."
        "Prayer after Communion" -> "El sacerdote pide que el sacramento dé fruto en la vida de los fieles."
        "Announcements" -> "Después de la Comunión pueden darse breves avisos parroquiales."
        "Blessing" -> "El sacerdote bendice a los fieles en el nombre de la Trinidad."
        "Dismissal" -> "El pueblo es enviado a glorificar al Señor con su vida."
        "Recessional" -> "Los ministros se retiran y los fieles salen para vivir el misterio que han recibido."
        else -> detail
    } else detail
    val localizedPosture get() = if (java.util.Locale.getDefault().language == "es") when (posture) {
        "Stand" -> "De pie"; "Sit" -> "Sentado"; "Kneel" -> "De rodillas"
        "Stand/Kneel" -> "De pie/De rodillas"; "Kneel/Stand" -> "De rodillas/De pie"
        "Sit/Stand" -> "Sentado/De pie"; "Process" -> "Procesión"; else -> posture
    } else posture
    val localizedResponse get() = if (java.util.Locale.getDefault().language == "es") when (response) {
        "Amen." -> "Amén."; "Thanks be to God." -> "Te alabamos, Señor."
        "And with your spirit." -> "Y con tu espíritu."
        "Amen. / And with your spirit." -> "Amén. / Y con tu espíritu."
        "Lord, have mercy." -> "Señor, ten piedad."
        "Lord, hear our prayer." -> "Te rogamos, óyenos."
        "Glory to you, O Lord. / Praise to you, Lord Jesus Christ." -> "Gloria a ti, Señor. / Gloria a ti, Señor Jesús."
        else -> response
    } else response
}

internal data class MassGuidePart(
    val id: String,
    val number: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val rows: List<MassGuideRow>,
    val showsDailyReadings: Boolean = false,
) {
    val localizedTitle get() = if (java.util.Locale.getDefault().language == "es") when (id) {
        "introductory-rites" -> "Ritos iniciales"; "liturgy-word" -> "Liturgia de la Palabra"
        "liturgy-eucharist" -> "Liturgia de la Eucaristía"; "communion-rite" -> "Rito de la Comunión"
        "concluding-rites" -> "Ritos de conclusión"; else -> title
    } else title
    val localizedSubtitle get() = if (java.util.Locale.getDefault().language == "es") when (id) {
        "introductory-rites" -> "Reunirse, arrepentirse, alabar y orar."
        "liturgy-word" -> "Escuchar, responder, profesar e interceder."
        "liturgy-eucharist" -> "Ofrecer, consagrar, recordar y adorar."
        "communion-rite" -> "Orar, compartir la paz, recibir y dar gracias."
        "concluding-rites" -> "Recibir la bendición y ser enviados."; else -> subtitle
    } else subtitle
    val localizedDetail get() = if (java.util.Locale.getDefault().language == "es") when (id) {
        "introductory-rites" -> "Los Ritos iniciales abren la Misa católica y preparan a los fieles para escuchar la Palabra de Dios y celebrar la Eucaristía. Incluyen la procesión de entrada, la veneración del altar, la señal de la cruz, el saludo, el acto penitencial, el Gloria y la oración colecta."
        "liturgy-word" -> "En la Liturgia de la Palabra, Dios habla a la Iglesia mediante la Sagrada Escritura. El pueblo escucha, responde con el salmo y la aclamación, profesa el Credo y ora por las necesidades del mundo."
        "liturgy-eucharist" -> "La Liturgia de la Eucaristía es el centro y culmen de la Misa. Se preparan los dones, se reza la Plegaria eucarística y Cristo se hace verdaderamente presente bajo las especies de pan y vino."
        "communion-rite" -> "El Rito de la Comunión prepara a los fieles para recibir al Señor. La Iglesia reza el Padre nuestro, pide la paz, invoca al Cordero de Dios y recibe la Sagrada Comunión."
        "concluding-rites" -> "La Misa concluye con la bendición y el envío. Los fieles salen para glorificar al Señor con su vida."; else -> detail
    } else detail
}

internal object MassGuideCatalog {
    const val dailyReadingsUrl = "https://bible.usccb.org/daily-bible-reading"

    private fun prayer(id: String, title: String, summary: String, fullText: String, note: String? = null, textNote: String? = null) =
        MassPrayerOption(id, title, summary, fullText.trimIndent(), note, textNote)

    val prayers = listOf(
        prayer("confiteor", "Penitential Act: Confiteor", "The people confess sin together, acknowledge the saints and the community, and ask for prayer and mercy.", """
            I confess to almighty God
            and to you, my brothers and sisters,
            that I have greatly sinned,
            in my thoughts and in my words,
            in what I have done and in what I have failed to do,

            through my fault, through my fault,
            through my most grievous fault;

            therefore I ask blessed Mary ever-Virgin,
            all the Angels and Saints,
            and you, my brothers and sisters,
            to pray for me to the Lord our God.
        """, "Often recognized by the opening words: “I confess…”", "Use the text provided in the parish missal or worship aid when praying at Mass."),
        prayer("dialogue", "Penitential Act: Dialogue", "The priest leads short invocations and the people respond by asking the Lord to show mercy and grant salvation.", """
            Priest: Have mercy on us, O Lord.
            People: For we have sinned against you.

            Priest: Show us, O Lord, your mercy.
            People: And grant us your salvation.
        """, textNote = "The absolution that follows is prayed by the priest."),
        prayer("tropes", "Penitential Act: Invocations with Kyrie", "Christ is addressed with brief titles or invocations, and the people respond: Lord, have mercy; Christ, have mercy.", """
            English:
            Lord, have mercy.
            Christ, have mercy.
            Lord, have mercy.

            Greek:
            Kyrie eleison.
            Christe eleison.
            Kyrie eleison.

            At Mass this form may include short invocations such as:
            “You were sent to heal the contrite of heart.”
            The people respond with the Kyrie.
        """, textNote = "Exact invocations may vary by the priest, deacon, or liturgical text used."),
        prayer("sprinkling", "Sprinkling Rite", "Especially during Easter Time, the priest may bless and sprinkle the people with holy water as a reminder of Baptism.", """
            During the sprinkling rite, recall your Baptism and renew your desire to live as a child of God.

            A simple prayer while being sprinkled:
            Lord, cleanse me. Renew the grace of my Baptism. Help me live as your disciple.
        """, "This can replace the usual Penitential Act.", "The official blessing prayers are prayed by the priest from the Roman Missal."),
        prayer("gloria", "Gloria", "A hymn of praise normally prayed or sung on Sundays outside Advent and Lent, solemnities, and feasts.", """
            Glory to God in the highest,
            and on earth peace to people of good will.

            We praise you, we bless you,
            we adore you, we glorify you,
            we give you thanks for your great glory,
            Lord God, heavenly King,
            O God, almighty Father.

            Lord Jesus Christ, Only Begotten Son,
            Lord God, Lamb of God, Son of the Father,
            you take away the sins of the world, have mercy on us;
            you take away the sins of the world, receive our prayer;
            you are seated at the right hand of the Father, have mercy on us.

            For you alone are the Holy One,
            you alone are the Lord,
            you alone are the Most High,
            Jesus Christ,
            with the Holy Spirit,
            in the glory of God the Father. Amen.
        """, textNote = "Use the text provided in the parish missal or worship aid when praying at Mass."),
        placeholder("collect", "The Collect", "The opening prayer proper to the day. The priest gathers the prayer of the Church and directs it to God.", """
            The Collect changes according to the day, feast, season, and Mass being celebrated.

            What to listen for:
            • The invitation “Let us pray”
            • A short silence in which the people pray
            • The priest gathering those prayers into one prayer
            • A conclusion through Christ, to which the people respond “Amen”
        """, "The priest gathers the prayers of the faithful into the opening prayer proper to that Mass.", "Add licensed Roman Missal Collect texts here when available."),
        prayer("nicene", "Nicene Creed", "The ordinary Sunday profession of faith, proclaiming belief in the Trinity, the Incarnation, the Church, Baptism, Resurrection, and eternal life.", """
            I believe in one God,
            the Father almighty,
            maker of heaven and earth,
            of all things visible and invisible.

            I believe in one Lord Jesus Christ,
            the Only Begotten Son of God,
            born of the Father before all ages.
            God from God, Light from Light,
            true God from true God,
            begotten, not made,
            consubstantial with the Father;
            through him all things were made.

            For us men and for our salvation
            he came down from heaven,
            and by the Holy Spirit was incarnate of the Virgin Mary,
            and became man.

            For our sake he was crucified under Pontius Pilate,
            he suffered death and was buried,
            and rose again on the third day
            in accordance with the Scriptures.

            He ascended into heaven
            and is seated at the right hand of the Father.
            He will come again in glory
            to judge the living and the dead
            and his kingdom will have no end.

            I believe in the Holy Spirit,
            the Lord, the giver of life,
            who proceeds from the Father and the Son,
            who with the Father and the Son is adored and glorified,
            who has spoken through the prophets.

            I believe in one, holy, catholic and apostolic Church.
            I confess one Baptism for the forgiveness of sins
            and I look forward to the resurrection of the dead
            and the life of the world to come. Amen.
        """, textNote = "Use the text provided in the parish missal or worship aid when praying at Mass."),
        prayer("apostles", "Apostles’ Creed", "A shorter baptismal creed that may be used in some seasons and settings, especially Lent and Easter Time.", """
            I believe in God,
            the Father almighty,
            Creator of heaven and earth,
            and in Jesus Christ, his only Son, our Lord,
            who was conceived by the Holy Spirit,
            born of the Virgin Mary,
            suffered under Pontius Pilate,
            was crucified, died and was buried;
            he descended into hell;
            on the third day he rose again from the dead;
            he ascended into heaven,
            and is seated at the right hand of God the Father almighty;
            from there he will come to judge the living and the dead.

            I believe in the Holy Spirit,
            the holy catholic Church,
            the communion of saints,
            the forgiveness of sins,
            the resurrection of the body,
            and life everlasting. Amen.
        """, textNote = "Use the text provided in the parish missal or worship aid when praying at Mass."),
        prayer("universal-prayer", "Universal Prayer", "The petitions after the Creed, also called the Prayer of the Faithful.", """
            The Universal Prayer changes by parish, season, and circumstance.

            Common pattern:
            • For the needs of the Church
            • For public authorities and the salvation of the world
            • For those burdened by any difficulty
            • For the local community

            The usual response is often:
            Lord, hear our prayer.
        """, "The deacon, lector, cantor, or another minister may announce the intentions.", "Local petitions are normally prepared for each Mass."),
        placeholder("presentation-gifts", "Preparation of the Gifts", "Bread and wine are prepared at the altar, and the offering of the people is joined to Christ’s sacrifice.", """
            What is happening:
            • Bread and wine are brought to the altar
            • The priest prepares the gifts
            • The people are invited to pray that the sacrifice may be acceptable to God
            • The assembly responds before the Prayer over the Offerings
        """, "This moment teaches that our lives, work, joys, and sufferings are offered with Christ.", "Add licensed Roman Missal text here when available."),
        placeholder("prayer-over-offerings", "Prayer over the Offerings", "The priest prays that God receive and sanctify the gifts prepared for the Eucharist.", """
            This prayer changes according to the day, feast, season, and Mass being celebrated.

            What to listen for:
            • The offering of bread and wine
            • A request that God receive the gifts
            • A request that the sacrifice bear fruit in the Church
            • The people’s response: Amen
        """, textNote = "Add licensed Roman Missal Prayer over the Offerings texts here when available."),
        prayer("preface-dialogue", "Preface Dialogue", "The priest invites the people to lift up their hearts and give thanks to the Lord.", """
            Priest: The Lord be with you.
            People: And with your spirit.

            Priest: Lift up your hearts.
            People: We lift them up to the Lord.

            Priest: Let us give thanks to the Lord our God.
            People: It is right and just.
        """, "This dialogue begins the Eucharistic Prayer.", "Use the text provided in the parish missal or worship aid when praying at Mass."),
        eucharisticPrayer("ep1", "Eucharistic Prayer I: Roman Canon", "The ancient Roman Canon. It has a solemn, expansive character, with longer commemorations of the saints and intercessions for the Church.", "Thanksgiving and praise\n• Prayer for the Church and her leaders\n• Remembrance of the living\n• Communion with Mary and the saints\n• Offering and consecration\n• Memorial of Christ’s Passion, Resurrection, and Ascension\n• Intercessions for the dead\n• Final doxology and Great Amen", "Often used on major feasts, solemnities, and occasions with special solemnity."),
        eucharisticPrayer("ep2", "Eucharistic Prayer II", "A concise Eucharistic Prayer with a clear structure of thanksgiving, epiclesis, institution narrative, memorial, offering, and intercession.", "Preface and Holy, Holy, Holy\n• Calling down the Holy Spirit upon the gifts\n• Institution narrative and consecration\n• Memorial acclamation\n• Offering of Christ’s sacrifice\n• Prayer for the Church, the living, and the dead\n• Final doxology and Great Amen", "Commonly used at daily Mass and many Sunday Masses."),
        eucharisticPrayer("ep3", "Eucharistic Prayer III", "A fuller prayer often used on Sundays and feasts. It emphasizes the gathered Church, the sacrifice of Christ, and the unity of the faithful.", "Praise of God’s holiness\n• Calling down the Holy Spirit upon the gifts\n• Institution narrative and consecration\n• Memorial acclamation\n• Offering of the living sacrifice\n• Prayer that the faithful become one body and one spirit in Christ\n• Intercessions for the Church and the dead\n• Final doxology and Great Amen", "Frequently used for Sunday parish Masses."),
        eucharisticPrayer("ep4", "Eucharistic Prayer IV", "A longer prayer with a fixed preface that recounts salvation history, from creation and covenant to Christ and the mission of the Spirit.", "Salvation history from creation through Christ\n• Thanksgiving for God’s covenant love\n• Calling down the Holy Spirit upon the gifts\n• Institution narrative and consecration\n• Memorial acclamation\n• Offering and intercessions\n• Final doxology and Great Amen", "Used less often because it has its own preface."),
        prayer("sanctus", "Holy, Holy, Holy", "The acclamation before the Eucharistic Prayer, joining the praise of angels and saints.", """
            English:
            Holy, Holy, Holy Lord God of hosts.
            Heaven and earth are full of your glory.
            Hosanna in the highest.

            Blessed is he who comes in the name of the Lord.
            Hosanna in the highest.

            Latin:
            Sanctus, Sanctus, Sanctus
            Dominus Deus Sabaoth.
            Pleni sunt cæli et terra gloria tua.
            Hosanna in excelsis.

            Benedictus qui venit in nomine Domini.
            Hosanna in excelsis.
        """, textNote = "Use the text provided in the parish missal or worship aid when praying at Mass."),
        prayer("memorial-acclamation", "Memorial Acclamations", "The people acclaim the mystery of faith after the consecration.", """
            Common forms include:

            We proclaim your Death, O Lord,
            and profess your Resurrection
            until you come again.

            Or:

            When we eat this Bread and drink this Cup,
            we proclaim your Death, O Lord,
            until you come again.

            Or:

            Save us, Savior of the world,
            for by your Cross and Resurrection
            you have set us free.
        """, textNote = "The acclamation used may vary by Mass setting."),
        prayer("great-amen", "Great Amen", "The people solemnly affirm the Eucharistic Prayer at its conclusion.", "Amen.\n\nThe Great Amen is the people’s full assent to the Eucharistic Prayer. It is often sung with special solemnity.", "This is one of the most important responses of the assembly."),
        prayer("lords-prayer", "Lord’s Prayer", "The prayer Jesus taught us, prayed by the whole Church in the Communion Rite.", """
            Our Father, who art in heaven,
            hallowed be thy name;
            thy kingdom come;
            thy will be done on earth as it is in heaven.

            Give us this day our daily bread,
            and forgive us our trespasses,
            as we forgive those who trespass against us;
            and lead us not into temptation,
            but deliver us from evil.

            Latin:
            Pater Noster, qui es in caelis,
            sanctificetur nomen tuum.
            Adveniat regnum tuum.
            Fiat voluntas tua, sicut in caelo et in terra.

            Panem nostrum quotidianum da nobis hodie,
            et dimitte nobis debita nostra sicut et nos dimittimus debitoribus nostris.
            Et ne nos inducas in tentationem,
            sed libera nos a malo. Amen.
        """, textNote = "At Mass the priest continues with the embolism, and the people respond with the doxology."),
        prayer("agnus-dei", "Lamb of God", "The litany sung or spoken during the breaking of the bread before Communion.", """
            English:
            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            have mercy on us.

            Lamb of God, you take away the sins of the world,
            grant us peace.

            Latin:
            Agnus Dei qui tollis peccata mundi,
            miserere nobis.

            Agnus Dei, qui tollis peccata mundi,
            miserere nobis.

            Agnus Dei, qui tollis peccata mundi,
            dona nobis pacem.
        """, textNote = "The first invocation may be repeated as needed during the fraction rite."),
        placeholder("communion-invitation", "Invitation to Communion", "The priest shows the Eucharist and invites the faithful to the supper of the Lamb.", """
            What to listen for:
            • The priest presents the Lamb of God
            • The faithful acknowledge their unworthiness
            • The Church approaches Communion with humility and faith
        """, textNote = "Add licensed Roman Missal text here when available."),
        placeholder("prayer-after-communion", "Prayer after Communion", "The priest prays that the sacrament received will bear fruit in the lives of the faithful.", """
            This prayer changes according to the day, feast, season, and Mass being celebrated.

            What to listen for:
            • Thanksgiving for the gift received
            • A request that Communion transform the faithful
            • A conclusion through Christ, to which the people respond “Amen”
        """, textNote = "Add licensed Roman Missal Prayer after Communion texts here when available."),
        placeholder("final-blessing", "Final Blessing", "The priest blesses the faithful before they are sent forth.", """
            The usual pattern:
            • The priest greets the people
            • The people respond
            • The priest blesses the faithful
            • The people answer: Amen
        """, "Some feasts and seasons use a solemn blessing or prayer over the people.", "Add licensed Roman Missal blessing texts here when available."),
        prayer("dismissal", "Dismissal", "The people are sent to live the mystery they have celebrated.", "The dismissal sends the faithful out from the Mass.\n\nThe response of the people:\nThanks be to God.", "The word “Mass” is connected to being sent on mission.", "The exact dismissal may vary according to the liturgical text used."),
    )

    val prayersById = prayers.associateBy { it.id }

    val communionRite = MassGuidePart("communion-rite", "3b", "Communion Rite", "Pray, share peace, receive, and give thanks.", "The Communion Rite prepares the faithful to receive the Lord. The Church prays the Lord’s Prayer, asks for peace, invokes the Lamb of God, and receives Holy Communion.", listOf(
        row("Lord’s Prayer", "The Church prays the prayer Jesus taught us.", "Stand", prayerIds = listOf("lords-prayer")),
        row("Sign of Peace", "The faithful express peace and charity before receiving Communion.", "Stand", "And with your spirit."),
        row("Lamb of God", "The Church calls upon Christ, the Lamb who takes away the sins of the world.", "Stand/Kneel", prayerIds = listOf("agnus-dei")),
        row("Holy Communion", "Those properly disposed receive the Body and Blood of Christ.", "Process", "Amen.", listOf("communion-invitation")),
        row("Prayer after Communion", "The priest asks that the sacrament bear fruit in the lives of the faithful.", "Stand", "Amen.", listOf("prayer-after-communion")),
    ))

    val parts = listOf(
        MassGuidePart("introductory-rites", "I", "Introductory Rites", "Gather, repent, praise, and pray.", "The Introductory Rites open the Catholic Mass, preparing the faithful to hear the Word of God and celebrate the Eucharist. This section includes the entrance procession, veneration of the altar, the Sign of the Cross, a formal greeting, the Penitential Act, the Gloria, and the opening prayer (Collect).", listOf(
            row("Entrance", "In the Catholic Mass, the entrance is the opening procession and rite. The priest, deacon, and altar servers walk from the back of the church to the altar. This symbolizes our life's journey toward heaven. An entrance chant or song is sung to unite the congregation in praise", "Stand"),
            row("Sign of the Cross and Greeting", "The Mass begins in the name of the Father, and of the Son, and of the Holy Spirit.", "Stand", "Amen. / And with your spirit."),
            row("Penitential Act", "The Penitential Act occurs at the beginning of the Catholic Mass. It prepares the faithful to worthily celebrate the sacred mysteries by acknowledging their sins and asking for God’s mercy. The rite might takes one of many forms—the Confiteor (I confess), a dialogue of versicles, invocations with the Kyrie eleison, or the sprinkling of water.", "Stand", "Lord, have mercy.", listOf("confiteor", "dialogue", "tropes", "sprinkling")),
            row("Gloria", "The Gloria (or 'Glory to God in the highest') is an ancient, joyful hymn of praise and adoration sung early in the Catholic Mass. It glorifies the Trinity, combining the song the angels sang at Jesus' birth (Luke 2:14) with prayers of thanksgiving and a plea for mercy.It is sung on Sundays outside Advent and Lent, solemnities, and feasts, the Church praises God with the hymn of glory.", "Stand", prayerIds = listOf("gloria")),
            row("Collect", "The Collect (or Opening Prayer) is the prayer that concludes the introductory rites of the Mass, just before the Liturgy of the Word. Its purpose is to literally 'collect' the silent prayers and intentions of the gathered congregation into one unified petition offered to God.", "Stand", "Amen.", listOf("collect")),
        )),
        MassGuidePart("liturgy-word", "II", "Liturgy of the Word", "Listen, respond, profess, and intercede.", "In the Liturgy of the Word, God speaks to the Church through Scripture. The people listen, respond in psalm and acclamation, profess the Creed, and pray for the needs of the world.", listOf(
            row("First Reading", "Usually from the Old Testament, except during Easter when Acts is often read.", "Sit", "Thanks be to God."),
            row("Responsorial Psalm", "The people respond to the Word of God in sung or spoken prayer.", "Sit"),
            row("Second Reading", "On Sundays and solemnities, this is usually from an apostolic letter or Revelation.", "Sit", "Thanks be to God."),
            row("Gospel Acclamation and Gospel", "The assembly stands to welcome Christ speaking in the Gospel.", "Stand", "Glory to you, O Lord. / Praise to you, Lord Jesus Christ."),
            row("Homily", "The homily is a sermon given by a priest or deacon during the Liturgy of the Word in the Catholic Mass. Its purpose is to explain the Scripture readings and help the congregation apply God's word to their daily lives.", "Sit"),
            row("Profession of Faith", "The Profession of Faith (or Creed) in the Catholic Mass is a solemn statement of core beliefs recited after the homily. It unites the congregation in shared faith and serves as a response to the Word of God.", "Stand", prayerIds = listOf("nicene", "apostles")),
            row("Universal Prayer", "The Universal Prayer (also known as the Prayer of the Faithful or General Intercessions) is a series of petitions where the congregation prays for the Church, civil leaders, the sick, and the world.", "Stand", "Lord, hear our prayer.", listOf("universal-prayer")),
        ), true),
        MassGuidePart("liturgy-eucharist", "III", "Liturgy of the Eucharist", "Offer, consecrate, remember, and adore.", "The Liturgy of the Eucharist is the center and high point of the Mass. The gifts are prepared, the Eucharistic Prayer is prayed, and Christ becomes truly present under the appearances of bread and wine.", listOf(
            row("Preparation of the Gifts", "Bread, wine, and the offering of the people are brought to the altar.", "Sit", prayerIds = listOf("presentation-gifts")),
            row("Prayer over the Offerings", "The priest prays that God will receive and sanctify the gifts.", "Stand", "Amen.", listOf("prayer-over-offerings")),
            row("Preface Dialogue", "The priest invites the people to lift up their hearts and give thanks.", "Stand", prayerIds = listOf("preface-dialogue")),
            row("Eucharistic Prayer", "The Church gives thanks, calls down the Spirit, remembers Christ’s saving sacrifice, and offers intercession.", "Stand/Kneel", prayerIds = listOf("ep1", "ep2", "ep3", "ep4")),
            row("Holy, Holy, Holy", "The Church joins the angels and saints in praise before the consecration.", "Stand", prayerIds = listOf("sanctus")),
            row("Institution Narrative and Consecration", "By Christ’s words and the Holy Spirit’s power, bread and wine become the Body and Blood of Christ.", "Kneel"),
            row("Memorial Acclamation", "The assembly proclaims the mystery of Christ’s death and resurrection.", "Kneel/Stand", prayerIds = listOf("memorial-acclamation")),
            row("Great Amen", "The people affirm the Eucharistic Prayer with a solemn Amen.", "Stand", "Amen.", listOf("great-amen")),
        )),
        MassGuidePart("concluding-rites", "IV", "Concluding Rites", "Be blessed and sent.", "The Mass ends with blessing and mission. The faithful are sent out to glorify the Lord by their lives.", listOf(
            row("Announcements", "Brief parish notices may be given after Communion.", "Sit/Stand"),
            row("Blessing", "The priest blesses the faithful in the name of the Trinity.", "Stand", "Amen.", listOf("final-blessing")),
            row("Dismissal", "The people are sent to glorify the Lord by their lives.", "Stand", "Thanks be to God.", listOf("dismissal")),
            row("Recessional", "The ministers depart, and the faithful go forth to live the mystery they have received.", "Stand"),
        )),
    )

    private fun placeholder(id: String, title: String, summary: String, text: String, note: String? = null, textNote: String? = null) = prayer(id, title, summary, text, note, textNote)
    private fun eucharisticPrayer(id: String, title: String, summary: String, structure: String, note: String) = placeholder(id, title, summary, "Follow-along structure:\n• $structure", note, "The full official Eucharistic Prayer is prayed by the priest from the Roman Missal.")
    private fun row(title: String, detail: String, posture: String? = null, response: String? = null, prayerIds: List<String> = emptyList()) = MassGuideRow(title, detail, posture, response, prayerIds)
}
