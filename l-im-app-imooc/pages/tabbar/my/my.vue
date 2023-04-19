<template>
	<view class="page">
		<!-- 导航栏 -->
		<nav-bar bgColor="bg-white">
			<icon-button slot="right" :icon="'\ue6ed'"></icon-button>
		</nav-bar>
		
		<list-item :cover="user.photo ? user.photo : '/static/images/userpic.jpg'"
		coverSize="120" :title="user.nickName" showRight @click="open('my/userinfo/userinfo')">
			<view class="flex flex-column">
				<text class="text-dark font-lg font-weight-bold">{{user.nickName}}</text>
				<text class="text-light-muted font mt-2">仿微信号：{{user.nickName}}</text>
			</view>
			<view slot="right">
				<text class="iconfont font-md text-light-muted">&#xe614;</text>
			</view>
		</list-item>
		
		<divider></divider>
		
		<list-item title="支付" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe66c;</text>
		</list-item>
		
		<divider></divider>

		<list-item title="相册" showRight
		@click="open()">
			<text slot="icon" class="iconfont font-lg py-1">&#xe608;</text>
		</list-item>
		
		<divider></divider>
		
		<list-item title="设置" showRight  @click="open('my/setting/setting')">
			<text slot="icon" class="iconfont font-lg py-1">&#xe612;</text>
		</list-item>
		
	</view>
</template>

<script>
	import navBar from "@/components/ui/nav-bar.vue"
	import iconButton from "@/components/ui/icon-button.vue"
	import listItem from "@/components/ui/list-item.vue"
	import divider from "@/components/ui/divider.vue"
	import auth from '@/common/mixin/auth.js'
	import { mapState } from 'vuex'
	
	export default {
		components:{
			navBar,
			listItem,
			iconButton,
			divider
		},
		data() {
			return {
				user:{
					nickName:"lld"
				}
			}
		},
		methods: {
			open(path){
				uni.navigateTo({
					url: '/pages/'+path
				});
			},
		},
		computed:{
		}
	}
</script>

<style>

</style>
