package com.illumined.app.ui

internal data class MassPrayerOption(
    val id: String,
    val title: String,
    val summary: String,
    val fullText: String,
    val note: String? = null,
    val textNote: String? = null,
) {
    val textHeading: String get() = if (fullText.startsWith("Full official text")) "Text Placeholder" else "Prayer Text"
}

internal data class MassGuideRow(
    val title: String,
    val detail: String,
    val posture: String? = null,
    val response: String? = null,
    val prayerIds: List<String> = emptyList(),
)

internal data class MassGuidePart(
    val id: String,
    val number: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val rows: List<MassGuideRow>,
    val showsDailyReadings: Boolean = false,
)

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
