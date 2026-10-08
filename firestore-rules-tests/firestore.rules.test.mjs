import { after, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  serverTimestamp,
  setDoc,
  updateDoc,
  Timestamp,
} from "firebase/firestore";

const testEnv = await initializeTestEnvironment({
  projectId: "rideaware-rules-test",
  firestore: {
    host: "127.0.0.1",
    port: 8080,
    rules: await readFile(new URL("../firestore.rules", import.meta.url), "utf8"),
  },
});

const contactPath = (uid, id = "contact-1") =>
  doc(testEnv.authenticatedContext(uid).firestore(), `users/${uid}/emergencyContacts/${id}`);

const validContact = (overrides = {}) => ({
  name: "Mariam",
  phoneNumber: "+65 8123 4567",
  relationship: "Family",
  createdAt: serverTimestamp(),
  updatedAt: serverTimestamp(),
  ...overrides,
});

const eventPath = (uid, id = "event-1") =>
  doc(testEnv.authenticatedContext(uid).firestore(), `users/${uid}/sosEvents/${id}`);

const validSosEvent = (overrides = {}) => ({
  status: "triggered",
  source: "simulated_helmet_button",
  contactId: "contact-1",
  createdAt: serverTimestamp(),
  locationStatus: "unavailable",
  ...overrides,
});

beforeEach(() => testEnv.clearFirestore());
after(() => testEnv.cleanup());

test("owner can create, read, edit, and delete an emergency contact", async () => {
  const ref = contactPath("owner");
  await assertSucceeds(setDoc(ref, validContact()));
  await assertSucceeds(getDoc(ref));
  await assertSucceeds(updateDoc(ref, {
    name: "Mary",
    phoneNumber: "+65 8765 4321",
    relationship: "Friend",
    updatedAt: serverTimestamp(),
  }));
  await assertSucceeds(deleteDoc(ref));
});

test("signed-out and different users cannot access an owner's contact", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/owner/emergencyContacts/contact-1"), validContact());
  });

  const anonymousRef = doc(testEnv.unauthenticatedContext().firestore(), "users/owner/emergencyContacts/contact-1");
  const otherUserRef = doc(testEnv.authenticatedContext("other").firestore(), "users/owner/emergencyContacts/contact-1");
  await assertFails(getDoc(anonymousRef));
  await assertFails(getDoc(otherUserRef));
  await assertFails(updateDoc(otherUserRef, { name: "Changed", updatedAt: serverTimestamp() }));
  await assertFails(deleteDoc(otherUserRef));
});

test("required fields, allowed fields, and phone format are enforced", async () => {
  const ref = contactPath("owner");
  const { relationship, ...missingRelationship } = validContact();
  assert.equal(relationship, "Family");
  await assertFails(setDoc(ref, missingRelationship));
  await assertFails(setDoc(ref, validContact({ admin: true })));
  await assertFails(setDoc(ref, validContact({ phoneNumber: "call-me" })));
});

test("createdAt cannot be changed and updates cannot add fields", async () => {
  const ref = contactPath("owner");
  await assertSucceeds(setDoc(ref, validContact()));
  await assertFails(updateDoc(ref, { createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(ref, { privateNote: "extra", updatedAt: serverTimestamp() }));
});

test("owner can create, read, and delete a valid SOS event", async () => {
  const contact = contactPath("owner");
  const event = eventPath("owner");
  await assertSucceeds(setDoc(contact, validContact()));
  await assertSucceeds(setDoc(event, validSosEvent()));
  await assertSucceeds(getDoc(event));
  await assertSucceeds(deleteDoc(event));
});

test("SOS events require the owner's saved contact and exact schema", async () => {
  const event = eventPath("owner");
  await assertFails(setDoc(event, validSosEvent()));
  await assertSucceeds(setDoc(contactPath("owner"), validContact()));
  await assertFails(setDoc(event, validSosEvent({ source: "phone_button" })));
  await assertFails(setDoc(event, validSosEvent({ note: "extra" })));
});

test("other users cannot access SOS events and events cannot be edited", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users/owner/emergencyContacts/contact-1"), validContact());
    await setDoc(doc(context.firestore(), "users/owner/sosEvents/event-1"), validSosEvent());
  });

  const ownerEvent = eventPath("owner");
  const otherEvent = doc(testEnv.authenticatedContext("other").firestore(), "users/owner/sosEvents/event-1");
  const signedOutEvent = doc(testEnv.unauthenticatedContext().firestore(), "users/owner/sosEvents/event-1");
  await assertFails(updateDoc(ownerEvent, { status: "cancelled" }));
  await assertFails(getDoc(otherEvent));
  await assertFails(deleteDoc(otherEvent));
  await assertFails(getDoc(signedOutEvent));
});

const validLocation = (overrides = {}) => ({
  latitude: 1.3521, longitude: 103.8198, accuracy: 12.5,
  capturedAt: Timestamp.now(), ...overrides,
});

