import SwiftUI

struct FormationGamesView: View {
    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 10) {
                            Label(IlluminedL10n.string("Formation Games"), systemImage: "puzzlepiece.extension")
                                .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                .foregroundStyle(IlluminedTheme.blue)

                            Text(IlluminedL10n.string("Practice Catholic moral theology terms with quick, repeatable games."))
                                .font(IlluminedTheme.font(size: 16))
                                .foregroundStyle(IlluminedTheme.secondaryText)
                                .lineSpacing(4)
                        }
                    }

                    VStack(spacing: 12) {
                        NavigationLink {
                            TermDefinitionMatchGameView()
                        } label: {
                            FormationGameMenuCard(
                                title: IlluminedL10n.string("Match Terms"),
                                subtitle: IlluminedL10n.string("Choose the correct definition for each virtue or vice."),
                                systemImage: "rectangle.and.text.magnifyingglass"
                            )
                        }
                        .buttonStyle(.plain)

                        NavigationLink {
                            DefinitionChoiceGameView()
                        } label: {
                            FormationGameMenuCard(
                                title: IlluminedL10n.string("Name That Term"),
                                subtitle: IlluminedL10n.string("Read the definition and select the matching term."),
                                systemImage: "checklist"
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding()
                .padding(.bottom, 88)
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct FormationGameMenuCard: View {
    let title: String
    let subtitle: String
    let systemImage: String

    var body: some View {
        IlluminedCard {
            HStack(spacing: 14) {
                Image(systemName: systemImage)
                    .font(IlluminedTheme.font(size: 22, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.gold)
                    .frame(width: 46, height: 46)
                    .background(IlluminedTheme.gold.opacity(0.12), in: Circle())

                VStack(alignment: .leading, spacing: 5) {
                    Text(title)
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)

                    Text(subtitle)
                        .font(IlluminedTheme.font(size: 14))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 0)

                Image(systemName: "chevron.right")
                    .font(IlluminedTheme.font(size: 12, weight: .bold))
                    .foregroundStyle(IlluminedTheme.secondaryText)
            }
        }
    }
}

private struct TermDefinitionMatchGameView: View {
    @State private var deck = FormationGameData.terms.shuffled()
    @State private var options: [FormationGameTerm] = []
    @State private var currentIndex = 0
    @State private var score = 0
    @State private var attempts = 0
    @State private var selectedID: String?
    @State private var isAnswered = false

    private var currentTerm: FormationGameTerm {
        deck[currentIndex]
    }

    var body: some View {
        FormationGameShell(
            title: IlluminedL10n.string("Match Terms"),
            subtitle: IlluminedL10n.string("Choose the definition that matches the term."),
            score: score,
            attempts: attempts,
            onReset: resetGame
        ) {
            IlluminedCard {
                VStack(alignment: .leading, spacing: 10) {
                    Text(currentTerm.localizedCategory)
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)

                    Text(currentTerm.localizedTerm)
                        .font(IlluminedTheme.font(size: 30, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.blue)

                    Text(IlluminedL10n.string("Which definition belongs to this term?"))
                        .font(IlluminedTheme.font(size: 16))
                        .foregroundStyle(IlluminedTheme.secondaryText)
                }
            }

            VStack(spacing: 10) {
                ForEach(options) { option in
                    FormationAnswerButton(
                        text: option.localizedDefinition,
                        isSelected: selectedID == option.id,
                        isCorrect: isAnswered && option.id == currentTerm.id,
                        isWrong: isAnswered && selectedID == option.id && option.id != currentTerm.id
                    ) {
                        choose(option)
                    }
                    .disabled(isAnswered)
                }
            }

            if isAnswered {
                Button {
                    nextRound()
                } label: {
                    Label(IlluminedL10n.string("Next Term"), systemImage: "arrow.right.circle.fill")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
            }
        }
        .onAppear {
            refreshOptions()
        }
    }

    private func choose(_ option: FormationGameTerm) {
        selectedID = option.id
        isAnswered = true
        attempts += 1

        if option.id == currentTerm.id {
            score += 1
        }
    }

    private func nextRound() {
        currentIndex = (currentIndex + 1) % deck.count
        selectedID = nil
        isAnswered = false
        refreshOptions()
    }

    private func resetGame() {
        deck = FormationGameData.terms.shuffled()
        currentIndex = 0
        score = 0
        attempts = 0
        selectedID = nil
        isAnswered = false
        refreshOptions()
    }

    private func refreshOptions() {
        let wrongOptions = Array(deck
            .filter { $0.id != currentTerm.id }
            .shuffled()
            .prefix(3))

        options = ([currentTerm] + wrongOptions).shuffled()
    }
}

private struct DefinitionChoiceGameView: View {
    @State private var deck = FormationGameData.terms.shuffled()
    @State private var options: [FormationGameTerm] = []
    @State private var currentIndex = 0
    @State private var score = 0
    @State private var attempts = 0
    @State private var selectedID: String?
    @State private var isAnswered = false

    private var currentTerm: FormationGameTerm {
        deck[currentIndex]
    }

    var body: some View {
        FormationGameShell(
            title: IlluminedL10n.string("Name That Term"),
            subtitle: IlluminedL10n.string("Choose the term that matches the definition."),
            score: score,
            attempts: attempts,
            onReset: resetGame
        ) {
            IlluminedCard {
                VStack(alignment: .leading, spacing: 12) {
                    Text(IlluminedL10n.string("Definition"))
                        .font(IlluminedTheme.font(size: 14, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.gold)

                    Text(currentTerm.localizedDefinition)
                        .font(IlluminedTheme.font(size: 20, weight: .semibold))
                        .foregroundStyle(IlluminedTheme.ink)
                        .lineSpacing(4)
                }
            }

            VStack(spacing: 10) {
                ForEach(options) { option in
                    FormationAnswerButton(
                        text: option.localizedTerm,
                        isSelected: selectedID == option.id,
                        isCorrect: isAnswered && option.id == currentTerm.id,
                        isWrong: isAnswered && selectedID == option.id && option.id != currentTerm.id
                    ) {
                        choose(option)
                    }
                    .disabled(isAnswered)
                }
            }

            if isAnswered {
                IlluminedCard {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(currentTerm.localizedCategory)
                            .font(IlluminedTheme.font(size: 14, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.gold)

                        Text(currentTerm.localizedTerm)
                            .font(IlluminedTheme.font(size: 22, weight: .semibold))
                            .foregroundStyle(IlluminedTheme.blue)
                    }
                }

                Button {
                    nextRound()
                } label: {
                    Label(IlluminedL10n.string("Next Definition"), systemImage: "arrow.right.circle.fill")
                        .font(IlluminedTheme.font(size: 17, weight: .semibold))
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(IlluminedPrimaryButtonStyle())
            }
        }
        .onAppear {
            refreshOptions()
        }
    }

    private func choose(_ option: FormationGameTerm) {
        selectedID = option.id
        isAnswered = true
        attempts += 1

        if option.id == currentTerm.id {
            score += 1
        }
    }

    private func nextRound() {
        currentIndex = (currentIndex + 1) % deck.count
        selectedID = nil
        isAnswered = false
        refreshOptions()
    }

    private func resetGame() {
        deck = FormationGameData.terms.shuffled()
        currentIndex = 0
        score = 0
        attempts = 0
        selectedID = nil
        isAnswered = false
        refreshOptions()
    }

    private func refreshOptions() {
        let wrongOptions = Array(deck
            .filter { $0.id != currentTerm.id }
            .shuffled()
            .prefix(3))

        options = ([currentTerm] + wrongOptions).shuffled()
    }
}

private struct FormationGameShell<Content: View>: View {
    let title: String
    let subtitle: String
    let score: Int
    let attempts: Int
    let onReset: () -> Void
    let content: Content

    init(
        title: String,
        subtitle: String,
        score: Int,
        attempts: Int,
        onReset: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.score = score
        self.attempts = attempts
        self.onReset = onReset
        self.content = content()
    }

    var body: some View {
        ZStack {
            IlluminedBackground()

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    IlluminedCard {
                        VStack(alignment: .leading, spacing: 14) {
                            HStack(alignment: .top, spacing: 12) {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text(title)
                                        .font(IlluminedTheme.font(size: 24, weight: .semibold))
                                        .foregroundStyle(IlluminedTheme.blue)

                                    Text(subtitle)
                                        .font(IlluminedTheme.font(size: 15))
                                        .foregroundStyle(IlluminedTheme.secondaryText)
                                        .lineSpacing(4)
                                }

                                Spacer()

                                Button(IlluminedL10n.string("Reset"), action: onReset)
                                    .font(IlluminedTheme.font(size: 14, weight: .semibold))
                                    .foregroundStyle(IlluminedTheme.blue)
                            }

                            HStack(spacing: 12) {
                                FormationScorePill(title: IlluminedL10n.string("Score"), value: "\(score)")
                                FormationScorePill(title: IlluminedL10n.string("Attempts"), value: "\(attempts)")
                            }
                        }
                    }

                    content
                }
                .padding()
                .padding(.bottom, 88)
            }
        }
        .illuminedBrandHeader()
        .illuminedNavigation()
    }
}

private struct FormationScorePill: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(IlluminedTheme.font(size: 22, weight: .semibold))
                .foregroundStyle(IlluminedTheme.blue)

            Text(title)
                .font(IlluminedTheme.font(size: 13))
                .foregroundStyle(IlluminedTheme.secondaryText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(IlluminedTheme.blue.opacity(0.07), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct FormationAnswerButton: View {
    let text: String
    let isSelected: Bool
    let isCorrect: Bool
    let isWrong: Bool
    let action: () -> Void

    private var borderColor: Color {
        if isCorrect { return .green }
        if isWrong { return .red }
        if isSelected { return IlluminedTheme.blue }
        return IlluminedTheme.gold.opacity(0.22)
    }

    private var iconName: String {
        if isCorrect { return "checkmark.circle.fill" }
        if isWrong { return "xmark.circle.fill" }
        return "circle"
    }

    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: iconName)
                    .font(IlluminedTheme.font(size: 20, weight: .semibold))
                    .foregroundStyle(isCorrect ? .green : isWrong ? .red : IlluminedTheme.gold)
                    .padding(.top, 2)

                Text(text)
                    .font(IlluminedTheme.font(size: 16, weight: .semibold))
                    .foregroundStyle(IlluminedTheme.ink)
                    .lineSpacing(3)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(14)
            .background(.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(borderColor, lineWidth: isCorrect || isWrong || isSelected ? 2 : 1)
            )
            .shadow(color: IlluminedTheme.softShadow.opacity(0.8), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
    }
}

private struct FormationGameTerm: Identifiable, Equatable {
    let id: String
    let term: String
    let definition: String
    let category: String

    var localizedTerm: String {
        Locale.current.languageCode == "es" ? FormationGameSpanishTranslations.byID[id]?.term ?? term : term
    }

    var localizedDefinition: String {
        Locale.current.languageCode == "es" ? FormationGameSpanishTranslations.byID[id]?.definition ?? definition : definition
    }

    var localizedCategory: String {
        let key: String
        switch category {
        case "Cardinal Virtues": key = "Cardinal Virtues"
        case "Parts of Prudence": key = "Parts of Prudence"
        case "Potential Parts of Prudence": key = "Potential Parts of Prudence"
        case "Parts of Justice": key = "Parts of Justice"
        case "Parts of Fortitude": key = "Parts of Fortitude"
        case "Parts of Temperance": key = "Parts of Temperance"
        case "Theological Virtues": key = "Theological Virtues"
        case "Vices Contrary to Prudence": key = "Vices Contrary to Prudence"
        case "Vices Contrary to Justice": key = "Vices Contrary to Justice"
        default: return category
        }
        return IlluminedL10n.string(key)
    }
}

private struct FormationGameTranslation {
    let term: String
    let definition: String
}

private enum FormationGameSpanishTranslations {
    static let byID: [String: FormationGameTranslation] = [
        "prudence": .init(term: "Prudencia", definition: "Conocer los medios para alcanzar el fin y saber aplicar un principio general en circunstancias concretas."),
        "memory": .init(term: "Memoria", definition: "Recordar las cosas correctas relacionadas con una acción y sus circunstancias."),
        "understanding": .init(term: "Entendimiento", definition: "La capacidad de comprender los principios prácticos y la naturaleza de diversas situaciones."),
        "docility": .init(term: "Docilidad", definition: "La capacidad de dejarse guiar y aceptar el consejo de otros."),
        "shrewdness": .init(term: "Sagacidad", definition: "Rapidez para descubrir los medios que conducen al fin."),
        "reason": .init(term: "Razón", definition: "La capacidad de razonar sobre asuntos prácticos y aplicar principios universales a situaciones particulares."),
        "foresight": .init(term: "Previsión", definition: "La capacidad de prever los resultados futuros de las acciones basándose en la experiencia pasada."),
        "circumspection": .init(term: "Circunspección", definition: "La virtud por la cual se tienen en cuenta las propias circunstancias."),
        "caution": .init(term: "Cautela", definition: "Aplicar el conocimiento del pasado a la acción para evitar impedimentos y males."),
        "good-counsel": .init(term: "Buen consejo", definition: "El hábito de buscar y aceptar un buen consejo."),
        "synesis": .init(term: "Sínesis", definition: "La capacidad de saber qué hacer cuando se aplica la ley común."),
        "gnome": .init(term: "Gnome", definition: "La capacidad de saber qué hacer cuando no se aplica la ley común."),
        "justice": .init(term: "Justicia", definition: "Dar a cada uno lo que le corresponde."),
        "commutative-justice": .init(term: "Justicia conmutativa", definition: "Justicia entre individuos."),
        "legal-justice": .init(term: "Justicia legal", definition: "Justicia del individuo hacia el bien común."),
        "distributive-justice": .init(term: "Justicia distributiva", definition: "Justicia de quienes están a cargo del bien común hacia el individuo."),
        "restitution": .init(term: "Restitución", definition: "El hábito por el cual se devuelve lo que se debe."),
        "religion": .init(term: "Religión", definition: "La virtud por la cual damos a Dios lo que se le debe."),
        "devotion": .init(term: "Devoción", definition: "Una voluntad pronta para hacer lo que pertenece al servicio de Dios."),
        "prayer": .init(term: "Oración", definition: "El acto, y también la virtud, de elevar la mente y el corazón a Dios."),
        "adoration": .init(term: "Adoración", definition: "El acto por el cual se manifiesta a Dios la reverencia que se le debe."),
        "sacrifice": .init(term: "Sacrificio", definition: "Ofrecer algún bien a Dios en forma de oblación."),
        "vow": .init(term: "Voto", definition: "Obligarse mediante una promesa a hacer algo, normalmente relacionado con el servicio de Dios."),
        "piety": .init(term: "Piedad", definition: "La virtud por la cual se rinde el debido honor y reverencia a los padres."),
        "dulia": .init(term: "Dulía", definition: "Dar el debido honor a los superiores."),
        "obedience": .init(term: "Obediencia", definition: "Prontitud de la voluntad para cumplir la voluntad del superior."),
        "gratitude": .init(term: "Gratitud", definition: "Aprecio, normalmente expresado, hacia un benefactor por algún don recibido."),
        "truthfulness": .init(term: "Veracidad", definition: "El hábito de decir la verdad."),
        "friendship": .init(term: "Amistad", definition: "La virtud por la cual una persona puede entablar amistad."),
        "liberality": .init(term: "Liberalidad", definition: "El uso de los bienes sobrantes para ayudar a los pobres."),
        "epikeia": .init(term: "Epiqueya", definition: "La virtud por la cual se conoce la intención del legislador."),
        "fortitude": .init(term: "Fortaleza", definition: "Disposición para afrontar lo arduo y soportar el sufrimiento a lo largo del tiempo."),
        "magnanimity": .init(term: "Magnanimidad", definition: "Buscar la excelencia en todo, especialmente en las cosas grandes."),
        "magnificence": .init(term: "Magnificencia", definition: "Usar las propias riquezas para realizar grandes obras."),
        "patience": .init(term: "Paciencia", definition: "La capacidad de soportar bien los males."),
        "perseverance": .init(term: "Perseverancia", definition: "Persistir en lo arduo hasta alcanzar el fin."),
        "longanimity": .init(term: "Longanimidad", definition: "Grandeza de alma; la capacidad de esperar el bien."),
        "mortification": .init(term: "Mortificación", definition: "La disposición a soportar bien el dolor y la incomodidad por amor a Dios."),
        "courage": .init(term: "Valentía", definition: "Elegir perseguir el bien a pesar del peligro de muerte."),
        "custody-mind": .init(term: "Custodia de la mente", definition: "No permitir que la mente albergue pensamientos impropios."),
        "custody-eyes": .init(term: "Custodia de los ojos", definition: "Mantener el control de la mirada para no ser atraído al pecado."),
        "temperance": .init(term: "Templanza", definition: "La virtud que modera los placeres del tacto y del gusto."),
        "shame": .init(term: "Vergüenza", definition: "Temor a ser percibido como indigno."),
        "honestia": .init(term: "Honestidad", definition: "El hábito de procurar siempre lo virtuoso en cada situación."),
        "abstinence": .init(term: "Abstinencia", definition: "Abstenerse de comer ciertos tipos de alimentos."),
        "fasting": .init(term: "Ayuno", definition: "Abstenerse de alimentos en general."),
        "sobriety": .init(term: "Sobriedad", definition: "La virtud por la cual se modera el consumo de alcohol."),
        "chastity": .init(term: "Castidad", definition: "Moderar los placeres del tacto en materias relativas al Sexto Mandamiento."),
        "continence": .init(term: "Continencia", definition: "Una virtud de la voluntad por la cual se permanece firme a pesar del tumulto de los apetitos."),
        "clemency": .init(term: "Clemencia o mansedumbre", definition: "Moderación del deleite de la reivindicación o de la ira."),
        "humility": .init(term: "Humildad", definition: "Disposición a vivir conforme a la verdad y a no juzgarse superior a lo que se es."),
        "eutrapelia": .init(term: "Eutrapelia", definition: "La virtud de la recreación adecuada."),
        "silence": .init(term: "Silencio", definition: "No hablar salvo cuando sea necesario y buscar la quietud interior de los apetitos."),
        "studiosity": .init(term: "Estudiosidad", definition: "Buscar el conocimiento de acuerdo con el propio estado de vida."),
        "simplicity": .init(term: "Sencillez", definition: "Moderar los bienes externos en cuanto a cantidad, sin tener ni demasiado ni demasiado poco."),
        "veracity": .init(term: "Veracidad", definition: "Regular el habla y orientarla hacia la verdad."),
        "faith": .init(term: "Fe", definition: "La virtud que nos inclina a creer precisamente lo que Dios nos revela."),
        "hope": .init(term: "Esperanza", definition: "La virtud referida a un bien futuro y arduo: la bienaventuranza eterna y la ayuda divina."),
        "charity": .init(term: "Caridad", definition: "Amistad entre Dios y el ser humano; amor sobrenatural ordenado a la bienaventuranza eterna."),
        "precipitation": .init(term: "Precipitación", definition: "Actuar con demasiada rapidez por no buscar consejo."),
        "inconsideration": .init(term: "Inconsideración", definition: "No juzgar cuál de los medios considerados es el mejor."),
        "inconsistency": .init(term: "Inconstancia", definition: "No ordenar o realizar la acción juzgada como la mejor."),
        "negligence": .init(term: "Negligencia", definition: "No buscar consejo o no hacer lo que se debe cuando corresponde."),
        "guile": .init(term: "Dolo", definition: "El hábito del engaño, normalmente mediante palabras."),
        "fraud": .init(term: "Fraude", definition: "El hábito del engaño, normalmente mediante acciones."),
        "murder": .init(term: "Asesinato", definition: "La muerte injusta de una persona inocente."),
        "theft": .init(term: "Hurto", definition: "Tomar a escondidas lo que pertenece a otro."),
        "robbery": .init(term: "Robo", definition: "Tomar abierta o violentamente lo que pertenece a otro."),
        "perjury": .init(term: "Perjurio", definition: "Mentir bajo juramento."),
        "detraction": .init(term: "Detracción", definition: "Decir algo verdadero con el fin de destruir la reputación de alguien."),
        "murmuring": .init(term: "Murmuración", definition: "Detracción oculta destinada a separar el afecto de una persona hacia otra."),
        "superstition": .init(term: "Superstición", definition: "Rendir a una criatura el honor o culto que solo se debe a Dios.")
    ]
}

private enum FormationGameData {
    static let terms: [FormationGameTerm] = [
        FormationGameTerm(id: "prudence", term: "Prudence", definition: "Knowing the means to attain the end and how to apply a general principle in concrete circumstances.", category: "Cardinal Virtues"),
        FormationGameTerm(id: "memory", term: "Memory", definition: "Remembering the right things pertaining to an action and its circumstances.", category: "Parts of Prudence"),
        FormationGameTerm(id: "understanding", term: "Understanding", definition: "The ability to grasp practical principles and the nature of various situations.", category: "Parts of Prudence"),
        FormationGameTerm(id: "docility", term: "Docility", definition: "The ability to be led and to take counsel from others.", category: "Parts of Prudence"),
        FormationGameTerm(id: "shrewdness", term: "Shrewdness", definition: "Quickness in arriving at the means to the end.", category: "Parts of Prudence"),
        FormationGameTerm(id: "reason", term: "Reason", definition: "The ability to reason about practical matters and apply universal principles to particular situations.", category: "Parts of Prudence"),
        FormationGameTerm(id: "foresight", term: "Foresight", definition: "The ability to see future outcomes of actions based on past experience.", category: "Parts of Prudence"),
        FormationGameTerm(id: "circumspection", term: "Circumspection", definition: "The virtue by which one keeps track of one's circumstances.", category: "Parts of Prudence"),
        FormationGameTerm(id: "caution", term: "Caution", definition: "Applying knowledge of the past to action in order to avoid impediments and evils.", category: "Parts of Prudence"),
        FormationGameTerm(id: "good-counsel", term: "Good Counsel", definition: "The habit of taking good counsel.", category: "Potential Parts of Prudence"),
        FormationGameTerm(id: "synesis", term: "Synesis", definition: "The ability to know what to do when the common law applies.", category: "Potential Parts of Prudence"),
        FormationGameTerm(id: "gnome", term: "Gnome", definition: "The ability to know what to do when the common law does not apply.", category: "Potential Parts of Prudence"),
        FormationGameTerm(id: "justice", term: "Justice", definition: "To render another his due.", category: "Cardinal Virtues"),
        FormationGameTerm(id: "commutative-justice", term: "Commutative Justice", definition: "Justice between individuals.", category: "Parts of Justice"),
        FormationGameTerm(id: "legal-justice", term: "Legal Justice", definition: "Justice of the individual toward the common good.", category: "Parts of Justice"),
        FormationGameTerm(id: "distributive-justice", term: "Distributive Justice", definition: "Justice of those in charge of the common good toward the individual.", category: "Parts of Justice"),
        FormationGameTerm(id: "restitution", term: "Restitution", definition: "The habit by which one pays back what one owes.", category: "Parts of Justice"),
        FormationGameTerm(id: "religion", term: "Religion", definition: "The virtue by which we render to God what is due to Him.", category: "Parts of Justice"),
        FormationGameTerm(id: "devotion", term: "Devotion", definition: "A prompt will to do those things pertaining to the service of God.", category: "Parts of Justice"),
        FormationGameTerm(id: "prayer", term: "Prayer", definition: "The act, and also a virtue, of lifting one's mind and heart to God.", category: "Parts of Justice"),
        FormationGameTerm(id: "adoration", term: "Adoration", definition: "The act by which one exhibits due reverence to God.", category: "Parts of Justice"),
        FormationGameTerm(id: "sacrifice", term: "Sacrifice", definition: "Offering some good to God in the form of oblation.", category: "Parts of Justice"),
        FormationGameTerm(id: "vow", term: "Vow", definition: "Binding oneself by promise to do something, usually in relation to the service of God.", category: "Parts of Justice"),
        FormationGameTerm(id: "piety", term: "Piety", definition: "The virtue by which one renders due honor and reverence to one's parents.", category: "Parts of Justice"),
        FormationGameTerm(id: "dulia", term: "Dulia", definition: "Giving due honor to one's superiors.", category: "Parts of Justice"),
        FormationGameTerm(id: "obedience", term: "Obedience", definition: "Promptness of the will to do the will of one's superior.", category: "Parts of Justice"),
        FormationGameTerm(id: "gratitude", term: "Gratitude", definition: "Appreciation, normally expressed, to a benefactor for some gift given.", category: "Parts of Justice"),
        FormationGameTerm(id: "truthfulness", term: "Truthfulness", definition: "The habit of telling the truth.", category: "Parts of Justice"),
        FormationGameTerm(id: "friendship", term: "Friendship", definition: "The virtue by which one is able to be befriended.", category: "Parts of Justice"),
        FormationGameTerm(id: "liberality", term: "Liberality", definition: "The use of one's surplus means to aid the poor.", category: "Parts of Justice"),
        FormationGameTerm(id: "epikeia", term: "Epikeia", definition: "The virtue by which one knows the mind of the legislator.", category: "Parts of Justice"),
        FormationGameTerm(id: "fortitude", term: "Fortitude", definition: "Willingness to engage the arduous and to endure suffering over time.", category: "Cardinal Virtues"),
        FormationGameTerm(id: "magnanimity", term: "Magnanimity", definition: "Seeking excellence in all things, especially great things.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "magnificence", term: "Magnificence", definition: "Using one's wealth to do great things.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "patience", term: "Patience", definition: "The ability to suffer evils well.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "perseverance", term: "Perseverance", definition: "Persisting in the arduous until the end is achieved.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "longanimity", term: "Longanimity", definition: "Longness of soul; the ability to await the good.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "mortification", term: "Mortification", definition: "The willingness to suffer pain and discomfort well for love of God.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "courage", term: "Courage", definition: "Choosing to pursue the good in spite of mortal danger.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "custody-mind", term: "Custody of the Mind", definition: "Not allowing improper thoughts to be entertained in the mind.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "custody-eyes", term: "Custody of the Eyes", definition: "Maintaining control of sight so as not to be drawn into sin.", category: "Parts of Fortitude"),
        FormationGameTerm(id: "temperance", term: "Temperance", definition: "The virtue which moderates the pleasures of touch and taste.", category: "Cardinal Virtues"),
        FormationGameTerm(id: "shame", term: "Shame", definition: "Fear of being perceived as lowly.", category: "Parts of Temperance"),
        FormationGameTerm(id: "honestia", term: "Honestia", definition: "The habit of always seeking to do what is virtuous in each situation.", category: "Parts of Temperance"),
        FormationGameTerm(id: "abstinence", term: "Abstinence", definition: "Refraining from eating certain kinds of food.", category: "Parts of Temperance"),
        FormationGameTerm(id: "fasting", term: "Fasting", definition: "Refraining from food in general.", category: "Parts of Temperance"),
        FormationGameTerm(id: "sobriety", term: "Sobriety", definition: "The virtue by which one has moderated use of alcohol.", category: "Parts of Temperance"),
        FormationGameTerm(id: "chastity", term: "Chastity", definition: "Moderating the pleasures of touch in matters pertaining to the Sixth Commandment.", category: "Parts of Temperance"),
        FormationGameTerm(id: "continence", term: "Continence", definition: "A virtue of the will by which one remains steadfast despite the tumult of the appetites.", category: "Parts of Temperance"),
        FormationGameTerm(id: "clemency", term: "Clemency or Meekness", definition: "Moderation of the delight of vindication or anger.", category: "Parts of Temperance"),
        FormationGameTerm(id: "humility", term: "Humility", definition: "Willingness to live according to the truth and not judge oneself greater than one is.", category: "Parts of Temperance"),
        FormationGameTerm(id: "eutrapelia", term: "Eutrapelia", definition: "The virtue of right recreation.", category: "Parts of Temperance"),
        FormationGameTerm(id: "silence", term: "Silence", definition: "Not speaking unless necessary and seeking interior quiet of the appetites.", category: "Parts of Temperance"),
        FormationGameTerm(id: "studiosity", term: "Studiosity", definition: "Pursuing knowledge according to one's state in life.", category: "Parts of Temperance"),
        FormationGameTerm(id: "simplicity", term: "Simplicity", definition: "Moderating one's externals as to quantity, having neither too much nor too little.", category: "Parts of Temperance"),
        FormationGameTerm(id: "veracity", term: "Veracity", definition: "Regulating speech and orienting it toward truth.", category: "Parts of Temperance"),
        FormationGameTerm(id: "faith", term: "Faith", definition: "The virtue that inclines us to believe precisely what God tells us.", category: "Theological Virtues"),
        FormationGameTerm(id: "hope", term: "Hope", definition: "The virtue concerned with future arduous good: eternal beatitude and divine aid.", category: "Theological Virtues"),
        FormationGameTerm(id: "charity", term: "Charity", definition: "Friendship between God and man; supernatural love ordered to eternal beatitude.", category: "Theological Virtues"),
        FormationGameTerm(id: "precipitation", term: "Precipitation", definition: "Acting too quickly because one does not take counsel.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "inconsideration", term: "Inconsideration", definition: "Failing to judge which means is best among those considered.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "inconsistency", term: "Inconsistency", definition: "Failing to command or do the action judged to be best.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "negligence", term: "Negligence", definition: "Failing to take counsel or failing to do what one should when one ought.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "guile", term: "Guile", definition: "The habit of deceit, usually in words.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "fraud", term: "Fraud", definition: "The habit of deceit, usually in deeds.", category: "Vices Contrary to Prudence"),
        FormationGameTerm(id: "murder", term: "Murder", definition: "Unjust killing of the innocent.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "theft", term: "Theft", definition: "Hidden taking of what belongs to another.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "robbery", term: "Robbery", definition: "Open or violent taking of what belongs to another.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "perjury", term: "Perjury", definition: "Lying under oath.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "detraction", term: "Detraction", definition: "Saying something true in order to destroy someone's reputation.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "murmuring", term: "Murmuring", definition: "Hidden detraction meant to separate one person's affection from another.", category: "Vices Contrary to Justice"),
        FormationGameTerm(id: "superstition", term: "Superstition", definition: "Rendering honor or practice to a creature that is due only to God.", category: "Vices Contrary to Justice")
    ]
}
