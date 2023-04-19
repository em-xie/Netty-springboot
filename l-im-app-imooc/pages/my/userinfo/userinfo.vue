<template>
	<view class="page">
		<!-- 导航栏 -->
		<nav-bar title="个人资料" showBack :showRight="false"></nav-bar>
		<list-item title="头像" showRight :showLeftIcon="false">
			<avater slot="right" :src="user.avatar"></avater>
		</list-item>
		<list-item title="昵称" showRight :showLeftIcon="false">
			<text slot="right" class="font text-muted">{{user.nickname}}</text>
		</list-item>
		<list-item title="账号" showRight :showLeftIcon="false">
			<text slot="right" class="font text-muted">{{user.username}}</text>
		</list-item>

		<confirm ref="confirm" :title="confirmTitle">
			<input type="text" v-model="confirmText" :placeholder="placeholder" class="border-bottom font-md"/>
		</confirm>
		
	</view>
</template>

<script>
	import navBar from "@/components/ui/nav-bar.vue"
	import avater from '@/components/ui/avater.vue';
	import listItem from "@/components/ui/list-item.vue"
	import confirm from '@/components/ui/confirm.vue';
	
	import $H from '@/common/lib/request.js';
	export default {
		components: {
			navBar,
			avater,
			listItem,
			confirm
		},
		data() {
			return {
				confirmText:"",
				confirmType:"",
				user:{
					avatar:"",
					nickname:"lld",
					username:"lld"
				}
			}
		},
		computed: {
			confirmTitle() {
				return this.confirmType == 'username' ? '修改账号' :'修改昵称' 
			},
			placeholder(){
				return this.confirmType == 'username' ? '输入账号' :'输入昵称' 
			}
		},
		methods: {
			
		}
	}
</script>

<style>

</style>
