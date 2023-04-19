<template>
	<view class="page">
		<!-- 导航栏 -->
		<nav-bar title="发现"></nav-bar>
		
		<!-- 通讯录列表 -->
		<list-item title="朋友圈" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe667;</text>
			<view slot="right" class="p-1">
				<view class="position-relative" v-if="!notice.num">
				<text class="rounded-circle bg-danger position-absolute" style="width: 20rpx;height: 20rpx;top: 0;right: 0;"></text>
				</view>
				<badge v-if="notice.num" :value="notice.num"></badge>
			</view>
		</list-item>
		<divider></divider>
		<list-item title="扫一扫" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe86d;</text>
		</list-item>
		<list-item title="摇一摇" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe63d;</text>
		</list-item>
		<divider></divider>
		<list-item title="看一看" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe610;</text>
		</list-item>
		<list-item title="搜一搜" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe611;</text>
		</list-item>
		<divider></divider>
		<list-item title="购物" showRight>
			<text slot="icon" class="iconfont font-lg py-1">&#xe658;</text>
		</list-item>
		
	</view>
</template>

<script>
	
	import navBar from "@/components/ui/nav-bar.vue"
	import listItem from "@/components/ui/list-item.vue"
	import divider from "@/components/ui/divider.vue"
	import badge from "@/components/ui/badge.vue"
	import auth from '@/common/mixin/auth.js'
	
	export default {
		mixins:[auth],
		components: {
			navBar,
			listItem,
			divider,
			badge
		},
		data() {
			return {
				notice:{
					num: 1,
					userId:"lld",
					avatar:"/static/images/userpic.png"
				}
			}
		},
		methods: {
			
		}
	}
</script>

<style>

</style>
