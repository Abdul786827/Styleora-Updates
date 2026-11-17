const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_alice";
const BARBER_UID = "barber_bob";
const OTHER_UID = "other_charlie";
const ADMIN_EMAIL = "abdulquddus7745@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, userType = "CUSTOMER") {
  return {
    uid,
    fullName: "Test User",
    email: "user@styleora.app",
    mobileNumber: "+923001234567",
    userType,
    accountStatus: "ACTIVE",
    preferredTimezone: "Asia/Karachi",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validSalonPayload(ownerId) {
  return {
    ownerId,
    salonName: "Noir Studio",
    ownerName: "Bob Artisan",
    contactNumber: "+923001112222",
    address: "Gulberg III, Lahore",
    workingDays: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
    openingTimeMinutes: 540,
    closingTimeMinutes: 1320,
    breakStartMinutes: 810,
    breakEndMinutes: 870,
    acceptedPaymentMethods: ["PAY_AT_SALON", "CASH"],
    depositPercent: 0,
    allowCustomerCancellation: true,
    minCancellationHours: 2,
    cancellationRequiresApproval: false,
    holdTimeoutMinutes: 10,
    timezone: "Asia/Karachi",
    averageRating: 0,
    reviewCount: 0,
    isActive: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("salons").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("services").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("appointments").where("customerId", "==", CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("notifications").where("recipientId", "==", CUSTOMER_UID).get());
});

test("User Profile: owner can create & read, cross-user PII read fails, shadow field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BARBER_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_UID).set(validUserPayload(CUSTOMER_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(CUSTOMER_UID).get());

  // Cross-user PII read rejected
  await assertFails(bobDb.collection("users").doc(CUSTOMER_UID).get());
  // Non-admin list users rejected
  await assertFails(bobDb.collection("users").get());
  // Shadow ghost field rejected
  await assertFails(
    bobDb.collection("users").doc(BARBER_UID).set({
      ...validUserPayload(BARBER_UID, "BARBER"),
      isAdmin: true,
    })
  );
  // Self-suspension rejected
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_UID).update({ accountStatus: "SUSPENDED" })
  );
});

test("Email Spoofing Test: unverified admin email rejected, verified admin allowed", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await assertFails(
    spoofDb.collection("app_settings").doc("global").set({
      defaultTimezone: "Asia/Karachi",
      defaultHoldMinutes: 10,
      updatedAt: serverTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("app_settings").doc("global").set({
      defaultTimezone: "Asia/Karachi",
      defaultHoldMinutes: 10,
      updatedAt: serverTimestamp(),
    })
  );
});

test("Salons & Services: barber creates own salon/services, non-owner cannot mutate", async () => {
  const barberDb = testEnv.authenticatedContext(BARBER_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();

  await assertSucceeds(
    barberDb.collection("salons").doc("salon_1").set(validSalonPayload(BARBER_UID))
  );
  await assertSucceeds(
    barberDb.collection("services").doc("svc_1").set({
      salonId: "salon_1",
      barberId: BARBER_UID,
      salonName: "Noir Studio",
      name: "Signature Haircut",
      category: "MALE",
      price: 2500,
      durationMinutes: 45,
      isAvailable: true,
      isActive: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Customer can browse active salons & services
  await assertSucceeds(aliceDb.collection("salons").where("isActive", "==", true).get());
  await assertSucceeds(aliceDb.collection("services").where("isActive", "==", true).get());

  // Customer cannot edit barber's salon or service
  await assertFails(
    aliceDb.collection("salons").doc("salon_1").update({ salonName: "Hacked" })
  );
  await assertFails(
    aliceDb.collection("services").doc("svc_1").update({ price: 1 })
  );
});

test("Appointments & Cancellation: privacy, customer cancellation, and terminal state lock", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("salons").doc("salon_1").set(validSalonPayload(BARBER_UID));
  });

  const aliceDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const barberDb = testEnv.authenticatedContext(BARBER_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("appointments").doc("appt_1").set({
      salonId: "salon_1",
      salonName: "Noir Studio",
      barberId: BARBER_UID,
      customerId: CUSTOMER_UID,
      customerName: "Alice",
      customerPhone: "+923001234567",
      customerEmail: "alice@test.com",
      customerGender: "FEMALE",
      category: "FEMALE",
      serviceIds: ["svc_1"],
      serviceNames: ["Signature Haircut"],
      totalPrice: 2500,
      totalDurationMinutes: 45,
      date: "2026-11-20",
      startMinutes: 600,
      endMinutes: 645,
      status: "BOOKED",
      paymentMethod: "PAY_AT_SALON",
      paymentStatus: "PENDING",
      hasReview: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    })
  );

  // Unrelated third party cannot read or list
  await assertFails(charlieDb.collection("appointments").doc("appt_1").get());
  await assertFails(
    charlieDb.collection("appointments").where("customerId", "==", CUSTOMER_UID).get()
  );

  // Customer & Barber can query their own appointments
  await assertSucceeds(
    aliceDb.collection("appointments").where("customerId", "==", CUSTOMER_UID).get()
  );
  await assertSucceeds(
    barberDb.collection("appointments").where("barberId", "==", BARBER_UID).get()
  );

  // Customer can cancel own BOOKED appointment
  await assertSucceeds(
    aliceDb.collection("appointments").doc("appt_1").update({
      status: "CANCELLED",
      cancellationReason: "Schedule conflict",
      cancelledByUid: CUSTOMER_UID,
      cancelledByRole: "CUSTOMER",
      cancelledAtEpochMs: Date.now(),
      paymentStatus: "PENDING",
      updatedAt: serverTimestamp(),
    })
  );

  // Terminal state lock: already CANCELLED appointment cannot be cancelled again or completed
  await assertFails(
    barberDb.collection("appointments").doc("appt_1").update({
      status: "COMPLETED",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Reviews: only allowed after COMPLETED appointment owned by customer", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("salons").doc("salon_1").set(validSalonPayload(BARBER_UID));
    await ctx.firestore().collection("appointments").doc("appt_done").set({
      salonId: "salon_1",
      salonName: "Noir Studio",
      barberId: BARBER_UID,
      customerId: CUSTOMER_UID,
      customerName: "Alice",
      customerPhone: "+923001234567",
      customerEmail: "alice@test.com",
      customerGender: "FEMALE",
      category: "FEMALE",
      serviceIds: ["svc_1"],
      serviceNames: ["Haircut"],
      totalPrice: 2500,
      totalDurationMinutes: 45,
      date: "2026-11-10",
      startMinutes: 600,
      endMinutes: 645,
      status: "COMPLETED",
      paymentMethod: "CASH",
      paymentStatus: "PAID",
      hasReview: false,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(OTHER_UID).firestore();

  await assertFails(
    charlieDb.collection("reviews").doc("rev_1").set({
      salonId: "salon_1",
      barberId: BARBER_UID,
      appointmentId: "appt_done",
      customerId: OTHER_UID,
      customerName: "Charlie",
      rating: 5,
      createdAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_1").set({
      salonId: "salon_1",
      barberId: BARBER_UID,
      appointmentId: "appt_done",
      customerId: CUSTOMER_UID,
      customerName: "Alice",
      rating: 5,
      comment: "Exceptional finish!",
      createdAt: serverTimestamp(),
    })
  );
});
