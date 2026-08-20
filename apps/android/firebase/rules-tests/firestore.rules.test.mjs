import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";

const projectId = "ocia-application";
let environment;

const profile = (userId, classIds, { instructor = false, admin = false } = {}) => ({
  userId,
  displayName: userId,
  classIds,
  isInstructor: instructor,
  isAdmin: admin,
});

before(async () => {
  environment = await initializeTestEnvironment({
    projectId,
    firestore: {
      rules: await readFile(new URL("../firestore.rules", import.meta.url), "utf8"),
    },
  });
});

beforeEach(async () => {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, "userProfiles", "instructorA"), profile("instructorA", ["classA"], { instructor: true })),
      setDoc(doc(db, "userProfiles", "instructorB"), profile("instructorB", ["classB"], { instructor: true })),
      setDoc(doc(db, "userProfiles", "studentA"), profile("studentA", ["classA"])),
      setDoc(doc(db, "userProfiles", "studentB"), profile("studentB", ["classB"])),
      setDoc(doc(db, "userProfiles", "admin"), profile("admin", [], { admin: true })),
      setDoc(doc(db, "classrooms", "classA"), { classId: "classA", instructorId: "instructorA", createdBy: "instructorA", isArchived: false }),
      setDoc(doc(db, "classrooms", "classB"), { classId: "classB", instructorId: "instructorB", createdBy: "instructorB", isArchived: false }),
      setDoc(doc(db, "announcements", "announcementA"), { classId: "classA", title: "A", message: "A" }),
      setDoc(doc(db, "announcements", "announcementB"), { classId: "classB", title: "B", message: "B" }),
      setDoc(doc(db, "classSchedule", "scheduleA"), { classId: "classA", topic: "A", details: "A" }),
      setDoc(doc(db, "classSchedule", "scheduleB"), { classId: "classB", topic: "B", details: "B" }),
      setDoc(doc(db, "assignments", "assignmentA"), { classId: "classA", title: "A", instructions: "A" }),
      setDoc(doc(db, "assignments", "assignmentB"), { classId: "classB", title: "B", instructions: "B" }),
      setDoc(doc(db, "assignmentCompletions", "completionB"), {
        classId: "classB",
        userId: "studentB",
        assignmentId: "assignmentB",
        isCompleted: false,
      }),
      setDoc(doc(db, "discussionParticipation", "participationB"), {
        classId: "classB",
        userId: "studentB",
        promptId: "promptB",
        hasParticipated: true,
      }),
      setDoc(doc(db, "discussionPrompts", "activePromptA"), {
        classId: "classA",
        isActive: true,
        title: "Visible",
      }),
      setDoc(doc(db, "discussionPrompts", "hiddenPromptA"), {
        classId: "classA",
        isActive: false,
        title: "Hidden",
      }),
      setDoc(doc(db, "discussionPrompts", "activePromptB"), { classId: "classB", isActive: true, title: "B" }),
      setDoc(doc(db, "chatMessages", "chatA"), { classId: "classA", senderId: "studentA", message: "A" }),
      setDoc(doc(db, "chatMessages", "chatB"), { classId: "classB", senderId: "studentB", message: "B" }),
      setDoc(doc(db, "prayerRequests", "prayerA"), { classId: "classA", requesterId: "studentA", title: "A", details: "", expiresAt: new Date(Date.now() + 86_400_000) }),
      setDoc(doc(db, "prayerRequests", "prayerB"), { classId: "classB", requesterId: "studentB", title: "B", details: "", expiresAt: new Date(Date.now() + 86_400_000) }),
      setDoc(doc(db, "instructorInviteCodes", "inviteA"), { classId: "classA", isActive: true, usedBy: "" }),
      setDoc(doc(db, "instructorInviteCodes", "inviteB"), { classId: "classB", isActive: true, usedBy: "" }),
      setDoc(doc(db, "parishSetupCodes", "setupExisting"), { classId: "", parishName: "", isActive: true, usedBy: "" }),
    ]);
  });
});

after(async () => {
  await environment?.cleanup();
});

test("an instructor can query and edit records in the assigned class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const result = await assertSucceeds(getDocs(query(collection(db, "announcements"), where("classId", "==", "classA"))));
  assert.equal(result.size, 1);
  await assertSucceeds(updateDoc(doc(db, "announcements", "announcementA"), { title: "Updated" }));
});

