"use strict";
const expired = new Set(["messaging/registration-token-not-registered", "messaging/invalid-registration-token"]);

// All sending runs on the trusted backend; no service account or FCM server key goes into Android.
async function dispatchUpdate(uid, eventId, devices, messaging) {
  const valid = devices.filter(device => typeof device.data().token === "string" && device.data().token.length > 0);
  for (let offset = 0; offset < valid.length; offset += 500) {
    const batch = valid.slice(offset, offset + 500);
    const result = await messaging.sendEachForMulticast({
      tokens: batch.map(device => device.data().token),
      data: { uid, eventId },
      android: { priority: "high", ttl: 3600000 }
    });
    await Promise.all(result.responses.map((response, index) =>
      expired.has(response.error?.code) ? batch[index].ref.delete() : Promise.resolve()));
    if (result.responses.some(response => !response.success && !expired.has(response.error?.code))) {
      throw new Error("Transient push delivery failure");
    }
  }
}
module.exports = { dispatchUpdate };
