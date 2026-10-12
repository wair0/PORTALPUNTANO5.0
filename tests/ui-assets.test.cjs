const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const assetsRoot = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'ui');
const html = fs.readFileSync(path.join(assetsRoot, 'index.html'), 'utf8');
const css = fs.readFileSync(path.join(assetsRoot, 'css', 'visual-identity.css'), 'utf8');
const app = fs.readFileSync(path.join(assetsRoot, 'js', 'app.js'), 'utf8');

test('todos los estilos y scripts enlazados existen como recursos locales', () => {
  const references = [...html.matchAll(/(?:href|src)="([^"]+)"/g)]
    .map(match => match[1])
    .filter(reference => !/^(?:https?:)?\/\//i.test(reference) && !reference.startsWith('data:'));

  assert.ok(references.length > 0, 'debe haber recursos locales enlazados');
  for (const reference of references) {
    assert.ok(
      fs.existsSync(path.join(assetsRoot, reference)),
      'falta el recurso enlazado: ' + reference
    );
  }
});

test('la política de seguridad evita conexiones remotas desde la interfaz', () => {
  assert.match(html, /default-src 'none'/);
  assert.match(html, /connect-src 'self'/);
  assert.match(html, /script-src 'self'/);
  assert.match(html, /style-src 'self'/);
  assert.match(html, /object-src 'none'/);
  assert.match(html, /frame-src 'none'/);
  assert.match(html, /worker-src 'none'/);
});

test('el WebView bloquea accesos a archivos y contenido desde orígenes locales', () => {
  const activity = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'ui', 'MainActivity.kt'),
    'utf8'
  );
  assert.match(activity, /allowFileAccess\s*=\s*false/);
  assert.match(activity, /allowContentAccess\s*=\s*false/);
  assert.match(activity, /allowFileAccessFromFileURLs\s*=\s*false/);
  assert.match(activity, /allowUniversalAccessFromFileURLs\s*=\s*false/);
});

test('la capa visual incluye foco accesible, pantallas pequeñas y movimiento reducido', () => {
  assert.match(css, /:focus-visible/);
  assert.match(css, /max-width:\s*359px/);
  assert.match(css, /min-width:\s*600px/);
  assert.match(css, /prefers-reduced-motion:\s*reduce/);
});

test('las pantallas principales tienen una ruta de aplicación declarada', () => {
  for (const route of [
    'home', 'health', 'shifts', 'procedures', 'credentials',
    'payslips', 'education', 'judicial', 'civil', 'profile'
  ]) {
    assert.match(app, new RegExp('\\b' + route + ':\\s*\\['), 'falta la ruta ' + route);
  }
});

test('la pantalla principal y el acceso al escaneo conservan los elementos visuales del diseño', () => {
  const screens = fs.readFileSync(path.join(assetsRoot, 'js', 'screens.js'), 'utf8');
  assert.match(screens, /class="orbit"/);
  assert.match(screens, /class="scan"/);
  assert.match(css, /\.hero \.orbit/);
  assert.match(css, /button\.scan \.beam/);
  assert.match(css, /@keyframes scanline/);
});

test('la navegación inferior identifica su región y la pestaña actual para lectores de pantalla', () => {
  const screens = fs.readFileSync(path.join(assetsRoot, 'js', 'screens.js'), 'utf8');
  assert.match(screens, /aria-label="Navegación principal"/);
  assert.match(screens, /aria-current="page"/);
  assert.match(screens, /button type="button" class="tab/);
});

test('los datos del perfil se escapan antes de insertarse en el HTML', () => {
  const vm = require('node:vm');
  const screensSource = fs.readFileSync(path.join(assetsRoot, 'js', 'screens.js'), 'utf8');
  const window = {
    Icons: { svg: () => '<svg></svg>', mark: '<svg></svg>' },
    PortalDemoMode: false
  };
  vm.runInNewContext(screensSource, { window });
  const html = window.Screens.profile({
    profile: {
      fullName: '<img src=x onerror=alert(1)>',
      docNumber: '" autofocus onfocus="alert(1)'
    }
  });
  assert.match(html, /&lt;img src=x onerror=alert\(1\)&gt;/);
  assert.match(html, /&quot; autofocus onfocus=&quot;alert\(1\)/);
  assert.doesNotMatch(html, /<img src=x onerror=alert\(1\)>/);
});

test('el puente nativo vence las solicitudes y descarta respuestas tardías', async () => {
  const vm = require('node:vm');
  const bridgeSource = fs.readFileSync(path.join(assetsRoot, 'js', 'bridge.js'), 'utf8');
  let timeoutCallback;
  let requestId;
  const window = {
    PortalNative: {
      request: id => { requestId = id; }
    }
  };

  vm.runInNewContext(bridgeSource, {
    window,
    setTimeout: callback => {
      timeoutCallback = callback;
      return 1;
    },
    clearTimeout: () => {}
  });

  const request = window.Bridge.call('home.summary');
  assert.equal(typeof timeoutCallback, 'function');
  assert.equal(typeof requestId, 'number');
  timeoutCallback();
  await assert.rejects(request, /La solicitud tardó demasiado/);

  assert.doesNotThrow(() => window.PortalBridge.resolve(requestId, { stale: true }));
  await assert.rejects(request, /La solicitud tardó demasiado/);
});

test('la capa de red no envía tokens antiguos al iniciar sesión e invalida sesiones ante HTTP 401', () => {
  const network = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'remote', 'NetworkModule.kt'),
    'utf8'
  );
  assert.match(network, /endsWith\("\/login",\s*ignoreCase\s*=\s*true\)/);
  assert.match(network, /takeIf\s*\{\s*it\.isNotBlank\(\)\s*\}/);
  assert.match(network, /response\.code\s*==\s*401/);
  assert.match(network, /session\.clear\(\)/);
});
