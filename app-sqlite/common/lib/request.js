import $C from './config.js';
import $U from './util.js';

export default {
    // 全局配置
    common:{
        baseUrl:$C.baseUrl + $C.version,
        header:{
            'Content-Type':'application/json;charset=UTF-8',
        },
        data:{},
        method:'GET',
        dataType:'json',
        token:true,
    },
    // 请求 返回promise
    request(options = {}){
        // 组织参数
        options.url = this.common.baseUrl  + options.url
        options.header = options.header || this.common.header
        options.data = options.data || this.common.data
        options.method = options.method || this.common.method
        options.dataType = options.dataType || this.common.dataType
        options.token = options.token === false ?  false : this.common.token
		// params
		options.url = options.url + "?appId=10000"
		console.log(options.url)

        // 请求
        return new Promise((res,rej)=>{
            // 请求中...
            uni.request({
                ...options,
                success: (result) => {
                    // 返回原始数据
                    if(options.native){
                        return res(result)
                    }
					console.log(result.statusCode)
                    // 服务端失败
                    if(result.statusCode !== 200){
                        if (options.toast !== false) {
                            uni.showToast({
                                title: result.data.data || '服务端失败',
                                icon: 'none'
                            });
                        }
                        return rej(result.data) 
                    }
                    // 其他验证...
                    // 成功
                    let data = result.data
					
                    res(data)
                },
                fail: (error) => {
                    uni.showToast({ title: error.errMsg || '请求失败', icon: 'none' });
                    return rej(error)
                }
            });
        })
    },
	setQueryConfig(params){ 
	　　var _str = "?"; 
	　　for(var o in params){ 
	 　　　　if(params[o] != -1){ 
	 　　　　　　_str += o + "=" + params[o] + "&"; 
	 　　　　} 
	　　} 
	　　 var _str = _str.substring(0, _str.length-1); //末尾是&
	　　 return _str; 
	},
    // get请求
    get(url,data = {},options = {}){
        options.url = url
        options.data = data
        options.method = 'GET'
        return this.request(options)
    },
    // post请求
    post(url,data = {},options = {}){
        options.url = url
        options.data = data
        options.method = 'POST'
		console.log(options)
        return this.request(options)
    },
    // delete请求
    del(url,data = {},options = {}){
        options.url = url
        options.data = data
        options.method = 'DELETE'
        return this.request(options)
    }
}