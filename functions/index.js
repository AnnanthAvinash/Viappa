const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

exports.onRequestCreated = onDocumentCreated(
  "connection_requests/{requestId}",
  async (event) => {
    const data = event.data.data();
    if (!data) return;

    const { receiverId, senderName } = data;
    if (!receiverId || !senderName) return;

    const receiverDoc = await db.collection("users").doc(receiverId).get();
    const fcmToken = receiverDoc.data()?.fcmToken;
    if (!fcmToken) return;

    await messaging.send({
      token: fcmToken,
      data: {
        type: "request_received",
        title: "New Connection Request",
        body: `${senderName} sent you a connection request`,
        requestId: event.params.requestId,
        senderId: data.senderId || "",
      },
    });
  }
);

exports.onRequestUpdated = onDocumentUpdated(
  "connection_requests/{requestId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after) return;

    if (before.status === after.status) return;

    if (after.status === "accepted") {
      const { senderId, receiverName } = after;
      if (!senderId) return;

      const senderDoc = await db.collection("users").doc(senderId).get();
      const fcmToken = senderDoc.data()?.fcmToken;
      if (!fcmToken) return;

      await messaging.send({
        token: fcmToken,
        data: {
          type: "request_accepted",
          title: "Request Accepted",
          body: `${receiverName || "Someone"} accepted your connection request`,
          requestId: event.params.requestId,
          receiverId: after.receiverId || "",
        },
      });
    }
  }
);
