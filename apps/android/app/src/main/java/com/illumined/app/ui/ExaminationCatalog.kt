package com.illumined.app.ui

internal data class ExaminationSection(val title: String, val items: List<String>)

internal object ExaminationCatalog {
    const val preExamPrayer = "Come, Holy Spirit, enlighten my mind and open my heart. Help me to see my life truthfully in the light of God’s mercy. Give me courage to acknowledge my sins, sorrow for having offended God, and confidence in the forgiveness won by Jesus Christ. Amen."
    const val actOfContrition = "O my God, I am heartily sorry for having offended You, and I detest all my sins because of Your just punishments, but most of all because they offend You, my God, who are all-good and deserving of all my love. I firmly resolve, with the help of Your grace, to sin no more and to avoid the near occasions of sin. Amen."

    private fun section(title: String, vararg items: String) = ExaminationSection(title, items.toList())

    val sections = listOf(
        section("First Commandment: Faith",
            "Have I deliberately doubted or denied any teaching of the Catholic Church?", "Have I neglected to learn my faith?", "Have I rejected Church authority or Magisterial teaching?", "Have I been ashamed to identify myself as Catholic?", "Have I led others away from the faith?"),
        section("First Commandment: Hope",
            "Have I despaired of God's mercy?", "Have I presumed that God will forgive me without repentance?", "Have I become overly anxious because I trust myself more than God?", "Have I sought security more in money, politics, success, or comfort than in God?"),
        section("First Commandment: Charity",
            "Do I truly love God above all else?", "Have I knowingly chosen something over God?", "Is there any attachment I would refuse to surrender if God asked?"),
        section("First Commandment: Worship",
            "Have I neglected daily prayer?", "Do I pray only when I need something?", "Have I prayed carelessly or distractedly without trying to focus?", "Have I ignored opportunities for Eucharistic Adoration?", "Have I neglected spiritual reading?"),
        section("First Commandment: False Religion",
            "Have I participated in occult practices?", "Have I used Ouija boards?", "Have I consulted psychics or mediums?", "Have I read horoscopes seriously?", "Have I practiced New Age spirituality?", "Have I used crystals or energy healing with superstitious beliefs?", "Have I practiced witchcraft or magic?", "Have I participated in seances?"),
        section("First Commandment: Superstition",
            "Have I treated sacramentals as lucky charms?", "Have I trusted in signs or omens more than Providence?", "Have I believed objects possess spiritual power apart from God?"),
        section("First Commandment: Idolatry",
            "Does career truly govern my life?", "Does money truly govern my life?", "Does politics truly govern my life?", "Does entertainment truly govern my life?", "Do sports, fitness, social media, reputation, or personal comfort govern my life?", "Have I elevated family above God?", "Could someone observing my life conclude these mattered more than God?"),
        section("Second Commandment: Reverence",
            "Have I used God's name carelessly?", "Have I cursed using God's name?", "Have I used Jesus' name irreverently?", "Have I mocked holy things?"),
        section("Second Commandment: Speech",
            "Have I made jokes that ridicule religion?", "Have I spoken irreverently about the saints?", "Have I spoken irreverently about the Blessed Virgin Mary?", "Have I spoken irreverently about the Pope or clergy without charity?"),
        section("Second Commandment: Promises",
            "Have I broken promises made to God?", "Have I failed to fulfill vows?", "Have I failed to complete a penance intentionally?"),
        section("Second Commandment: Witness",
            "Have I denied my faith through silence when charity required me to speak?", "Have I publicly acted contrary to Catholic teaching?"),
        section("Third Commandment: Sunday Mass",
            "Have I deliberately missed Sunday Mass?", "Have I missed Holy Days of Obligation?", "Have I arrived intentionally late?", "Have I left early without necessity?"),
        section("Third Commandment: Participation",
            "Was I attentive at Mass?", "Have I received Holy Communion unworthily?", "Have I received Communion while conscious of mortal sin?"),
        section("Third Commandment: Rest and Worship",
            "Have I worked unnecessarily on Sunday?", "Have I made others work without need?", "Have I failed to spend time with family because of unnecessary work or entertainment?", "Do I prepare for Mass through prayer?", "Do I give thanks afterward?"),
        section("Fourth Commandment: Parents",
            "Have I disobeyed my parents?", "Have I been disrespectful?", "Have I neglected aging parents?", "Have I refused forgiveness?", "Have I been impatient?"),
        section("Fourth Commandment: Marriage and Children",
            "Have I loved my spouse sacrificially?", "Have I spoken harshly?", "Have I neglected emotional intimacy?", "Have I been controlling or selfish?", "Have I failed to teach my children the faith?", "Have I failed to discipline appropriately?", "Have I disciplined in anger?", "Have I neglected affection?", "Have I failed to pray with them?"),
        section("Fourth Commandment: Authority and Duties",
            "Have I obeyed legitimate authority?", "Have I been dishonest with employers?", "Have I neglected duties at work?", "Have I been lazy?", "Have I stolen time from work?", "Have I failed to vote responsibly?", "Have I refused legitimate civic obligations?", "Have I knowingly supported grave injustice?"),
        section("Fifth Commandment: Violence and Anger",
            "Have I physically harmed another?", "Have I threatened violence?", "Have I encouraged violence?", "Have I held grudges?", "Have I refused forgiveness?", "Have I desired revenge?", "Have I delighted in another's suffering?", "Have I nourished hatred?"),
        section("Fifth Commandment: Respect for Life and Self",
            "Have I supported abortion?", "Have I encouraged abortion?", "Have I procured abortion?", "Have I assisted euthanasia?", "Have I approved assisted suicide?", "Have I abused alcohol?", "Have I used illegal drugs?", "Have I driven recklessly?", "Have I neglected serious medical care?", "Have I harmed myself intentionally?"),
        section("Fifth Commandment: Scandal and Charity",
            "Have I led another into sin?", "Have I encouraged immoral behavior?", "Have I mocked virtue?", "Have I ignored someone in serious need?", "Have I failed to defend the innocent?", "Have I been cruel in speech?"),
        section("Sixth and Ninth Commandments: Purity",
            "Have I viewed pornography?", "Have I read sexually explicit material?", "Have I watched immoral entertainment for sexual excitement?", "Have I engaged in masturbation?", "Have I entertained lustful fantasies?", "Have I sought sexual stimulation outside marriage?"),
        section("Sixth and Ninth Commandments: Dating and Marriage",
            "Have I engaged in sexual activity outside marriage?", "Have I lived together outside marriage?", "Have I encouraged impurity?", "Have I been unfaithful emotionally?", "Have I flirted inappropriately?", "Have I used contraception?", "Have I refused marital intimacy selfishly?", "Have I used my spouse merely for pleasure?"),
        section("Sixth and Ninth Commandments: Eyes and Thoughts",
            "Have I deliberately looked lustfully?", "Have I sought immodest images?", "Have I failed to avoid occasions of sin?", "Have I entertained fantasies instead of rejecting them?", "Have I objectified another person?"),
        section("Seventh and Tenth Commandments: Theft and Honesty",
            "Have I taken anything not mine?", "Have I cheated?", "Have I knowingly pirated software or media?", "Have I failed to repay debts?", "Have I damaged another's property?", "Have I cheated on taxes?", "Have I cheated customers?", "Have I defrauded employers?", "Have I accepted dishonest payments?"),
        section("Seventh and Tenth Commandments: Generosity, Envy, and Stewardship",
            "Have I been greedy?", "Have I neglected the poor?", "Have I refused reasonable charity?", "Have I been jealous of another's success?", "Have I rejoiced when others failed?", "Have I been resentful of another's blessings?", "Have I wasted resources?", "Have I been irresponsible with money?", "Have I gambled excessively?"),
        section("Eighth Commandment: Truthfulness and Gossip",
            "Have I lied?", "Have I exaggerated?", "Have I misled others?", "Have I hidden the truth unjustly?", "Have I spread rumors?", "Have I shared another's faults unnecessarily?", "Have I listened eagerly to gossip?", "Have I destroyed another's reputation?"),
        section("Eighth Commandment: Calumny, Judgment, and Confidence",
            "Have I accused someone falsely?", "Have I repeated accusations without knowing they were true?", "Have I assumed bad motives in another person?", "Have I judged without sufficient evidence?", "Have I refused charitable interpretations?", "Have I broken legitimate confidence?", "Have I revealed secrets unnecessarily?"),
        section("The Seven Deadly Sins",
            "Pride: Do I seek admiration?", "Do I refuse correction?", "Do I think myself morally superior?", "Do I need to win every argument?", "Is money my primary concern?", "Do I hoard?", "Do I refuse generosity?", "Lust: Do I indulge impure curiosity?", "Do I seek pleasure apart from God's design?", "Envy: Am I unhappy because others succeed?", "Gluttony: Do I overeat?", "Do I drink excessively?", "Do I lack moderation?", "Wrath: Do I lose my temper?", "Do I speak abusively?", "Do I harbor resentment?", "Sloth: Do I neglect prayer?", "Do I waste excessive time?", "Do I delay duties?", "Do I neglect spiritual growth?"),
        section("Sins of Omission",
            "Have I neglected prayer?", "Have I failed to forgive?", "Have I failed to evangelize when appropriate?", "Have I neglected corporal works of mercy?", "Have I neglected spiritual works of mercy?", "Have I failed to defend someone?", "Have I failed to comfort the suffering?", "Have I failed to visit the sick?", "Have I failed to encourage someone in faith?", "Have I failed to correct someone charitably when necessary?"),
        section("Questions About Love",
            "Have I loved God with all my heart?", "Have I loved my spouse and family sacrificially?", "Have I loved my neighbor as myself?", "Have I been patient?", "Have I been kind?", "Have I been humble?", "Have I been honest?", "Have I been chaste?", "Have I been merciful?", "Have I been forgiving?", "Have I been generous?", "Have I been faithful?", "Have I refused grace by ignoring urges to do good, avoid evil, or to be virtuous?", "Have I repeatedly resisted the Holy Spirit?")
    )
}
