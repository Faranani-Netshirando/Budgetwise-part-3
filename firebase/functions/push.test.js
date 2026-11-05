"use strict";
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { dispatchUpdate } = require("./push");
const device = (token, onDelete = () => {}) => ({ data: () => ({ token }), ref: { delete: async () => onDelete() } });

test("no device means no push request", async () => {
  await dispatchUpdate("owner", "event", [], { sendEachForMulticast: () => { throw Error("unexpected"); } });
});
test("chunks delivery at 500, scopes each message to the account, and sends no financial details", async () => {
  const calls = [];
  const messaging = { sendEachForMulticast: async message => {
    calls.push(message); return { responses: message.tokens.map(() => ({ success: true })) };
  }};
  await dispatchUpdate("owner", "event", Array.from({ length: 501 }, (_, i) => device("token" + i)), messaging);
  assert.deepEqual(calls.map(x => x.tokens.length), [500, 1]);
  assert.deepEqual(calls[0].data, { uid: "owner", eventId: "event" });
  assert.equal(calls[0].notification, undefined);
});
test("removes expired registrations and surfaces temporary failures", async () => {
  let deleted = false;
  await assert.rejects(dispatchUpdate("owner", "event", [device("a", () => { deleted = true; }), device("b")], {
    sendEachForMulticast: async () => ({ responses: [
      { success: false, error: { code: "messaging/registration-token-not-registered" } },
      { success: false, error: { code: "messaging/server-unavailable" } }
    ] })
  }));
  assert.equal(deleted, true);
});
