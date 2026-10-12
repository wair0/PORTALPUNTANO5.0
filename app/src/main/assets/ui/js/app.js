(function(){
  const root=document.getElementById('app');
  const html=document.documentElement;
  let loginState={sex:'M',docNumber:''};
  let current=null;
  window.PortalDemoMode=!Bridge.isNative;

  const modeStatus=Bridge.call('app.status')
    .then(status=>{window.PortalDemoMode=!!(status&&status.demoMode);})
    .catch(()=>{window.PortalDemoMode=!Bridge.isNative;});
  const modeReady=Promise.race([
    modeStatus,
    new Promise(resolve=>setTimeout(resolve,800))
  ]);

  function toast(message){
    const item=document.createElement('div');
    item.className='toast';
    item.textContent=message;
    root.appendChild(item);
    setTimeout(()=>item.remove(),2500);
  }

  window.PortalUI={
    setInsets:(top,bottom)=>{
      html.style.setProperty('--safe-top',top+'px');
      html.style.setProperty('--safe-bottom',bottom+'px');
    },
    toast
  };

  const routes={
    home:['home.summary','home'],
    health:['health.overview','health'],
    shifts:['shifts.list','shifts'],
    procedures:['procedures.list','procedures'],
    credentials:['credentials.list','credentials'],
    payslips:['payslips.list','payslips'],
    education:['education.students','education'],
    judicial:['judicial.list','judicial'],
    civil:['civil.certificates','civil'],
    profile:['home.summary','profile']
  };

  function paint(content){root.innerHTML=content;}

  function render(){
    const route=(location.hash.replace(/^#\/?/,'')||'boot').split('?')[0];
    current=route;

    if(route==='login'){
      modeReady.then(()=>{if(current==='login')paint(Screens.login(loginState));});
      return;
    }

    const target=routes[route];
    if(!target){boot();return;}

    paint(Screens.loading());
    modeReady
      .then(()=>Bridge.call(target[0]))
      .then(data=>{if(current===route)paint(Screens[target[1]](data));})
      .catch(error=>{
        if(current!==route)return;
        if(/401|sesi[oó]n|sesion/i.test(error.message||'')){
          location.hash='#/login';
          return;
        }
        paint(Screens.error(error.message||'No se pudo completar la solicitud.'));
      });
  }

  function boot(){
    current='boot';
    Bridge.call('auth.session')
      .then(session=>location.replace(session&&session.authenticated?'#/home':'#/login'))
      .catch(()=>location.replace('#/login'));
  }

  root.addEventListener('click',event=>{
    const target=event.target.closest('[data-go],[data-back],[data-action],[data-seg] button');
    if(!target)return;

    if(target.hasAttribute('data-go')){
      location.hash=target.getAttribute('data-go');
      return;
    }

    if(target.hasAttribute('data-back')){
      if(history.length>1)history.back();
      else location.hash='#/home';
      return;
    }

    if(target.closest('[data-seg]')&&target.hasAttribute('data-val')){
      loginState.sex=target.getAttribute('data-val');
      target.parentNode.querySelectorAll('button').forEach(button=>button.classList.toggle('on',button===target));
      return;
    }

    switch(target.getAttribute('data-action')){
      case 'scan':
        Bridge.call('auth.scanDni')
          .then(result=>{
            loginState.docNumber=result.docNumber||'';
            loginState.sex=result.sex||'M';
            paint(Screens.login(loginState));
          })
          .catch(error=>toast(error.message||'No se pudo escanear el DNI.'));
        break;
      case 'theme':
        html.setAttribute('data-theme',html.getAttribute('data-theme')==='dark'?'light':'dark');
        break;
      case 'logout':
        target.disabled=true;
        Bridge.call('auth.logout')
          .then(()=>{location.hash='#/login';})
          .catch(error=>toast(error.message||'No se pudo cerrar la sesión.'))
          .finally(()=>{target.disabled=false;});
        break;
      case 'retry':
        render();
        break;
      default:
        toast('Función prevista para una fase posterior');
    }
  });

  root.addEventListener('submit',event=>{
    event.preventDefault();
    const form=event.target;
    if(form.getAttribute('data-form')!=='login')return;

    const payload={
      docNumber:form.docNumber.value.replace(/\D/g,''),
      password:form.password.value,
      sex:loginState.sex
    };

    if(payload.docNumber.length<7||!payload.password){
      toast('Completá DNI y contraseña');
      return;
    }

    const submit=form.querySelector('[type="submit"]');
    if(submit)submit.disabled=true;
    Bridge.call('auth.login',payload)
      .then(()=>{location.hash='#/home';})
      .catch(error=>toast(error.message||'No se pudo iniciar sesión.'))
      .finally(()=>{if(submit)submit.disabled=false;});
  });

  addEventListener('hashchange',render);
  if(!location.hash)boot();
  else render();
})();