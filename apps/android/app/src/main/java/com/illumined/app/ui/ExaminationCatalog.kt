package com.illumined.app.ui

internal data class ExaminationSection(
    val title: String,
    val items: List<String>,
    val titleEs: String? = null,
    val itemsEs: List<String>? = null,
) {
    val localizedTitle get() = if (java.util.Locale.getDefault().language == "es") titleEs ?: title else title
    val localizedItems get() = if (java.util.Locale.getDefault().language == "es") itemsEs ?: items else items
}

internal object ExaminationCatalog {
    const val preExamPrayer = "Come, Holy Spirit, enlighten my mind and open my heart. Help me to see my life truthfully in the light of God’s mercy. Give me courage to acknowledge my sins, sorrow for having offended God, and confidence in the forgiveness won by Jesus Christ. Amen."
    const val actOfContrition = "O my God, I am heartily sorry for having offended You, and I detest all my sins because of Your just punishments, but most of all because they offend You, my God, who are all-good and deserving of all my love. I firmly resolve, with the help of Your grace, to sin no more and to avoid the near occasions of sin. Amen."
    private const val preExamPrayerEs = "Ven, Espíritu Santo, ilumina mi mente y abre mi corazón. Ayúdame a ver mi vida con sinceridad a la luz de la misericordia de Dios. Dame valor para reconocer mis pecados, dolor por haber ofendido a Dios y confianza en el perdón obtenido por Jesucristo. Amén."
    private const val actOfContritionEs = "Dios mío, me arrepiento de todo corazón de todos mis pecados y los aborrezco, porque te ofendí a ti, sumo Bien y digno de amor sobre todas las cosas. Propongo firmemente, con ayuda de tu gracia, no pecar más y evitar las ocasiones próximas de pecado. Amén."
    val localizedPreExamPrayer get() = if(java.util.Locale.getDefault().language=="es") preExamPrayerEs else preExamPrayer
    val localizedActOfContrition get() = if(java.util.Locale.getDefault().language=="es") actOfContritionEs else actOfContrition

    private fun section(title: String, vararg items: String) = ExaminationSection(title, items.toList())
    private fun sectionEs(title: String, titleEs: String, items: List<String>, itemsEs: List<String>) =
        ExaminationSection(title, items, titleEs, itemsEs)

