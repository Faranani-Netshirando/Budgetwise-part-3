import { after, before, beforeEach, test } from "node:test";
import { readFileSync } from "node:fs";
import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, deleteDoc } from "firebase/firestore";
import { ref, uploadBytes, getBytes } from "firebase/storage";
let env;
before(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-budgetwise", firestore: {
    rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8")
  }, storage: { rules: readFileSync(new URL("../storage.rules", import.meta.url), "utf8") } });
});
beforeEach(() => env.clearFirestore());
after(() => env?.cleanup());
const transaction = { id: "one", revision: "revision", deleted: false, type: "EXPENSE", category: "Food",
  amount: 10, date: 1793872800000, description: "Lunch" };
test("owner can create/read a transaction; other accounts and signed out users cannot", async () => {
  const own = env.authenticatedContext("a").firestore();
  await assertSucceeds(setDoc(doc(own, "users/a/transactions/one"), transaction));
  await assertSucceeds(getDoc(doc(own, "users/a/transactions/one")));
  await assertFails(getDoc(doc(env.authenticatedContext("b").firestore(), "users/a/transactions/one")));
  await assertFails(setDoc(doc(env.authenticatedContext("b").firestore(), "users/a/transactions/one"), transaction));
  await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), "users/a/transactions/one")));
});
test("rejects invalid values and id mismatches; accepts deletions as tombstones", async () => {
  const target = doc(env.authenticatedContext("a").firestore(), "users/a/transactions/one");
  await assertFails(setDoc(target, { ...transaction, amount: -10 }));
  await assertFails(setDoc(target, { ...transaction, id: "two" }));
  await assertSucceeds(setDoc(target, { ...transaction, deleted: true }));
  await assertFails(deleteDoc(target));
});
test("receipt blobs require owner identity and image content type", async () => {
  const path = "users/a/receipts/one/revision";
  await assertSucceeds(uploadBytes(ref(env.authenticatedContext("a").storage(), path), new Uint8Array([1,2,3]), { contentType: "image/jpeg" }));
  await assertSucceeds(getBytes(ref(env.authenticatedContext("a").storage(), path)));
  await assertFails(getBytes(ref(env.authenticatedContext("b").storage(), path)));
  await assertFails(uploadBytes(ref(env.authenticatedContext("a").storage(), path + "text"), new Uint8Array([1]), { contentType: "text/plain" }));
});
