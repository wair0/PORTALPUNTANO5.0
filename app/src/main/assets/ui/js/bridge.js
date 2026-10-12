window.Bridge=(function(){
  const pending=new Map();
  let seq=0;
  const native=window.PortalNative;

  function call(route,payload){
    if(!native)return window.Mock.handle(route,payload||{});
    return new Promise((resolve,reject)=>{
      const id=++seq;
      pending.set(id,{resolve,reject});
      try{
        native.request(id,route,JSON.stringify(payload||{}));
      }catch(error){
        pending.delete(id);
        reject(error instanceof Error?error:new Error(String(error||'No se pudo enviar la solicitud al sistema')));
      }
    });
  }

  function settle(id,action,value){
    const p=pending.get(id);
    if(!p)return;
    pending.delete(id);
    p[action](value);
  }

  window.PortalBridge={
    resolve:(id,data)=>settle(id,'resolve',data),
    reject:(id,message)=>settle(id,'reject',new Error(message||'Error'))
  };

  return{call,isNative:!!native};
})();