test("archived classes remain readable but reject new activity and client-side restoration", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), "classrooms", "classA"), { isArchived: true });
  });
  const studentDb = environment.authenticatedContext("studentA").firestore();
  const instructorDb = environment.authenticatedContext("instructorA").firestore();
  await assertSucceeds(getDoc(doc(studentDb, "announcements", "announcementA")));
  await assertFails(setDoc(doc(studentDb, "chatMessages", "archivedChat"), { classId: "classA", senderId: "studentA", message: "Hello" }));
  await assertFails(setDoc(doc(instructorDb, "assignments", "archivedAssignment"), { classId: "classA", title: "No", instructions: "No" }));
  await assertFails(updateDoc(doc(instructorDb, "classrooms", "classA"), { isArchived: false }));
});

test("an instructor cannot read, create, edit, or delete records in another class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  await assertFails(getDoc(doc(db, "announcements", "announcementB")));
  await assertFails(setDoc(doc(db, "announcements", "forged"), { classId: "classB", title: "X", message: "X" }));
  await assertFails(updateDoc(doc(db, "announcements", "announcementB"), { title: "Forged" }));
});

test("all instructor managers are writable only inside the instructor class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const managers = [
    ["announcements", { title: "New", message: "Message" }],
    ["classSchedule", { topic: "New", details: "Details" }],
    ["assignments", { title: "New", instructions: "Instructions" }],
    ["discussionPrompts", { title: "New", prompt: "Prompt", isActive: true }],
  ];
  for (const [collectionName, fields] of managers) {
    const ownId = `${collectionName}Own`;
    const foreignId = `${collectionName}Foreign`;
    await assertSucceeds(setDoc(doc(db, collectionName, ownId), { ...fields, classId: "classA" }));
    await assertSucceeds(updateDoc(doc(db, collectionName, ownId), { ...fields, classId: "classA" }));
    await assertFails(updateDoc(doc(db, collectionName, ownId), { classId: "classB" }));
    await assertSucceeds(deleteDoc(doc(db, collectionName, ownId)));
    await assertFails(setDoc(doc(db, collectionName, foreignId), { ...fields, classId: "classB" }));
  }
});

test("learners can read their class content but cannot read another class", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  for (const [collectionName, ownId, foreignId] of [
    ["announcements", "announcementA", "announcementB"],
    ["classSchedule", "scheduleA", "scheduleB"],
    ["assignments", "assignmentA", "assignmentB"],
    ["discussionPrompts", "activePromptA", "activePromptB"],
    ["chatMessages", "chatA", "chatB"],
    ["prayerRequests", "prayerA", "prayerB"],
  ]) {
    await assertSucceeds(getDoc(doc(db, collectionName, ownId)));
    await assertFails(getDoc(doc(db, collectionName, foreignId)));
  }
  await assertFails(getDoc(doc(db, "discussionPrompts", "hiddenPromptA")));
});

test("chat ownership and instructor moderation cannot change class or sender", async () => {
  const learner = environment.authenticatedContext("studentA").firestore();
  await assertSucceeds(setDoc(doc(learner, "chatMessages", "newChat"), { classId: "classA", senderId: "studentA", message: "Hello" }));
  await assertFails(setDoc(doc(learner, "chatMessages", "forgedChat"), { classId: "classA", senderId: "instructorA", message: "Forged" }));
  await assertFails(updateDoc(doc(learner, "chatMessages", "chatA"), { senderId: "studentB" }));
  const instructor = environment.authenticatedContext("instructorA").firestore();
  await assertSucceeds(updateDoc(doc(instructor, "chatMessages", "chatA"), { message: "Moderated" }));
  await assertFails(updateDoc(doc(instructor, "chatMessages", "chatA"), { classId: "classB" }));
  await assertFails(deleteDoc(doc(instructor, "chatMessages", "chatB")));
});

test("prayer requests enforce requester identity, class isolation, and owner cleanup", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  await assertSucceeds(setDoc(doc(db, "prayerRequests", "newPrayer"), { classId: "classA", requesterId: "studentA", title: "Please pray", details: "", expiresAt: new Date(Date.now() + 86_400_000) }));
  await assertFails(setDoc(doc(db, "prayerRequests", "forgedPrayer"), { classId: "classA", requesterId: "studentB", title: "Forged", details: "", expiresAt: new Date(Date.now() + 86_400_000) }));
  await assertFails(updateDoc(doc(db, "prayerRequests", "prayerA"), { classId: "classB" }));
  await assertSucceeds(deleteDoc(doc(db, "prayerRequests", "prayerA")));
  await assertFails(deleteDoc(doc(db, "prayerRequests", "prayerB")));
});

