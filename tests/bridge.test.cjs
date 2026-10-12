const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync('app/src/main/assets/ui/js/bridge.js', 'utf8');

function loadBridge(native, mock) {
  const window = { PortalNative: native, Mock: mock || { handle: () => Promise.resolve({ demo: true }) } };
  vm.runInNewContext(source, { window });
  return window;
}

test('usa el adaptador de demostración cuando no existe el puente nativo', async () => {
  const calls = [];
  const window = loadBridge(null, { handle: (route, payload) => {
    calls.push({ route, payload });
    return Promise.resolve({ demo: true });
  }});
  const result = await window.Bridge.call('app.status', { from: 'test' });
  assert.equal(JSON.stringify(result), '{"demo":true}');
  assert.equal(calls[0].route, 'app.status');
  assert.equal(calls[0].payload.from, 'test');
  assert.equal(window.Bridge.isNative, false);
});

test('resuelve solicitudes nativas y libera la solicitud pendiente', async () => {
  const window = loadBridge({ request(id, route, payload) {
    assert.equal(route, 'home.summary');
    assert.equal(JSON.parse(payload).refresh, true);
    window.PortalBridge.resolve(id, { ok: true });
  }});
  const result = await window.Bridge.call('home.summary', { refresh: true });
  assert.equal(JSON.stringify(result), '{"ok":true}');
  assert.equal(window.Bridge.isNative, true);
});

test('rechaza y limpia la solicitud si el puente nativo falla al enviarla', async () => {
  const window = loadBridge({ request() { throw new Error('fallo de transporte'); } });
  await assert.rejects(window.Bridge.call('home.summary'), /fallo de transporte/);
});

test('convierte rechazos del lado nativo en errores de JavaScript', async () => {
  const window = loadBridge({ request(id) {
    window.PortalBridge.reject(id, 'sesión expirada');
  }});
  await assert.rejects(window.Bridge.call('auth.session'), /sesión expirada/);
});
