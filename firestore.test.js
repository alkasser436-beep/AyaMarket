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

test("Unauthenticated users cannot read or write profiles, cart, or orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      favoriteIds: [],
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and update UserProfile and toggle favorites; ghost fields and cross-user writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aya Customer",
      phone: "+962790001122",
      address: "Amman, Gardens St 14",
      favoriteIds: ["gas_12kg"],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Update address (Action 1)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Aya Customer",
      phone: "+962790001122",
      address: "Amman, Mecca St 42",
      notes: "Ring doorbell",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Toggle favorites (Action 2)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteIds: ["gas_12kg", "tomato_1kg"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / immutable createdAt change
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject Bob reading or updating Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      address: "Hijacked",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("Cart operations enforce ownership, bounds, and clean partial updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("gas_12kg").set({
      userId: ALICE_UID,
      productId: "gas_12kg",
      quantity: 2,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("gas_12kg").update({
      quantity: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").get()
  );

  // Reject invalid quantity (0 or > 99) and mismatched productId
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("gas_12kg").update({
      quantity: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("gas_5kg").set({
      id: "gas_5kg", // @DocumentId forbidden inside body
      userId: ALICE_UID,
      productId: "gas_5kg",
      quantity: 1,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject Bob accessing Alice's cart
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cart").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cart").doc("gas_12kg").delete()
  );
});

test("Orders enforce pending initial status, single-step cancellation, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const validOrder = {
    userId: ALICE_UID,
    orderNumber: "AM-1001",
    customerName: "Alice",
    phone: "+962791112233",
    deliveryAddress: "Amman, Rainbow St 8",
    deliveryNotes: "Call on arrival",
    paymentMethod: "cash_on_delivery",
    status: "pending",
    itemsSummary: "2x أسطوانة غاز 12 كغ",
    items: [
      { productId: "gas_12kg", nameAr: "أسطوانة غاز منزلية 12 كغ", unitPrice: 7.0, quantity: 2 },
    ],
    totalItems: 2,
    subtotal: 14.0,
    deliveryFee: 1.0,
    totalAmount: 15.0,
    createdAt: created,
    updatedAt: created,
  };

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").set(validOrder)
  );

  // List query succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").get()
  );

  // Cannot create directly as delivered
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_2").set({
      ...validOrder,
      status: "delivered",
    })
  );

  // Owner can cancel a pending order
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").update({
      status: "cancelled",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: cannot modify a cancelled order
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").update({
      status: "pending",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cannot delete orders or read another user's orders
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").delete()
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("orders").get());
});
