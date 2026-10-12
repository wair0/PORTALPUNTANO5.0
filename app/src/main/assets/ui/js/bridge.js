window.Bridge=(function(){
  const pending=new Map();
  const REQUEST_TIMEOUT_MS=30000;
  let seq=0;
  const native=window.PortalNative;

  function call(route,payload){
    if(!native)return window.Mock.handle(route,payload||{});
    return new Promise((resolve,reject)=>{
      const id=++seq;
      const timer=setTimeout(()=>{
        const request=pending.get(id);
        if(!request)return;
        pending.delete(id);
        request.reject(new Error('La solicitud tardó demasiado. Comprueba la conexión e inténtalo de nuevo.'));
      },REQUEST_TIMEOUT_MS);
      pending.set(id,{resolve,reject,timer});
      try{
        native.request(id,route,JSON.stringify(payload||{}));
      }catch(error){
        const request=pending.get(id);
        if(request){
          clearTimeout(request.timer);
          pending.delete(id);
        }
        reject(error instanceof Error?error:new Error(String(error||'No se pudo enviar la solicitud al sistema')));
      }
    });
  }

  function settle(id,action,value){
    const request=pending.get(id);
    if(!request)return;
    clearTimeout(request.timer);
    pending.delete(id);
    request[action](value);
  }

  window.PortalBridge={
    resolve:(id,data)=>settle(id,'resolve',data),
    reject:(id,message)=>settle(id,'reject',new Error(message||'Error'))
  };

  return{call,isNative:!!native};
})();