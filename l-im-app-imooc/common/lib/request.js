import $C from './config.js';
import $store from '@/store/index.js';
export default {
	// 全局配置
	common: {
		baseUrl: $C.baseUrl + $C.version,
		header: {
			'Content-Type': 'application/json;charset=UTF-8',
		},
		data: {},
		method: 'GET',
		dataType: 'json',
		token: true,
	},
	
	// get请求
	get(url, data = {}, options = {}) {
		options.url = url
		options.data = data
		options.method = 'GET'
		return this.request(options)
	},
	// post请求
	post(url, data = {}, options = {}) {
		options.url = url
		options.data = data
		options.method = 'POST'
		return this.request(options)
	},
	// delete请求
	del(url, data = {}, options = {}) {
		options.url = url
		options.data = data
		options.method = 'DELETE'
		return this.request(options)
	},
	
	// 请求 返回promise
	request(options = {}) {
		// 组织参数
		options.url = this.common.baseUrl + options.url
		options.header = options.header || this.common.header
		options.data = options.data || this.common.data
		options.method = options.method || this.common.method
		options.dataType = options.dataType || this.common.dataType
		options.token = options.token === false ? false : this.common.token

		// 请求
		return new Promise((res, rej) => {
			// 请求中...
			uni.request({
				...options,
				success: (result) => {
					// 返回原始数据
					if (options.native) {
						return res(result)
					}
					// 服务端失败
					if (result.statusCode !== 200) {
						if (options.toast !== false) {
							uni.showToast({
								title: result.data.data || '服务器失败',
								icon: 'none'
							});
						}
						// token不合法，直接退出登录
						if (result.data.code === 401) {
							return uni.reLaunch({
								url: '/pages/common/login/login',
							})
						}
						return rej(result.data)
					}
					// 成功
					let data = result.data

					res(data)
				},
				fail: (error) => {
					uni.showToast({
						title: '请求失败',
						icon: 'none'
					});
					return rej(error)
				}
			});
		})
	}
	

}
