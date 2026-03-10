const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

const TRIAL_DAYS = 30;
const DAY_MS = 24 * 60 * 60 * 1000;

exports.initUserAccessOnSignup = functions.auth.user().onCreate(async (user) => {
  const now = Date.now();
  const trialStartAt = admin.firestore.Timestamp.fromMillis(now);
  const trialEndAt = admin.firestore.Timestamp.fromMillis(now + TRIAL_DAYS * DAY_MS);

  await admin.firestore().collection("users").doc(user.uid).set(
    {
      status: "approved",
      planStatus: "trial",
      trialStartAt,
      trialEndAt,
      subscriptionEndAt: null,
      phone: user.phoneNumber || "",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    },
    { merge: true }
  );
});

exports.expireTrialsAndSubscriptions = functions.pubsub
  .schedule("every day 00:10")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const nowTs = admin.firestore.Timestamp.now();
    const db = admin.firestore();

    const trialSnap = await db.collection("users")
      .where("status", "==", "approved")
      .where("planStatus", "==", "trial")
      .where("trialEndAt", "<=", nowTs)
      .get();

    const activeSnap = await db.collection("users")
      .where("status", "==", "approved")
      .where("planStatus", "==", "active")
      .where("subscriptionEndAt", "<=", nowTs)
      .get();

    const batch = db.batch();
    trialSnap.docs.forEach((doc) => {
      batch.update(doc.ref, {
        status: "pending",
        planStatus: "expired",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });
    activeSnap.docs.forEach((doc) => {
      batch.update(doc.ref, {
        status: "pending",
        planStatus: "expired",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    });

    if (!trialSnap.empty || !activeSnap.empty) {
      await batch.commit();
    }

    return null;
  });
