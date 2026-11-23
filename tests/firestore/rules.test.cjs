const { createRequire } = require('node:module');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { test, before, after } = require('node:test');
const req = process.env.FIREBASE_TEST_MODULES
  ? createRequire(resolve(process.env.FIREBASE_TEST_MODULES, 'package.json')) : require;
const { initializeTestEnvironment, assertSucceeds, assertFails } = req('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, getDocs, collection, updateDoc, deleteDoc } = req('firebase/firestore');
let env;
const id = '3eb17c01-b365-43a7-bbf0-12c051b8c601';
const valid = () => ({ claveSincronizacion: id, titulo: 'Prueba reglas Teclab', descripcion: null,
  categoria: 'Estudio', prioridad: 'ALTA', estado: 'PENDIENTE', fechaCreacion: 1000,
  fechaModificacion: 1000, ubicacion: null, aviso: null, versionLocal: 1, eliminado: false });
const ref = (uid, owner = 'A', rid = id) => doc(
  (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore(),
  `users/${owner}/reminders/${rid}`);
before(async () => {
  // Host y proyecto ficticio fijados: no puede apuntar accidentalmente a producción.
  env = await initializeTestEnvironment({ projectId: 'demo-teclab-rules',
    firestore: { host: '127.0.0.1', port: 8088, rules: readFileSync('firestore.rules', 'utf8') } });
});
after(async () => { await env?.cleanup(); });
test('anónimo: lectura y escritura denegadas', async () => {
  await assertFails(getDoc(ref(null))); await assertFails(setDoc(ref(null), valid()));
});
test('A: crear, leer, editar, reintentar y eliminar lógicamente sus documentos', async () => {
  const r = ref('A');
  await assertSucceeds(setDoc(r, valid()));
  await assertSucceeds(getDoc(r));
  await assertSucceeds(setDoc(r, valid()));
  await assertSucceeds(updateDoc(r, { titulo: 'Prueba editada', versionLocal: 2 }));
  await assertSucceeds(updateDoc(r, { eliminado: true, versionLocal: 3 }));
});
test('B: lectura, listado y escritura bajo A denegados', async () => {
  await assertFails(getDoc(ref('B')));
  await assertFails(getDocs(collection(env.authenticatedContext('B').firestore(), 'users/A/reminders')));
  await assertFails(setDoc(ref('B'), valid()));
});
test('campos adicionales, faltantes, categoría, UUID y tipos inválidos denegados', async () => {
  const r = ref('A', 'A', '4eb17c01-b365-43a7-bbf0-12c051b8c601');
  const base = { ...valid(), claveSincronizacion: r.id };
  const missing = { ...base }; delete missing.eliminado;
  for (const data of [missing, {...base, extra: true}, {...base, categoria: 'Otra'},
    {...base, versionLocal: '1'}, {...base, fechaCreacion: 1.5}, {...base, eliminado: 'false'},
    {...base, prioridad: 'URGENTE'}, {...base, titulo: 'x'}, {...base, descripcion: 5},
    {...base, claveSincronizacion: id}]) await assertFails(setDoc(r, data));
  await assertFails(setDoc(ref('A', 'A', 'no-uuid'), {...base, claveSincronizacion: 'no-uuid'}));
});
test('eliminación física denegada al propietario', async () => { await assertFails(deleteDoc(ref('A'))); });
test('versión antigua, cambio sin incrementar versión y fecha de creación alterada denegados', async () => {
  await assertFails(updateDoc(ref('A'), {versionLocal: 1}));
  await assertFails(updateDoc(ref('A'), {titulo: 'Cambio sin versión'}));
  await assertFails(updateDoc(ref('A'), {versionLocal: 4, fechaCreacion: 2000}));
});
test('ruta global sin permisos', async () => {
  await assertFails(setDoc(doc(env.authenticatedContext('A').firestore(), 'reminders', id), valid()));
});
