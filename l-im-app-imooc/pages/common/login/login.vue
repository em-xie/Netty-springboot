<template>
	<view>
		<view>
			<view class="flex align-center justify-center"
			style="height: 350rpx;">
				<text style="font-size: 50rpx;">LOGO</text>
			</view>
			<view class="px-3">
				<input type="text" class="bg-light px-3 mb-3 font" :placeholder=getUserNameText style="height: 100rpx;" v-model="form.userName"/>
				<input type="text" class="bg-light px-3 mb-3 font" placeholder="请输入密码" style="height: 100rpx;" v-model="form.password"/>
				<input v-if="type === 'reg'" type="text" class="bg-light px-3 mb-3 font" placeholder="请输入确认密码" style="height: 100rpx;"  v-model="form.repassword"/>
			</view>
			<view class="p-3 flex align-center justify-center">
				<view class="main-bg-color rounded p-3 flex align-center justify-center flex-1"
				hover-class="main-bg-hover-color" @click="submit">
					<text class="text-white font-md">{{type === 'login' ? '登 录' : '注 册'}}</text>
				</view>
			</view>
			
			<view class="flex align-center justify-center">
				<text class="text-light-muted font p-2" @click="changeType">{{type === 'login' ?  '注册账号' : '马上登录'}}</text>
				<text class="text-light-muted font">|</text>
				<text class="text-light-muted font p-2">忘记密码</text>
			</view>
		</view>
		
	</view>
</template>

<script>
	import $H from '@/common/lib/request.js';
	import Vue from 'vue'
	
	const ListenerMap = {
	    onSocketConnectEvent: (option, status, data) => {
	        console.log("已建立连接:" + JSON.stringify(status));
	    },
	    onSocketErrorEvent: (error) => {
	        console.log("连接出现错误:" + userId);
	    },
	    onSocketReConnectEvent: () => {
	        console.log("正在重连:" );
	    },
	    onSocketCloseEvent: () => {
	        console.log("连接关闭:" );
	    },onSocketReConnectSuccessEvent: () => {
		    console.log("重连成功" );
		},
	    onTestMessage: (e) => {
	        console.log("onTestMessage ：" + e );
	    },
	    onP2PMessage: (e) => {
			console.log("onP2PMessage ：" + e );
			e = JSON.parse(e)
			uni.$emit('P2PMessage', e.data);
	    },
		onLogin: (uid) => {
		    console.log("用户"+uid+"登陆sdk成功" );
		}
		
	};
	
	export default {
		data() {
			return {
				type:"login",
				form: {
					userName:"lld",
					password:"lld",
					repassword:"",
					loginType:1
				},
				show:true
			}
		},
		created() {
			
		},
		methods: {
			changeType(){
				// this.type = this.type === 'login' ? 'reg' : 'login'
				// this.form = {
				// 	username:"",
				// 	password:"",
				// 	repassword:""
				// }
			},
			submit() {
				if(this.type === 'login'){
					$H.post('/login',this.form,{
						token:false
					}).then(res=>{
						console.log(res)
						if(res.code !== 200){
							uni.showToast({
							    title: res.msg,
							    icon: 'none'
							});
						}else{
							//app服务登录成功，去登录im
							const lim = this.imsdk
							const sdk = lim.im;
							
								var userId = this.form.userName;
								var listeners = {};
								for (const v in ListenerMap) {
								    listeners[v] = ListenerMap[v];
								}
								
								sdk.init("http://127.0.0.1:8000/v1",res.data.appId,res.data.userId,res.data.imUserSign,listeners,function (sdk) {
							    if(sdk){
							        console.log('sdk 成功连接的回调, 可以使用 sdk 请求数据了.');
									return uni.switchTab({
										url:"/pages/tabbar/index/index"
									})
							    }
							    else {
							        console.log('sdk 初始化失败.');
							    }
							});
							
							
						}
					})
					
				}
			}
		},
		computed:{
			getUserNameText(){
				if(this.type === 'login'){
					return "请输入用户名";
				}
				else if(this.type === 'reg'){
					return "请输入手机号";
				}
			}
		}
	}
</script>

<style>

</style>
