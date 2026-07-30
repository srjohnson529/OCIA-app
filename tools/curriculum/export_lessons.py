import json
import re
import sys
from html import unescape
from html.parser import HTMLParser
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
SOURCE = PROJECT_ROOT / "curriculum" / "lessons.json"
OUTPUT_DIR = PROJECT_ROOT / "curriculum" / "review"

CATEGORY_ORDER = [
    "Profession of Faith",
    "Celebration of the Christian Mysteries",
    "Life in Christ",
    "Christian Prayer",
]


class HTMLToMarkdown(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.parts = []
        self.list_stack = []
        self.li_number = []
        self.in_blockquote = False

    def emit(self, value):
        self.parts.append(value)

    def handle_starttag(self, tag, attrs):
        if tag in {"h1", "h2", "h3", "h4"}:
            self.emit("\n\n" + {"h1": "# ", "h2": "## ", "h3": "### ", "h4": "#### "}[tag])
        elif tag == "p":
            self.emit("\n\n")
        elif tag == "br":
            self.emit("\n")
        elif tag in {"strong", "b"}:
            self.emit("**")
        elif tag in {"em", "i"}:
            self.emit("*")
        elif tag == "blockquote":
            self.in_blockquote = True
            self.emit("\n\n> ")
        elif tag in {"ul", "ol"}:
            self.list_stack.append(tag)
            self.li_number.append(0)
            self.emit("\n")
        elif tag == "li":
            depth = max(len(self.list_stack) - 1, 0)
            prefix = "- "
            if self.list_stack and self.list_stack[-1] == "ol":
                self.li_number[-1] += 1
                prefix = f"{self.li_number[-1]}. "
            self.emit("\n" + "  " * depth + prefix)

    def handle_endtag(self, tag):
        if tag in {"strong", "b"}:
            self.emit("**")
        elif tag in {"em", "i"}:
            self.emit("*")
        elif tag == "blockquote":
            self.in_blockquote = False
            self.emit("\n")
        elif tag in {"ul", "ol"}:
            if self.list_stack:
                self.list_stack.pop()
                self.li_number.pop()
            self.emit("\n")

    def handle_data(self, data):
        text = re.sub(r"\s+", " ", data)
        if text.strip():
            self.emit(text)

    def markdown(self):
        text = unescape("".join(self.parts))
        text = re.sub(r"[ \t]+\n", "\n", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()


def html_to_markdown(html):
    parser = HTMLToMarkdown()
    parser.feed(html)
    return parser.markdown()


def markdown_to_text(markdown):
    text = re.sub(r"^#{1,4}\s+", "", markdown, flags=re.MULTILINE)
    text = text.replace("**", "").replace("*", "")
    text = re.sub(r"^>\s?", "    ", text, flags=re.MULTILINE)
    return text


def quiz_markdown(quiz):
    lines = ["### Review Quiz", f"Passing score: {quiz.get('passingScore', 0)}%", ""]
    for index, question in enumerate(quiz.get("questions", []), start=1):
        lines.append(f"{index}. {question.get('question', '').strip()}")
        for option_index, option in enumerate(question.get("options", [])):
            marker = chr(ord("A") + option_index)
            lines.append(f"   - {marker}. {option.strip()}")
        correct_index = question.get("correctAnswerIndex")
        if isinstance(correct_index, int) and 0 <= correct_index < len(question.get("options", [])):
            marker = chr(ord("A") + correct_index)
            answer = question["options"][correct_index].strip()
            lines.append(f"   - Answer: {marker}. {answer}")
        explanation = question.get("explanation", "").strip()
        if explanation:
            lines.append(f"   - Explanation: {explanation}")
        lines.append("")
    return "\n".join(lines).strip()


def main():
    catalog = json.loads(SOURCE.read_text(encoding="utf-8"))
    lessons = catalog["lessons"]
    grouped = {category: [] for category in CATEGORY_ORDER}
    for lesson in lessons:
        grouped.setdefault(lesson["category"], []).append(lesson)

    lines = [
        "# Illumined Lesson Content — Clergy and Instructor Review Edition",
        "",
        "This document presents the lesson content in a readable review format for bishops’ staff, parish pastors, catechetical leaders, and Illumined instructors.",
        "",
        f"Source catalog version: {catalog.get('version', 'unknown')}",
        f"Total lessons: {len(lessons)}",
        "",
        "Editorial note: This export reproduces the current application lesson text and quizzes. It is intended for content inspection and does not itself indicate ecclesiastical approval.",
        "",
        "## Table of Contents",
        "",
    ]
    for category in CATEGORY_ORDER:
        category_lessons = grouped.get(category, [])
        lines.append(f"- {category} ({len(category_lessons)} lessons)")
        for lesson in category_lessons:
            lines.append(f"  - {lesson['title']}")

    for category in CATEGORY_ORDER:
        category_lessons = grouped.get(category, [])
        lines.extend(["", "---", "", f"# {category}", ""])
        for number, lesson in enumerate(category_lessons, start=1):
            lines.extend([
                f"## {number}. {lesson['title']}",
                "",
                f"Lesson ID: {lesson['id']}",
                "",
                html_to_markdown(lesson.get("contentHTML", "")),
                "",
            ])
            video = lesson.get("videoUrl")
            if video:
                lines.extend(["### Video Resource", video, ""])
            lines.extend([quiz_markdown(lesson.get("quiz", {})), "", "---", ""])

    markdown = "\n".join(lines).strip() + "\n"
    plain_text = markdown_to_text(markdown)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUTPUT_DIR / "Illumined_Lesson_Content_Review.md").write_text(markdown, encoding="utf-8")
    (OUTPUT_DIR / "Illumined_Lesson_Content_Review.txt").write_text(plain_text, encoding="utf-8")
    print(f"Generated review editions for {len(lessons)} lessons in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
