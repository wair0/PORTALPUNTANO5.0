(function(){
  const root=document.getElementById('app');
  const html=document.documentElement;
  let loginState={sex:'M',docNumber:''};
  let current=null;
  let detailItems=[];
  let detailRoute='home';
  window.PortalDemoMode=!Bridge.isNative;

  const modeStatus=Bridge.call('app.status')
    .then(status=>{
      window.PortalDemoMode=!!(status&&status.demoMode);
      syncDemoBanner();
    })
    .catch(()=>{
      window.PortalDemoMode=!Bridge.isNative;
      syncDemoBanner();
    });
  const modeReady=Promise.race([
    modeStatus,
    new Promise(resolve=>setTimeout(resolve,800))
  ]);

  function syncDemoBanner(){
    const screen=root.querySelector('.screen');
    if(!screen)return;
    const existing=screen.querySelector('.demo-banner');
    if(window.PortalDemoMode&&!existing){
      const banner=document.createElement('div');
      banner.className='demo-banner';
      banner.setAttribute('role','status');
      const title=document.createElement('b');
      title.textContent='MODO DEMOSTRACIÓN';
      const description=document.createElement('span');
      description.textContent='Los datos mostrados son ficticios y no representan registros oficiales.';
      banner.append(title,description);
      screen.prepend(banner);
    }else if(!window.PortalDemoMode&&existing){
      existing.remove();
    }
  }

  function toast(message){
    const item=document.createElement('div');
    item.className='toast';
    item.setAttribute('role','status');
    item.setAttribute('aria-live','polite');
    item.setAttribute('aria-atomic','true');
    item.textContent=String(message||'Ocurrió un error inesperado.');
    root.querySelectorAll('.toast').forEach(existing=>existing.remove());
    root.appendChild(item);
    setTimeout(()=>item.remove(),3500);
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
    shifts:['official.section','officialSection','shifts'],
    procedures:['official.section','officialSection','procedures'],
    credentials:['official.section','officialSection','credentials'],
    payslips:['official.section','officialSection','payslips'],
    education:['official.section','officialSection','education'],
    judicial:['official.section','officialSection','judicial'],
    civil:['official.section','officialSection','civil'],
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
      .then(()=>Bridge.call(target[0],target[2]?{path:target[2]}:{}))
      .then(data=>{if(current===route){detailItems=Array.isArray(data)?data:(data&&Array.isArray(data.records)?data.records:[]);detailRoute=route;paint(Screens[target[1]](data));}})
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
    const target=event.target.closest('[data-go],[data-back],[data-action],[data-item],[data-official-path],[data-seg] button');
    if(!target)return;

    if(target.hasAttribute('data-item')){
      const index=Number(target.getAttribute('data-item'));
      if(Number.isInteger(index)&&detailItems[index]){
        const item=detailItems[index];
        const titles={shifts:'Turno',procedures:'Trámite',credentials:'Credencial',payslips:'Recibo de sueldo',education:'Alumno',judicial:'Expediente judicial',civil:'Certificado',health:'Registro de salud'};
        const tabs={shifts:'shifts',procedures:'procedures',credentials:'credentials',payslips:'profile',education:'profile',judicial:'profile',civil:'profile',health:'home'};
        const openDetail=data=>{
          if(current!=='detail')return;
          paint(Screens.detail(data,titles[detailRoute]||'Detalle',tabs[detailRoute]||'profile'));
        };
        if(detailRoute==='judicial'&&item.id){
          current='detail'; paint(Screens.loading());
          Bridge.call('judicial.actions',{id:String(item.id)})
            .then(data=>openDetail({expediente:item,actuaciones:data}))
            .catch(error=>{if(current==='detail')paint(Screens.error(error.message||'No se pudieron cargar las actuaciones.'));});
        }else if(detailRoute==='education'&&item.id){
          current='detail'; paint(Screens.loading());
          Bridge.call('education.studentDetail',{studentId:String(item.id)})
            .then(data=>openDetail({alumno:item,datosEducativos:data}))
            .catch(error=>{if(current==='detail')paint(Screens.error(error.message||'No se pudieron cargar los datos educativos.'));});
        }else{
          current='detail';
          paint(Screens.detail(item,titles[detailRoute]||'Detalle',tabs[detailRoute]||'profile'));
        }
      }
      return;
    }

    if(target.hasAttribute('data-official-path')){
      const key=target.getAttribute('data-official-path');
      current='official-section';
      paint(Screens.loading());
      Bridge.call('health.section',{path:key})
        .then(data=>{
          if(current!=='official-section')return;
          detailItems=Array.isArray(data.records)?data.records:[];
          detailRoute=key;
          paint(Screens.officialSection(data));
        })
        .catch(error=>{
          if(current==='official-section')paint(Screens.error(error.message||'No se pudieron extraer los datos oficiales.'));
        });
      return;
    }

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
      case 'detail-back':
      case 'official-back':
        render();
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