test("only administrators can create and manage parish setup codes", async () => {
  const adminDb = environment.authenticatedContext("admin").firestore();
  await assertSucceeds(setDoc(doc(adminDb, "parishSetupCodes", "setupNew"), { classId: "", parishName: "", isActive: true, usedBy: "" }));
  await assertSucceeds(updateDoc(doc(adminDb, "parishSetupCodes", "setupNew"), { parishName: "Parish" }));
  await assertSucceeds(deleteDoc(doc(adminDb, "parishSetupCodes", "setupNew")));
  const instructorDb = environment.authenticatedContext("instructorA").firestore();
  await assertFails(getDocs(collection(instructorDb, "parishSetupCodes")));
  await assertFails(setDoc(doc(instructorDb, "parishSetupCodes", "forgedSetup"), { classId: "", parishName: "", isActive: true, usedBy: "" }));
  await assertFails(deleteDoc(doc(instructorDb, "parishSetupCodes", "setupExisting")));
});

test("a newly authenticated parish organizer can redeem a setup code and create the first class", async () => {
  const db = environment.authenticatedContext("parishOrganizer", { email: "organizer@example.com" }).firestore();
  const batch = writeBatch(db);
  batch.set(doc(db, "userProfiles", "parishOrganizer"), {
    userId: "parishOrganizer",
    email: "organizer@example.com",
    displayName: "Parish Organizer",
    username: "Parish Organizer",
    isInstructor: true,
    isAdmin: false,
    classIds: ["NEWPARISH"],
    activeClassId: "NEWPARISH",
    classId: "NEWPARISH",
    parishSetupCode: "setupExisting",
  });
  batch.set(doc(db, "classrooms", "NEWPARISH"), {
    id: "NEWPARISH",
    classId: "NEWPARISH",
    name: "New Parish",
    parishName: "New Parish",
    instructorId: "parishOrganizer",
    instructorName: "Parish Organizer",
    studentIds: [],
    createdBy: "parishOrganizer",
    createdAt: serverTimestamp(),
    isArchived: false,
  });
  batch.update(doc(db, "parishSetupCodes", "setupExisting"), {
    isActive: false,
    usedBy: "parishOrganizer",
    usedByEmail: "organizer@example.com",
    usedByName: "Parish Organizer",
    classId: "NEWPARISH",
    parishName: "New Parish",
    usedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());
});

test("an instructor roster query returns only profiles sharing the assigned class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const result = await assertSucceeds(getDocs(query(collection(db, "userProfiles"), where("classIds", "array-contains", "classA"))));
  assert.deepEqual(result.docs.map((item) => item.id).sort(), ["instructorA", "studentA"]);
  await assertFails(getDoc(doc(db, "userProfiles", "studentB")));
});

test("a multi-class instructor can query only the currently active class roster", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), "userProfiles", "instructorA"), {
      classIds: ["classA", "classB"],
      activeClassId: "classB",
      classId: "classB",
    });
  });
  const db = environment.authenticatedContext("instructorA").firestore();
  const activeRoster = await assertSucceeds(getDocs(query(
    collection(db, "userProfiles"),
    where("classIds", "array-contains", "classB"),
  )));
  assert.deepEqual(activeRoster.docs.map((item) => item.id).sort(), ["instructorA", "instructorB", "studentB"]);
  await assertFails(getDocs(query(
    collection(db, "userProfiles"),
    where("classIds", "array-contains", "classA"),
  )));
});

test("an instructor cannot self-assign an additional class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  await assertFails(updateDoc(doc(db, "userProfiles", "instructorA"), { classIds: ["classA", "classB"] }));
});

test("an instructor can create and activate an additional owned class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "userProfiles", "instructorA"), {
    classIds: ["classA", "classC"],
    activeClassId: "classC",
    classId: "classC",
  });
  batch.set(doc(db, "classrooms", "classC"), {
    id: "classC",
    classId: "classC",
    name: "classC",
    parishName: "",
    instructorId: "instructorA",
    instructorName: "instructorA",
    studentIds: [],
    createdBy: "instructorA",
    createdAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());
  await assertSucceeds(setDoc(doc(db, "announcements", "announcementC"), {
    classId: "classC",
    title: "C",
    message: "C",
  }));
});

