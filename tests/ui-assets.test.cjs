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

test('Android bloquea tráfico HTTP sin excepciones por dominio', () => {
  const root = path.join(__dirname, '..', 'app', 'src', 'main');
  const manifest = fs.readFileSync(path.join(root, 'AndroidManifest.xml'), 'utf8');
  const networkConfig = fs.readFileSync(path.join(root, 'res', 'xml', 'network_security_config.xml'), 'utf8');
  assert.match(manifest, /android:networkSecurityConfig="@xml\/network_security_config"/);
  assert.match(networkConfig, /<base-config\s+cleartextTrafficPermitted="false">/);
  assert.match(networkConfig, /<certificates\s+src="system"\s*\/>/);
  assert.doesNotMatch(networkConfig, /cleartextTrafficPermitted="true"/);
  assert.doesNotMatch(networkConfig, /<domain-config/);
});


test('las rutas usadas por la interfaz existen en el contrato del puente Android', () => {
  const root = path.join(__dirname, '..', 'app', 'src', 'main');
  const router = fs.readFileSync(
    path.join(root, 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'ui', 'web', 'BridgeRouter.kt'),
    'utf8'
  );
  const screens = fs.readFileSync(path.join(assetsRoot, 'js', 'screens.js'), 'utf8');
  const routes = [...app.matchAll(/\w+:\s*\['([^']+)'\s*,\s*'([^']+)'\]/g)];
  assert.ok(routes.length > 0, 'debe haber rutas de pantalla declaradas');

  for (const [, bridgeRoute, screenName] of routes) {
    assert.ok(
      router.includes('"' + bridgeRoute + '"'),
      'la ruta del puente no está registrada: ' + bridgeRoute
    );
    assert.ok(
      screens.includes('S.' + screenName + '='),
      'la pantalla no está implementada: ' + screenName
    );
  }

  const calledRoutes = [...app.matchAll(/Bridge\.call\('([^']+)'/g)].map(match => match[1]);
  for (const bridgeRoute of calledRoutes) {
    assert.ok(
      router.includes('"' + bridgeRoute + '"'),
      'la interfaz llama a una ruta sin contrato nativo: ' + bridgeRoute
    );
  }
});

test('la descarga pendiente no se presenta como una operación implementada', () => {
  const router = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'ui', 'web', 'BridgeRouter.kt'),
    'utf8'
  );
  const docs = fs.readFileSync(path.join(__dirname, '..', 'docs', 'ESTADO_TECNICO_Y_CONTRATOS.md'), 'utf8');
  assert.ok(router.replace(/\s+/g, " ").includes('"files.download" -> error("Descarga disponible en la próxima fase")'));
  assert.ok(docs.includes("| `files.download` | Devuelve error explícito indicando que la descarga aún no está disponible | Pendiente |"));
});


test('la presentación visual se carga desde recursos locales sin depender de Internet', () => {
  const activity = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'ui', 'MainActivity.kt'),
    'utf8'
  );
  assert.match(activity, /WebViewAssetLoader/);
  assert.match(activity, /WebViewAssetLoader\.DEFAULT_DOMAIN/);
  assert.match(activity, /assets\/ui/);
  assert.doesNotMatch(html, /(?:src|href)=["']https?:\/\//i);
  assert.doesNotMatch(html, /@import\s+url\(["']?https?:\/\//i);
});

test('el puente libera solicitudes agotadas y no deja respuestas tardías pendientes', async () => {
  const vm = require('node:vm');
  const bridgeSource = fs.readFileSync(path.join(assetsRoot, 'js', 'bridge.js'), 'utf8');
  let timer;
  let requestedId;
  const window = { PortalNative: { request: id => { requestedId = id; } } };
  vm.runInNewContext(bridgeSource, {
    window,
    setTimeout: callback => { timer = callback; return 1; },
    clearTimeout: () => {}
  });

  const pending = window.Bridge.call('home.summary');
  timer();
  await assert.rejects(pending, /La solicitud tardó demasiado/);
  assert.doesNotThrow(() => window.PortalBridge.resolve(requestedId, { late: true }));
});


test('la sesión se guarda como una unidad y rechaza tokens vacíos', () => {
  const session = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'local', 'SessionStore.kt'),
    'utf8'
  );
  const auth = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'repository', 'AuthRepositoryImpl.kt'),
    'utf8'
  );
  assert.match(session, /fun saveSession\(token: String, docNumber: String, fullName: String\)/);
  assert.match(session, /require\(token\.isNotBlank\(\)\)/);
  assert.match(session, /prefs\.edit\(\)[\s\S]*putString\("token", token\)[\s\S]*putString\("doc", docNumber\)[\s\S]*putString\("name", fullName\)[\s\S]*apply\(\)/);
  assert.equal((auth.match(/session\.saveSession\(/g) || []).length, 2, 'el login demo y el login real deben guardar la sesión de forma atómica');
  assert.doesNotMatch(auth, /session\.token\s*=/);
  assert.doesNotMatch(auth, /session\.docNumber\s*=/);
  assert.doesNotMatch(auth, /session\.fullName\s*=/);
});

