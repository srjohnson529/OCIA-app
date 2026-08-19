import Foundation

struct LessonCategory: Identifiable, Codable, Equatable {
    var id: String { category }
    var category: String
    var lessons: [Lesson]
}

struct Lesson: Identifiable, Codable, Equatable {
    var id: String
    var title: String
    var category: String
    var contentHTML: String
    var videoURL: String?
    var quiz: [QuizQuestion]

    init(id: String, title: String, category: String, contentHTML: String, videoURL: String?, quiz: [QuizQuestion]) {
        self.id = id
        self.title = title
        self.category = category
        self.contentHTML = contentHTML
        self.videoURL = videoURL
        self.quiz = quiz
    }

    enum CodingKeys: String, CodingKey {
        case id
        case title
        case category
        case contentHTML
        case videoURL = "videoUrl"
        case quiz
    }

    private struct QuizContainer: Codable, Equatable {
        var passingScore: Int?
        var questions: [QuizQuestion]
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        title = try container.decode(String.self, forKey: .title)
        category = try container.decode(String.self, forKey: .category)
        contentHTML = try container.decode(String.self, forKey: .contentHTML)
        videoURL = try container.decodeIfPresent(String.self, forKey: .videoURL)

        if let questionArray = try? container.decode([QuizQuestion].self, forKey: .quiz) {
            quiz = questionArray
        } else {
            let wrappedQuiz = try container.decode(QuizContainer.self, forKey: .quiz)
            quiz = wrappedQuiz.questions
        }
    }
}

struct QuizQuestion: Identifiable, Codable, Equatable {
    var id = UUID().uuidString
    var question: String
    var options: [String]
    var correct: Int

    init(id: String = UUID().uuidString, question: String, options: [String], correct: Int) {
        self.id = id
        self.question = question
        self.options = options
        self.correct = correct
    }

    enum CodingKeys: String, CodingKey {
        case id
        case question
        case options
        case correct
        case correctAnswerIndex
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decodeIfPresent(String.self, forKey: .id) ?? UUID().uuidString
        question = try container.decode(String.self, forKey: .question)
        options = try container.decode([String].self, forKey: .options)
        correct = try container.decodeIfPresent(Int.self, forKey: .correct)
            ?? container.decode(Int.self, forKey: .correctAnswerIndex)
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(question, forKey: .question)
        try container.encode(options, forKey: .options)
        try container.encode(correct, forKey: .correct)
    }
}

struct Badge: Identifiable, Codable, Equatable {
    var id: String
    var name: String
    var description: String
    var imageURL: String?
    var requiredCategory: String?
    var requiredMystery: String?
}