test("an instructor cannot create a class owned by another account", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "userProfiles", "instructorA"), {
    classIds: ["classA", "classC"],
    activeClassId: "classC",
    classId: "classC",
  });
  batch.set(doc(db, "classrooms", "classC"), {
    classId: "classC",
    instructorId: "instructorB",
    createdBy: "instructorB",
  });
  await assertFails(batch.commit());
});

test("an instructor can select only a class already assigned to the profile", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  await assertSucceeds(updateDoc(doc(db, "userProfiles", "instructorA"), {
    activeClassId: "classA",
    classId: "classA",
  }));
  await assertFails(updateDoc(doc(db, "userProfiles", "instructorA"), {
    activeClassId: "classB",
    classId: "classB",
  }));
});

test("an instructor can add and activate another class by consuming its invite", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const batch = writeBatch(db);
  batch.update(doc(db, "userProfiles", "instructorA"), {
    classIds: ["classA", "classB"],
    activeClassId: "classB",
    classId: "classB",
    instructorInviteCode: "inviteB",
  });
  batch.update(doc(db, "instructorInviteCodes", "inviteB"), {
    isActive: false,
    usedBy: "instructorA",
    usedAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());
});

test("a learner can update the learner's own formation fields", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  await assertSucceeds(updateDoc(doc(db, "userProfiles", "studentA"), { completedLessons: ["lesson-1"] }));
  await assertSucceeds(updateDoc(doc(db, "userProfiles", "studentA"), { selectedPrayerIds: ["our-father"] }));
});

test("a newly authenticated user can check for a profile before setup", async () => {
  const db = environment.authenticatedContext("newUser").firestore();
  const profileSnapshot = await assertSucceeds(getDoc(doc(db, "userProfiles", "newUser")));
  assert.equal(profileSnapshot.exists(), false);
});

test("a new learner can join and use a legacy class without a classroom document", async () => {
  const db = environment.authenticatedContext("legacyStudent").firestore();
  await assertSucceeds(setDoc(doc(db, "userProfiles", "legacyStudent"), {
    userId: "legacyStudent",
    displayName: "Legacy Student",
    classIds: ["legacyClass"],
    activeClassId: "legacyClass",
    classId: "legacyClass",
    isInstructor: false,
    isAdmin: false,
  }));
  await assertSucceeds(setDoc(doc(db, "chatMessages", "legacyChat"), {
    classId: "legacyClass",
    senderId: "legacyStudent",
    message: "Hello",
  }));
});

test("a new learner cannot join an archived class", async () => {
  await environment.withSecurityRulesDisabled(async (context) => {
    await updateDoc(doc(context.firestore(), "classrooms", "classA"), { isArchived: true });
  });
  const db = environment.authenticatedContext("blockedStudent").firestore();
  await assertFails(setDoc(doc(db, "userProfiles", "blockedStudent"), {
    userId: "blockedStudent",
    displayName: "Blocked Student",
    classIds: ["classA"],
    activeClassId: "classA",
    classId: "classA",
    isInstructor: false,
    isAdmin: false,
  }));
});

test("a learner prompt query requires the iOS class and active filters", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  await assertFails(getDocs(query(collection(db, "discussionPrompts"), where("classId", "==", "classA"))));
  const result = await assertSucceeds(getDocs(query(
    collection(db, "discussionPrompts"),
    where("classId", "==", "classA"),
    where("isActive", "==", true),
  )));
  assert.deepEqual(result.docs.map((item) => item.id), ["activePromptA"]);
});

test("a learner cannot take over another learner's completion", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  await assertFails(updateDoc(doc(db, "assignmentCompletions", "completionB"), {
    userId: "studentA",
    classId: "classA",
    isCompleted: true,
  }));
});

test("a learner cannot take over another learner's discussion participation", async () => {
  const db = environment.authenticatedContext("studentA").firestore();
  await assertFails(updateDoc(doc(db, "discussionParticipation", "participationB"), {
    userId: "studentA",
    classId: "classA",
  }));
});

test("an instructor invite list is restricted to the assigned class", async () => {
  const db = environment.authenticatedContext("instructorA").firestore();
  const result = await assertSucceeds(getDocs(query(collection(db, "instructorInviteCodes"), where("classId", "==", "classA"))));
  assert.deepEqual(result.docs.map((item) => item.id), ["inviteA"]);
  await assertFails(getDoc(doc(db, "instructorInviteCodes", "inviteB")).then(async (snapshot) => {
    if (!snapshot.exists()) throw new Error("missing document");
    await updateDoc(snapshot.ref, { isActive: false });
  }));
});