test('el cierre de sesión elimina todos los datos locales de identidad', () => {
  const session = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'local', 'SessionStore.kt'),
    'utf8'
  );
  const auth = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'repository', 'AuthRepositoryImpl.kt'),
    'utf8'
  );
  assert.match(session, /fun clear\(\) = prefs\.edit\(\)\.clear\(\)\.apply\(\)/);
  assert.match(auth, /override fun logout\(\) = session\.clear\(\)/);
});

test('los endpoints de servicios se configuran externamente y no incluyen HTTP heredado', () => {
  const gradle = fs.readFileSync(path.join(__dirname, '..', 'app', 'build.gradle.kts'), 'utf8');
  for (const key of ['PORTAL_BASE_URL', 'SALUD_BASE_URL', 'RECIPE_BASE_URL', 'EDU_BASE_URL']) {
    assert.match(gradle, new RegExp('gradleProperty\\("' + key + '"\\)'));
    assert.match(gradle, new RegExp('environmentVariable\\("' + key + '"\\)'));
  }
  assert.equal(gradle.includes('http://'), false, 'no debe haber endpoints HTTP');
  for (const legacy of ['puntano.gob.ar', 'registrosmedicos.sanluis', 'Escuelas3punto0']) { assert.equal(gradle.includes(legacy), false, 'endpoint heredado: ' + legacy); }
});

test('la aplicación Android se compila sin modo demo y valida la URL al realizar solicitudes', () => {
  const gradle = fs.readFileSync(path.join(__dirname, '..', 'app', 'build.gradle.kts'), 'utf8');
  const network = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'remote', 'NetworkModule.kt'),
    'utf8'
  );
  assert.match(gradle, /buildConfigField\("boolean",\s*"DEMO_MODE",\s*"false"\)/);
  assert.match(network, /PORTAL_BASE_URL\.trim\(\)/);
  assert.match(network, /configuredBaseUrl\.isBlank\(\)/);
  assert.match(network, /PORTAL_BASE_URL con una dirección HTTPS oficial/);
  assert.match(network, /startsWith\("https:\/\//);
  assert.match(network, /throw IOException\(/);
  assert.match(network, /portal-puntano-unconfigured\.invalid/);
});

test('el cliente HTTP protege rutas autenticadas y limpia sesión ante 401', () => {
  const network = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'remote', 'NetworkModule.kt'),
    'utf8'
  );
  assert.match(network, /endsWith\("\/login",\s*ignoreCase\s*=\s*true\)/);
  assert.match(network, /if\s*\(!isLoginRequest\)\s*session\.token\?\.takeIf\s*\{\s*it\.isNotBlank\(\)\s*\}\?\.let/);
  assert.match(network, /header\("Authorization",\s*"Bearer \$it"\)/);
  assert.match(network, /if\s*\(response\.code\s*==\s*401\)\s*\{[\s\S]*?session\.clear\(\)/);
  assert.match(network, /connectTimeout\(20,\s*TimeUnit\.SECONDS\)/);
  assert.match(network, /readTimeout\(30,\s*TimeUnit\.SECONDS\)/);
});

test('la navegación WebView solo confía en recursos locales HTTPS y bloquea esquemas personalizados', () => {
  const client = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'ui', 'web', 'PortalWebViewClient.kt'),
    'utf8'
  );
  assert.match(client, /uri\.scheme\.equals\("https",\s*ignoreCase\s*=\s*true\)/);
  assert.match(client, /uri\.host\.equals\(WebViewAssetLoader\.DEFAULT_DOMAIN,\s*ignoreCase\s*=\s*true\)/);
  assert.match(client, /uri\.port\s*==\s*-1\s*\|\|\s*uri\.port\s*==\s*443/);
  assert.match(client, /uri\.userInfo\s*==\s*null/);
  assert.match(client, /uri\.path\.orEmpty\(\)\.startsWith\("\/assets\/"\)/);
  assert.match(client, /uri\.scheme\.equals\("http",\s*ignoreCase\s*=\s*true\)/);
  assert.match(client, /uri\.scheme\.equals\("https",\s*ignoreCase\s*=\s*true\)/);
  assert.match(client, /return true/);
  assert.match(client, /Intent\(Intent\.ACTION_VIEW,\s*uri\)/);
});

test('la base reservada solo permite construir Retrofit y la red exige configuración HTTPS real', () => {
  const network = fs.readFileSync(
    path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'gpsl', 'st', 'portalpuntano5', 'data', 'remote', 'NetworkModule.kt'),
    'utf8'
  );
  assert.match(network, /configuredBaseUrl\.ifBlank\s*\{\s*"https:\/\/portal-puntano-unconfigured\.invalid\/"\s*\}/);
  assert.match(network, /baseUrl\(effectiveBaseUrl\)/);
  assert.match(network, /throw IOException\(/);
  assert.doesNotMatch(network, /if\s*\(!BuildConfig\.DEMO_MODE\)\s*\{\s*require\(/);
});
