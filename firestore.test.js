const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_host_123";
const BOB_UID = "bob_guest_456";
const MALLORY_UID = "mallory_attacker_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validRoomPayload(roomCode, hostUid, now) {
  return {
    roomCode,
    roomTitle: "Design Sync Call",
    hostUid,
    hostName: "Alice",
    guestUid: "",
    guestName: "",
    status: "waiting",
    hostMuted: false,
    hostCameraOn: true,
    hostScreenSharing: false,
    guestMuted: false,
    guestCameraOn: true,
    guestScreenSharing: false,
    sdpOffer: "v=0...",
    sdpAnswer: "",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rooms").doc("PLS100").get());
  await assertFails(
    unauthDb
      .collection("rooms")
      .doc("PLS100")
      .set(validRoomPayload("PLS100", ALICE_UID, new Date()))
  );
});

test("Host user: can create a valid room and query own hosted rooms", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("PLS100")
      .set(validRoomPayload("PLS100", ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("rooms").where("hostUid", "==", ALICE_UID).get()
  );
});

test("Cross-user isolation: non-participant cannot get another user's room or list without filter", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("rooms")
      .doc("PLS100")
      .set(validRoomPayload("PLS100", ALICE_UID, now));
  });

  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  await assertFails(malloryDb.collection("rooms").doc("PLS100").get());
  await assertFails(malloryDb.collection("rooms").get());
});

test("Guest join & reaction sync: guest can join waiting room and both can sync reactions", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("rooms")
      .doc("PLS100")
      .set(validRoomPayload("PLS100", ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("rooms").doc("PLS100").update({
      guestUid: BOB_UID,
      guestName: "Bob",
      status: "active",
      sdpAnswer: "v=0-answer",
      updatedAt: new Date(),
    })
  );

  // Now Bob can read the room and post a HEART gesture reaction
  await assertSucceeds(bobDb.collection("rooms").doc("PLS100").get());
  await assertSucceeds(
    bobDb
      .collection("rooms")
      .doc("PLS100")
      .collection("reactions")
      .doc("rx_1")
      .set({
        roomId: "PLS100",
        senderUid: BOB_UID,
        senderName: "Bob",
        gestureType: "HEART",
        emoji: "❤️",
        source: "AI_GESTURE",
        originX: 50,
        originY: 85,
        confidencePercent: 96,
        allowedUids: [ALICE_UID, BOB_UID],
        createdAt: new Date(),
      })
  );

  // Mallory cannot read or list reactions in Alice & Bob's room
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  await assertFails(
    malloryDb
      .collection("rooms")
      .doc("PLS100")
      .collection("reactions")
      .get()
  );
  await assertFails(
    malloryDb
      .collection("rooms")
      .doc("PLS100")
      .collection("reactions")
      .doc("rx_1")
      .get()
  );
});
