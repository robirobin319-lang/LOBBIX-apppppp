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
const ADMIN_UID = "admin_owner";
const ADMIN_EMAIL = "robirobin319@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 1000) {
  return new Date(Date.now() - ms);
}

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

test("PlayerProfile starts at walletBalance 0 and blocks self-inflation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(2000);

  // Signup bonus / non-zero initial wallet must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sniper",
      inGameName: "AliceFF",
      email: "alice@gmail.com",
      freeFireId: "284719305",
      freeFireLevel: 64,
      avatarPreset: "cyber_crimson",
      walletBalance: 500,
      lockedBalance: 0,
      lastWithdrawalId: "",
      totalMatches: 0,
      totalWins: 0,
      totalKills: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Zero balance create succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sniper",
      inGameName: "AliceFF",
      email: "alice@gmail.com",
      freeFireId: "284719305",
      freeFireLevel: 64,
      avatarPreset: "cyber_crimson",
      walletBalance: 0,
      lockedBalance: 0,
      lastWithdrawalId: "",
      totalMatches: 0,
      totalWins: 0,
      totalKills: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cannot inflate walletBalance directly
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 1000,
      updatedAt: pastDate(500),
    })
  );
});

test("Deposit request must be Pending and does not auto-credit wallet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("deposit_requests").doc("dep_1").set({
      userId: ALICE_UID,
      amount: 250,
      utrNumber: "432198765432",
      adminUpiId: "lobbix.esports@okaxis",
      screenshotName: "upi_receipt.jpg",
      screenshotPreviewBase64: "data_preview_base64_payload",
      status: "Pending",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player cannot create Approved deposit
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("deposit_requests").doc("dep_2").set({
      userId: ALICE_UID,
      amount: 250,
      utrNumber: "432198765432",
      adminUpiId: "lobbix.esports@okaxis",
      screenshotName: "upi_receipt.jpg",
      screenshotPreviewBase64: "data_preview_base64_payload",
      status: "Approved",
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Withdrawal locks balance on Pending and auto-refunds once when Rejected", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Sniper",
      inGameName: "AliceFF",
      email: "alice@gmail.com",
      freeFireId: "284719305",
      freeFireLevel: 64,
      avatarPreset: "cyber_crimson",
      walletBalance: 300,
      lockedBalance: 0,
      lastWithdrawalId: "",
      totalMatches: 0,
      totalWins: 0,
      totalKills: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  // Atomic withdrawal submit (deducts 200 -> walletBalance=100, lockedBalance=200)
  const batch = aliceDb.batch();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);
  const wdRef = userRef.collection("withdrawal_requests").doc("wd_1");
  batch.update(userRef, {
    walletBalance: 100,
    lockedBalance: 200,
    lastWithdrawalId: "wd_1",
    updatedAt: pastDate(1000),
  });
  batch.set(wdRef, {
    userId: ALICE_UID,
    upiId: "alice@upi",
    amount: 200,
    status: "Pending",
    refundProcessed: false,
    adminNote: "",
    createdAt: pastDate(1000),
    updatedAt: pastDate(1000),
  });
  await assertSucceeds(batch.commit());

  // Admin rejects withdrawal
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("withdrawal_requests").doc("wd_1").update({
      status: "Rejected",
      adminNote: "Invalid UPI handle",
      updatedAt: pastDate(500),
    })
  );

  // App automatically refunds 200 back to walletBalance once
  const refundBatch = aliceDb.batch();
  refundBatch.update(wdRef, {
    refundProcessed: true,
    updatedAt: pastDate(100),
  });
  refundBatch.update(userRef, {
    walletBalance: 300,
    lockedBalance: 0,
    lastWithdrawalId: "wd_1",
    updatedAt: pastDate(100),
  });
  await assertSucceeds(refundBatch.commit());

  // Second refund attempt must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 500,
      lockedBalance: 0,
      lastWithdrawalId: "wd_1",
      updatedAt: pastDate(50),
    })
  );
});
