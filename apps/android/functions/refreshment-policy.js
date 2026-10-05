import {activeMessageMember} from './message-notification-policy.js';
export function dateKey(date, timeZone) {
  return new Intl.DateTimeFormat('en-CA', {timeZone,year:'numeric',month:'2-digit',day:'2-digit'}).format(date);
}
export function nextDateKey(key) {
  const date = new Date(`${key}T12:00:00Z`); date.setUTCDate(date.getUTCDate()+1);
  return date.toISOString().slice(0,10);
}
export function scheduleDays(documents, zone, today) {
  const days = new Map();
  for (const document of documents) {
    const data = document.data(); const date = data.date?.toDate?.();
    if (!date || !Number.isFinite(date.getTime())) continue;
    const day = dateKey(date, zone); if (day < today) continue;
    if (!days.has(day)) days.set(day, {day, topics:[]});
    days.get(day).topics.push(String(data.topic || ''));
  }
  return [...days.values()].sort((a,b)=>a.day.localeCompare(b.day));
}
export function canChangeSlot(profile, uid, classId, slot, targetId) {
  return activeMessageMember(profile,classId) && (profile.isInstructor === true ||
    (((!slot?.volunteerId && !slot?.volunteerName) || slot.volunteerId === uid) && (!targetId || targetId === uid)));
}
export function reminderDue(now, zone, day) {
  const hour = Number(new Intl.DateTimeFormat('en-US',{timeZone:zone,hour:'2-digit',hourCycle:'h23'}).format(now));
  return hour >= 9 && nextDateKey(dateKey(now,zone)) === day;
}
