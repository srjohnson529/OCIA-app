// Pure, shared completion policy. null means an instructions-only assignment
// with no trackable parts; preserve its explicit completion check.
export function assignmentProgress(assignment, profile, completions, prompts, posts) {
  const lessonIds = [...new Set((assignment.lessonLinks?.length ? assignment.lessonLinks.map(x => x.lessonId) : [assignment.lessonId]).filter(Boolean))];
  const readings = (assignment.readings || []).filter(x => x.title?.trim() && x.text?.trim());
  const readingIds = readings.length ? readings.map(x => x.id) : assignment.readingTitle?.trim() && assignment.readingText?.trim() ? ['legacy-reading'] : [];
  const active = prompts.filter(x => x.isActive !== false && x.classId === assignment.classId);
  const direct = active.filter(x => x.assignmentId === assignment.id);
  const requiredPrompts = (direct.length ? direct : active.filter(x => !x.assignmentId?.trim() && lessonIds.includes(x.lessonId))).filter(x => x.requiredForAssignment !== false);
  const total = readingIds.length + lessonIds.length + requiredPrompts.length;
  if (!total) return null;
  const completedReadings = new Set(completions.filter(x => x.userId === profile.id && x.classId === assignment.classId && x.isCompleted === true).map(x => x.assignmentId));
  const completedLessons = new Set(profile.completedLessons || []);
  const responded = new Set(posts.filter(x => x.authorId === profile.id && x.classId === assignment.classId).map(x => x.promptId));
  const completed = readingIds.filter(id => id && completedReadings.has(`${assignment.id}__reading__${id}`)).length
    + lessonIds.filter(id => completedLessons.has(id)).length
    + requiredPrompts.filter(x => responded.has(x.id)).length;
  return { completed, total, isCompleted: completed === total };
}
