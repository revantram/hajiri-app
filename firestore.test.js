const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const validTimestamp = () => new Date(Date.now() - 5000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("2026-11-12")
      .set({
        status: "present",
        note: "",
        createdAt: validTimestamp(),
        updatedAt: validTimestamp(),
      })
  );
});

test("User profile: owner can create and update phone/name; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = validTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Ramesh Kumar",
      email: "ramesh@example.com",
      phone: "9876543210",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update phone only
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phone: "9988776655",
      updatedAt: validTimestamp(),
    })
  );

  // Ghost field rejected (Shadow update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: validTimestamp(),
    })
  );

  // Mutating createdAt rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: validTimestamp(),
    })
  );

  // Bob cannot read or write Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      name: "Hacked",
      updatedAt: validTimestamp(),
    })
  );
});

test("Attendance: owner can create/update/list/delete valid attendance; rejects bad dateKey & status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = validTimestamp();

  const attRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("attendance")
    .doc("2026-11-12");

  await assertSucceeds(
    attRef.set({
      status: "present",
      note: "समय पर पहुंचा",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    attRef.update({
      status: "half",
      note: "दोपहर बाद छुट्टी",
      updatedAt: validTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  // Invalid date key format rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("invalid-date")
      .set({
        status: "present",
        note: "",
        createdAt: validTimestamp(),
        updatedAt: validTimestamp(),
      })
  );

  // Invalid status enum rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("2026-11-13")
      .set({
        status: "PRESENT",
        note: "",
        createdAt: validTimestamp(),
        updatedAt: validTimestamp(),
      })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("2026-11-14")
      .set({
        status: "present",
        note: "",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: validTimestamp(),
      })
  );

  // Cross-user read & write rejected
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("2026-11-12")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );

  // Owner can delete
  await assertSucceeds(attRef.delete());
});

test("Overtime: owner can create/update/delete valid hours; rejects <=0 or >24", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const otRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("overtime")
    .doc("2026-11-12");

  await assertSucceeds(
    otRef.set({
      hours: 2.5,
      createdAt: validTimestamp(),
      updatedAt: validTimestamp(),
    })
  );

  await assertSucceeds(
    otRef.update({
      hours: 4.0,
      updatedAt: validTimestamp(),
    })
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("overtime")
      .doc("2026-11-13")
      .set({
        hours: 30,
        createdAt: validTimestamp(),
        updatedAt: validTimestamp(),
      })
  );

  await assertSucceeds(otRef.delete());
});

test("Settings & Advances: owner CRUD succeeds; cross-user and invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const settRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("settings")
    .doc("default");

  await assertSucceeds(
    settRef.set({
      salary: 18000,
      dailyWage: 700,
      wageMode: "daily",
      otRate: 100,
      workDays: 26,
      themeStyle: "ocean_glass",
      settledMonths: ["2026-09"],
      monthPayments: { "2026-10": 10000 },
      activeSiteId: "site_metro",
      sites: [
        {
          id: "site_metro",
          name: "मेट्रो साइट",
          wageMode: "daily",
          salary: 18000,
          dailyWage: 700,
          otRate: 100,
          workDays: 26,
        },
      ],
      createdAt: validTimestamp(),
      updatedAt: validTimestamp(),
    })
  );

  await assertSucceeds(
    settRef.update({
      settledMonths: ["2026-09", "2026-10"],
      monthPayments: { "2026-10": 15000 },
      activeSiteId: "default",
      updatedAt: validTimestamp(),
    })
  );

  // Invalid workDays > 31 fails
  await assertFails(
    settRef.update({
      workDays: 40,
      updatedAt: validTimestamp(),
    })
  );

  // Invalid wageMode fails
  await assertFails(
    settRef.update({
      wageMode: "weekly",
      updatedAt: validTimestamp(),
    })
  );

  // Invalid themeStyle fails
  await assertFails(
    settRef.update({
      themeStyle: "neon_invalid",
      updatedAt: validTimestamp(),
    })
  );

  const advRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("advances")
    .doc("adv_1");

  await assertSucceeds(
    advRef.set({
      amount: 1500,
      reason: "दवाई",
      dateKey: "2026-11-12",
      createdAt: validTimestamp(),
      updatedAt: validTimestamp(),
    })
  );

  // Bob cannot delete Alice's advance
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("advances")
      .doc("adv_1")
      .delete()
  );

  // Negative amount fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("advances")
      .doc("adv_2")
      .set({
        amount: -500,
        reason: "invalid",
        dateKey: "2026-11-12",
        createdAt: validTimestamp(),
        updatedAt: validTimestamp(),
      })
  );
});