test("SOS accepts coordinates and each explicit location fallback", async () => {
  await assertSucceeds(setDoc(contactPath("owner"), validContact()));
  await assertSucceeds(setDoc(eventPath("owner", "located"), validSosEvent({
    locationStatus: "available", location: validLocation(),
  })));
  for (const locationStatus of ["permission_denied", "disabled", "timeout", "unavailable"]) {
    await assertSucceeds(setDoc(eventPath("owner", locationStatus), validSosEvent({ locationStatus })));
  }
});

test("SOS rejects inconsistent, missing, or unrecognized location status", async () => {
  await assertSucceeds(setDoc(contactPath("owner"), validContact()));
  const { locationStatus, ...missingStatus } = validSosEvent();
  await assertFails(setDoc(eventPath("owner"), missingStatus));
  await assertFails(setDoc(eventPath("owner"), validSosEvent({ locationStatus: "unknown" })));
  await assertFails(setDoc(eventPath("owner"), validSosEvent({ locationStatus: "available" })));
  await assertFails(setDoc(eventPath("owner"), validSosEvent({ location: validLocation() })));
});

test("SOS validates coordinate bounds, accuracy, timestamp, and exact location fields", async () => {
  await assertSucceeds(setDoc(contactPath("owner"), validContact()));
  const { accuracy, ...missingAccuracy } = validLocation();
  for (const location of [
    validLocation({ latitude: 90.01 }), validLocation({ latitude: -90.01 }),
    validLocation({ longitude: 180.01 }), validLocation({ longitude: -180.01 }),
    validLocation({ latitude: "1.2" }), validLocation({ latitude: NaN }),
    validLocation({ longitude: Infinity }), validLocation({ accuracy: -1 }),
    validLocation({ accuracy: Infinity }), validLocation({ accuracy: "10" }),
    validLocation({ capturedAt: Date.now() }), validLocation({ extra: true }),
    missingAccuracy,
  ]) {
    await assertFails(setDoc(eventPath("owner"), validSosEvent({ locationStatus: "available", location })));
  }
});

test("location-bearing SOS events remain owner-only for writes and reads", async () => {
  await assertSucceeds(setDoc(contactPath("owner"), validContact()));
  const payload = validSosEvent({ locationStatus: "available", location: validLocation() });
  const otherRef = doc(testEnv.authenticatedContext("other").firestore(), "users/owner/sosEvents/located");
  const signedOutRef = doc(testEnv.unauthenticatedContext().firestore(), "users/owner/sosEvents/located");
  await assertFails(setDoc(otherRef, payload));
  await assertFails(setDoc(signedOutRef, payload));
  await assertSucceeds(setDoc(eventPath("owner", "located"), payload));
  await assertFails(getDoc(otherRef));
  await assertFails(getDoc(signedOutRef));
  await assertFails(updateDoc(eventPath("owner", "located"), { location: validLocation({ latitude: 20 }) }));
});

const userPath = (uid) => doc(testEnv.authenticatedContext(uid).firestore(), `users/${uid}`);

test("owner can pair and unpair the provisioned sample helmet", async () => {
  const user = userPath("owner");
  await assertSucceeds(setDoc(user, { uid: "owner", accountType: "anonymous" }));
  await assertSucceeds(updateDoc(user, {
    helmetId: "RA-DEMO-001", helmetPairedAt: serverTimestamp(),
  }));
  await assertSucceeds(updateDoc(user, {
    helmetId: deleteField(), helmetPairedAt: deleteField(),
  }));
});

test("helmet pairing rejects unknown QR payloads and malformed updates", async () => {
  const user = userPath("owner");
  await assertSucceeds(setDoc(user, { uid: "owner" }));
  await assertFails(updateDoc(user, { helmetId: "RA-OTHER-002", helmetPairedAt: serverTimestamp() }));
  await assertFails(updateDoc(user, { helmetId: "RA-DEMO-001" }));
  await assertFails(updateDoc(user, { helmetId: "RA-DEMO-001", helmetPairedAt: Timestamp.fromMillis(1) }));
  await assertSucceeds(updateDoc(user, { helmetId: "RA-DEMO-001", helmetPairedAt: serverTimestamp() }));
  await assertFails(updateDoc(user, { helmetId: deleteField() }));
});

test("paired helmet data stays owner-only and profile updates retain a valid pair", async () => {
  const user = userPath("owner");
  await assertSucceeds(setDoc(user, { uid: "owner" }));
  await assertSucceeds(updateDoc(user, { helmetId: "RA-DEMO-001", helmetPairedAt: serverTimestamp() }));
  await assertSucceeds(updateDoc(user, { lastSeenAt: serverTimestamp() }));
  const otherUser = doc(testEnv.authenticatedContext("other").firestore(), "users/owner");
  await assertFails(getDoc(otherUser));
  await assertFails(updateDoc(otherUser, { helmetId: deleteField(), helmetPairedAt: deleteField() }));
});
