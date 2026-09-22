export async function getCpiSeries(db) {
  const snapshot = await db.collection("cpi").orderBy("__name__").get();
  return snapshot.docs.map((doc) => ({ period: doc.id, index: doc.data().index }));
}
