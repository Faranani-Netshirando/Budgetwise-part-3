"use strict";
// References: https://firebase.google.com/docs/functions/firestore-events
// https://firebase.google.com/docs/cloud-messaging/send/admin-sdk
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { dispatchUpdate } = require("./push");
initializeApp();

exports.notifyAccountChange = onDocumentWritten("users/{uid}/{collection}/{id}", async event => {
  if (!["transactions", "budgets", "goals"].includes(event.params.collection)) return;
  const devices = await getFirestore().collection("users").doc(event.params.uid).collection("devices").get();
  await dispatchUpdate(event.params.uid, event.id, devices.docs, getMessaging());
});