    val sections = listOf(
        sectionEs("First Commandment: Faith", "Primer mandamiento: Fe",
            listOf("Have I deliberately doubted or denied any teaching of the Catholic Church?", "Have I neglected to learn my faith?", "Have I rejected Church authority or Magisterial teaching?", "Have I been ashamed to identify myself as Catholic?", "Have I led others away from the faith?"),
            listOf("¿He dudado deliberadamente o negado alguna enseñanza de la Iglesia católica?", "¿He descuidado aprender mi fe?", "¿He rechazado la autoridad de la Iglesia o la enseñanza del Magisterio?", "¿Me he avergonzado de identificarme como católico?", "¿He alejado a otros de la fe?")),
        sectionEs("First Commandment: Hope", "Primer mandamiento: Esperanza",
            listOf("Have I despaired of God's mercy?", "Have I presumed that God will forgive me without repentance?", "Have I become overly anxious because I trust myself more than God?", "Have I sought security more in money, politics, success, or comfort than in God?"),
            listOf("¿He desesperado de la misericordia de Dios?", "¿He presumido que Dios me perdonará sin arrepentirme?", "¿Me he dejado dominar por la ansiedad porque confío más en mí mismo que en Dios?", "¿He buscado seguridad más en el dinero, la política, el éxito o la comodidad que en Dios?")),
        sectionEs("First Commandment: Charity", "Primer mandamiento: Caridad",
            listOf("Do I truly love God above all else?", "Have I knowingly chosen something over God?", "Is there any attachment I would refuse to surrender if God asked?"),
            listOf("¿Amo verdaderamente a Dios sobre todas las cosas?", "¿He elegido conscientemente algo por encima de Dios?", "¿Hay algún apego al que me negaría a renunciar si Dios me lo pidiera?")),
        sectionEs("First Commandment: Worship", "Primer mandamiento: Culto",
            listOf("Have I neglected daily prayer?", "Do I pray only when I need something?", "Have I prayed carelessly or distractedly without trying to focus?", "Have I ignored opportunities for Eucharistic Adoration?", "Have I neglected spiritual reading?"),
            listOf("¿He descuidado la oración diaria?", "¿Rezo solamente cuando necesito algo?", "¿He rezado con descuido o distracción sin esforzarme por concentrarme?", "¿He ignorado oportunidades de adoración eucarística?", "¿He descuidado la lectura espiritual?")),
        sectionEs("First Commandment: False Religion", "Primer mandamiento: Falsa religión",
            listOf("Have I participated in occult practices?", "Have I used Ouija boards?", "Have I consulted psychics or mediums?", "Have I read horoscopes seriously?", "Have I practiced New Age spirituality?", "Have I used crystals or energy healing with superstitious beliefs?", "Have I practiced witchcraft or magic?", "Have I participated in seances?"),
            listOf("¿He participado en prácticas ocultistas?", "¿He utilizado tablas ouija?", "¿He consultado a videntes o médiums?", "¿He tomado en serio los horóscopos?", "¿He practicado espiritualidades de la Nueva Era?", "¿He utilizado cristales o sanación energética con creencias supersticiosas?", "¿He practicado brujería o magia?", "¿He participado en sesiones espiritistas?")),
        sectionEs("First Commandment: Superstition", "Primer mandamiento: Superstición",
            listOf("Have I treated sacramentals as lucky charms?", "Have I trusted in signs or omens more than Providence?", "Have I believed objects possess spiritual power apart from God?"),
            listOf("¿He tratado los sacramentales como amuletos de buena suerte?", "¿He confiado más en señales o presagios que en la Providencia?", "¿He creído que ciertos objetos poseen poder espiritual al margen de Dios?")),
        sectionEs("First Commandment: Idolatry", "Primer mandamiento: Idolatría",
            listOf("Does career truly govern my life?", "Does money truly govern my life?", "Does politics truly govern my life?", "Does entertainment truly govern my life?", "Do sports, fitness, social media, reputation, or personal comfort govern my life?", "Have I elevated family above God?", "Could someone observing my life conclude these mattered more than God?"),
            listOf("¿Gobierna realmente mi vida mi carrera profesional?", "¿Gobierna realmente mi vida el dinero?", "¿Gobierna realmente mi vida la política?", "¿Gobierna realmente mi vida el entretenimiento?", "¿Gobiernan mi vida los deportes, la condición física, las redes sociales, la reputación o la comodidad personal?", "¿He puesto a mi familia por encima de Dios?", "¿Podría alguien que observa mi vida concluir que estas cosas me importan más que Dios?")),
        sectionEs("Second Commandment: Reverence", "Segundo mandamiento: Reverencia",
            listOf("Have I used God's name carelessly?", "Have I cursed using God's name?", "Have I used Jesus' name irreverently?", "Have I mocked holy things?"),
            listOf("¿He usado el nombre de Dios sin respeto?", "¿He maldecido usando el nombre de Dios?", "¿He usado irreverentemente el nombre de Jesús?", "¿Me he burlado de las cosas santas?")),
        sectionEs("Second Commandment: Speech", "Segundo mandamiento: Palabras",
            listOf("Have I made jokes that ridicule religion?", "Have I spoken irreverently about the saints?", "Have I spoken irreverently about the Blessed Virgin Mary?", "Have I spoken irreverently about the Pope or clergy without charity?"),
            listOf("¿He hecho bromas que ridiculizan la religión?", "¿He hablado irreverentemente de los santos?", "¿He hablado irreverentemente de la Santísima Virgen María?", "¿He hablado irreverentemente del Papa o del clero sin caridad?")),
        sectionEs("Second Commandment: Promises", "Segundo mandamiento: Promesas",
            listOf("Have I broken promises made to God?", "Have I failed to fulfill vows?", "Have I failed to complete a penance intentionally?"),
            listOf("¿He quebrantado promesas hechas a Dios?", "¿He dejado de cumplir votos?", "¿He dejado intencionalmente de cumplir una penitencia?")),
        sectionEs("Second Commandment: Witness", "Segundo mandamiento: Testimonio",
            listOf("Have I denied my faith through silence when charity required me to speak?", "Have I publicly acted contrary to Catholic teaching?"),
            listOf("¿He negado mi fe mediante el silencio cuando la caridad exigía que hablara?", "¿He actuado públicamente en contra de la enseñanza católica?")),
        sectionEs("Third Commandment: Sunday Mass", "Tercer mandamiento: Misa dominical",
            listOf("Have I deliberately missed Sunday Mass?", "Have I missed Holy Days of Obligation?", "Have I arrived intentionally late?", "Have I left early without necessity?"),
            listOf("¿He faltado deliberadamente a la Misa dominical?", "¿He faltado a Misa en días de precepto?", "¿He llegado tarde intencionalmente?", "¿Me he marchado antes sin necesidad?")),
        sectionEs("Third Commandment: Participation", "Tercer mandamiento: Participación",
            listOf("Was I attentive at Mass?", "Have I received Holy Communion unworthily?", "Have I received Communion while conscious of mortal sin?"),
            listOf("¿He estado atento durante la Misa?", "¿He recibido indignamente la Sagrada Comunión?", "¿He recibido la Comunión siendo consciente de estar en pecado mortal?")),
        sectionEs("Third Commandment: Rest and Worship", "Tercer mandamiento: Descanso y culto",
            listOf("Have I worked unnecessarily on Sunday?", "Have I made others work without need?", "Have I failed to spend time with family because of unnecessary work or entertainment?", "Do I prepare for Mass through prayer?", "Do I give thanks afterward?"),
            listOf("¿He trabajado innecesariamente el domingo?", "¿He hecho trabajar a otros sin necesidad?", "¿He dejado de pasar tiempo con mi familia por trabajo o entretenimiento innecesarios?", "¿Me preparo para la Misa mediante la oración?", "¿Doy gracias después?")),
        sectionEs("Fourth Commandment: Parents", "Cuarto mandamiento: Padres",
            listOf("Have I disobeyed my parents?", "Have I been disrespectful?", "Have I neglected aging parents?", "Have I refused forgiveness?", "Have I been impatient?"),
            listOf("¿He desobedecido a mis padres?", "¿Les he faltado al respeto?", "¿He descuidado a mis padres ancianos?", "¿Me he negado a perdonar?", "¿He sido impaciente?")),
        sectionEs("Fourth Commandment: Marriage and Children", "Cuarto mandamiento: Matrimonio e hijos",
            listOf("Have I loved my spouse sacrificially?", "Have I spoken harshly?", "Have I neglected emotional intimacy?", "Have I been controlling or selfish?", "Have I failed to teach my children the faith?", "Have I failed to discipline appropriately?", "Have I disciplined in anger?", "Have I neglected affection?", "Have I failed to pray with them?"),
            listOf("¿He amado a mi cónyuge con espíritu de sacrificio?", "¿He hablado con dureza?", "¿He descuidado la intimidad emocional?", "¿He sido controlador o egoísta?", "¿He dejado de enseñar la fe a mis hijos?", "¿He dejado de corregirlos adecuadamente?", "¿Los he corregido con ira?", "¿He descuidado el afecto?", "¿He dejado de rezar con ellos?")),
        sectionEs("Fourth Commandment: Authority and Duties", "Cuarto mandamiento: Autoridad y deberes",
            listOf("Have I obeyed legitimate authority?", "Have I been dishonest with employers?", "Have I neglected duties at work?", "Have I been lazy?", "Have I stolen time from work?", "Have I failed to vote responsibly?", "Have I refused legitimate civic obligations?", "Have I knowingly supported grave injustice?"),
            listOf("¿He obedecido a la autoridad legítima?", "¿He sido deshonesto con mis empleadores?", "¿He descuidado mis deberes laborales?", "¿He sido perezoso?", "¿He robado tiempo en el trabajo?", "¿He dejado de votar responsablemente?", "¿He rechazado obligaciones cívicas legítimas?", "¿He apoyado conscientemente una injusticia grave?")),
        sectionEs("Fifth Commandment: Violence and Anger", "Quinto mandamiento: Violencia e ira",
            listOf("Have I physically harmed another?", "Have I threatened violence?", "Have I encouraged violence?", "Have I held grudges?", "Have I refused forgiveness?", "Have I desired revenge?", "Have I delighted in another's suffering?", "Have I nourished hatred?"),
            listOf("¿He causado daño físico a otra persona?", "¿He amenazado con violencia?", "¿He fomentado la violencia?", "¿He guardado rencor?", "¿Me he negado a perdonar?", "¿He deseado vengarme?", "¿Me he alegrado del sufrimiento ajeno?", "¿He alimentado el odio?")),
        sectionEs("Fifth Commandment: Respect for Life and Self", "Quinto mandamiento: Respeto por la vida y por uno mismo",
            listOf("Have I supported abortion?", "Have I encouraged abortion?", "Have I procured abortion?", "Have I assisted euthanasia?", "Have I approved assisted suicide?", "Have I abused alcohol?", "Have I used illegal drugs?", "Have I driven recklessly?", "Have I neglected serious medical care?", "Have I harmed myself intentionally?"),
            listOf("¿He apoyado el aborto?", "¿He alentado a alguien a abortar?", "¿He procurado un aborto?", "¿He colaborado con la eutanasia?", "¿He aprobado el suicidio asistido?", "¿He abusado del alcohol?", "¿He consumido drogas ilegales?", "¿He conducido temerariamente?", "¿He descuidado atención médica seria?", "¿Me he hecho daño intencionalmente?")),
        sectionEs("Fifth Commandment: Scandal and Charity", "Quinto mandamiento: Escándalo y caridad",
            listOf("Have I led another into sin?", "Have I encouraged immoral behavior?", "Have I mocked virtue?", "Have I ignored someone in serious need?", "Have I failed to defend the innocent?", "Have I been cruel in speech?"),
            listOf("¿He llevado a otra persona al pecado?", "¿He fomentado una conducta inmoral?", "¿Me he burlado de la virtud?", "¿He ignorado a alguien con una necesidad grave?", "¿He dejado de defender al inocente?", "¿He sido cruel con mis palabras?")),
        sectionEs("Sixth and Ninth Commandments: Purity", "Sexto y noveno mandamientos: Pureza",
            listOf("Have I viewed pornography?", "Have I read sexually explicit material?", "Have I watched immoral entertainment for sexual excitement?", "Have I engaged in masturbation?", "Have I entertained lustful fantasies?", "Have I sought sexual stimulation outside marriage?"),
            listOf("¿He visto pornografía?", "¿He leído material sexualmente explícito?", "¿He visto entretenimiento inmoral buscando excitación sexual?", "¿He practicado la masturbación?", "¿He consentido fantasías lujuriosas?", "¿He buscado estimulación sexual fuera del matrimonio?")),
        sectionEs("Sixth and Ninth Commandments: Dating and Marriage", "Sexto y noveno mandamientos: Noviazgo y matrimonio",
            listOf("Have I engaged in sexual activity outside marriage?", "Have I lived together outside marriage?", "Have I encouraged impurity?", "Have I been unfaithful emotionally?", "Have I flirted inappropriately?", "Have I used contraception?", "Have I refused marital intimacy selfishly?", "Have I used my spouse merely for pleasure?"),
            listOf("¿He mantenido actividad sexual fuera del matrimonio?", "¿He convivido como pareja fuera del matrimonio?", "¿He fomentado la impureza?", "¿He sido infiel emocionalmente?", "¿He coqueteado de manera inapropiada?", "¿He usado anticonceptivos?", "¿He rechazado egoístamente la intimidad conyugal?", "¿He usado a mi cónyuge solamente para obtener placer?")),
        sectionEs("Sixth and Ninth Commandments: Eyes and Thoughts", "Sexto y noveno mandamientos: Mirada y pensamientos",
            listOf("Have I deliberately looked lustfully?", "Have I sought immodest images?", "Have I failed to avoid occasions of sin?", "Have I entertained fantasies instead of rejecting them?", "Have I objectified another person?"),
            listOf("¿He mirado deliberadamente con lujuria?", "¿He buscado imágenes impúdicas?", "¿He dejado de evitar ocasiones de pecado?", "¿He consentido fantasías en vez de rechazarlas?", "¿He tratado a otra persona como un objeto?")),
        sectionEs("Seventh and Tenth Commandments: Theft and Honesty", "Séptimo y décimo mandamientos: Robo y honradez",
            listOf("Have I taken anything not mine?", "Have I cheated?", "Have I knowingly pirated software or media?", "Have I failed to repay debts?", "Have I damaged another's property?", "Have I cheated on taxes?", "Have I cheated customers?", "Have I defrauded employers?", "Have I accepted dishonest payments?"),
            listOf("¿He tomado algo que no me pertenecía?", "¿He hecho trampa?", "¿He pirateado conscientemente programas o contenido multimedia?", "¿He dejado de pagar mis deudas?", "¿He dañado la propiedad ajena?", "¿He defraudado en los impuestos?", "¿He engañado a clientes?", "¿He defraudado a empleadores?", "¿He aceptado pagos deshonestos?")),
        sectionEs("Seventh and Tenth Commandments: Generosity, Envy, and Stewardship", "Séptimo y décimo mandamientos: Generosidad, envidia y administración",
            listOf("Have I been greedy?", "Have I neglected the poor?", "Have I refused reasonable charity?", "Have I been jealous of another's success?", "Have I rejoiced when others failed?", "Have I been resentful of another's blessings?", "Have I wasted resources?", "Have I been irresponsible with money?", "Have I gambled excessively?"),
            listOf("¿He sido codicioso?", "¿He descuidado a los pobres?", "¿He rechazado una ayuda caritativa razonable?", "¿He sentido celos del éxito ajeno?", "¿Me he alegrado cuando otros fracasaron?", "¿He resentido las bendiciones recibidas por otra persona?", "¿He desperdiciado recursos?", "¿He sido irresponsable con el dinero?", "¿He apostado en exceso?")),
        sectionEs("Eighth Commandment: Truthfulness and Gossip", "Octavo mandamiento: Veracidad y chismes",
            listOf("Have I lied?", "Have I exaggerated?", "Have I misled others?", "Have I hidden the truth unjustly?", "Have I spread rumors?", "Have I shared another's faults unnecessarily?", "Have I listened eagerly to gossip?", "Have I destroyed another's reputation?"),
            listOf("¿He mentido?", "¿He exagerado?", "¿He engañado a otros?", "¿He ocultado injustamente la verdad?", "¿He difundido rumores?", "¿He divulgado innecesariamente las faltas de otra persona?", "¿He escuchado chismes con interés?", "¿He destruido la reputación de otra persona?")),
        sectionEs("Eighth Commandment: Calumny, Judgment, and Confidence", "Octavo mandamiento: Calumnia, juicio y confidencialidad",
            listOf("Have I accused someone falsely?", "Have I repeated accusations without knowing they were true?", "Have I assumed bad motives in another person?", "Have I judged without sufficient evidence?", "Have I refused charitable interpretations?", "Have I broken legitimate confidence?", "Have I revealed secrets unnecessarily?"),
            listOf("¿He acusado falsamente a alguien?", "¿He repetido acusaciones sin saber si eran verdaderas?", "¿He atribuido malas intenciones a otra persona?", "¿He juzgado sin pruebas suficientes?", "¿Me he negado a interpretar caritativamente las acciones ajenas?", "¿He quebrantado una confidencia legítima?", "¿He revelado secretos innecesariamente?")),
        sectionEs("The Seven Deadly Sins", "Los siete pecados capitales",
            listOf("Pride: Do I seek admiration?", "Do I refuse correction?", "Do I think myself morally superior?", "Do I need to win every argument?", "Is money my primary concern?", "Do I hoard?", "Do I refuse generosity?", "Lust: Do I indulge impure curiosity?", "Do I seek pleasure apart from God's design?", "Envy: Am I unhappy because others succeed?", "Gluttony: Do I overeat?", "Do I drink excessively?", "Do I lack moderation?", "Wrath: Do I lose my temper?", "Do I speak abusively?", "Do I harbor resentment?", "Sloth: Do I neglect prayer?", "Do I waste excessive time?", "Do I delay duties?", "Do I neglect spiritual growth?"),
            listOf("Soberbia: ¿Busco admiración?", "¿Rechazo la corrección?", "¿Me considero moralmente superior?", "¿Necesito ganar todas las discusiones?", "¿Es el dinero mi principal preocupación?", "¿Acumulo bienes sin necesidad?", "¿Me niego a ser generoso?", "Lujuria: ¿Me entrego a una curiosidad impura?", "¿Busco placer al margen del designio de Dios?", "Envidia: ¿Me entristece el éxito de los demás?", "Gula: ¿Como en exceso?", "¿Bebo en exceso?", "¿Me falta moderación?", "Ira: ¿Pierdo el control de mi temperamento?", "¿Hablo de manera abusiva?", "¿Guardo resentimiento?", "Pereza: ¿Descuido la oración?", "¿Desperdicio demasiado tiempo?", "¿Aplazo mis deberes?", "¿Descuido mi crecimiento espiritual?")),
        sectionEs("Sins of Omission", "Pecados de omisión",
            listOf("Have I neglected prayer?", "Have I failed to forgive?", "Have I failed to evangelize when appropriate?", "Have I neglected corporal works of mercy?", "Have I neglected spiritual works of mercy?", "Have I failed to defend someone?", "Have I failed to comfort the suffering?", "Have I failed to visit the sick?", "Have I failed to encourage someone in faith?", "Have I failed to correct someone charitably when necessary?"),
            listOf("¿He descuidado la oración?", "¿He dejado de perdonar?", "¿He dejado de evangelizar cuando era oportuno?", "¿He descuidado las obras de misericordia corporales?", "¿He descuidado las obras de misericordia espirituales?", "¿He dejado de defender a alguien?", "¿He dejado de consolar a quien sufre?", "¿He dejado de visitar a los enfermos?", "¿He dejado de animar a alguien en la fe?", "¿He dejado de corregir caritativamente a alguien cuando era necesario?")),
        sectionEs("Questions About Love", "Preguntas sobre el amor",
            listOf("Have I loved God with all my heart?", "Have I loved my spouse and family sacrificially?", "Have I loved my neighbor as myself?", "Have I been patient?", "Have I been kind?", "Have I been humble?", "Have I been honest?", "Have I been chaste?", "Have I been merciful?", "Have I been forgiving?", "Have I been generous?", "Have I been faithful?", "Have I refused grace by ignoring urges to do good, avoid evil, or to be virtuous?", "Have I repeatedly resisted the Holy Spirit?"),
            listOf("¿He amado a Dios con todo mi corazón?", "¿He amado a mi cónyuge y a mi familia con espíritu de sacrificio?", "¿He amado a mi prójimo como a mí mismo?", "¿He sido paciente?", "¿He sido amable?", "¿He sido humilde?", "¿He sido honesto?", "¿He sido casto?", "¿He sido misericordioso?", "¿He sabido perdonar?", "¿He sido generoso?", "¿He sido fiel?", "¿He rechazado la gracia ignorando impulsos de hacer el bien, evitar el mal o practicar la virtud?", "¿He resistido repetidamente al Espíritu Santo?"))
    )
}
