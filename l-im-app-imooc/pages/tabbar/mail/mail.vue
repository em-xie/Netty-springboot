<template>
	<view>
		<!-- 导航栏 -->
		<nav-bar title="通讯录" :showBack="false" :showRight="false">

		</nav-bar>

		<scroll-view scroll-y="true" :style="'height:'+scrollHeight+'px;'" :scroll-into-view="scrollInto">
			<!-- 通讯录列表 :showRight="item.id === 'friend' && applyCount > 0" -->
			<list-item v-for="(item,index) in topList" :title="item.title" :key="item.id" :cover="item.cover">
			</list-item>
		</scroll-view>


		<!-- 侧边导航条 -->
		<view class="position-fixed right-0 bottom-0 bg-light flex flex-column" :style="'top:' +top+ 'px'"
			style="width: 50rpx;">
		</view>

		<!-- 侧边导航条 -->
		<!-- <view class="position-fixed right-0 bottom-0 bg-light flex flex-column" :style="'top:'+top+'px;'"
			style="width: 50rpx;" @touchstart="touchstart" @touchmove="touchmove" @touchend="touchend">
			<view class="flex-1 flex align-center justify-center" v-for="(item,index) in list.keys()" :key="index">
				<text class="font-sm text-muted">{{item}}</text>
			</view>
		</view> -->
		
		<!-- <view class="position-fixed rounded-circle bg-light border flex align-center justify-center" v-if="current"
		style="width: 150rpx;height: 150rpx;left: 300rpx;"
		:style="'top:'+modalTop+'px;'">
			<text class="font-lg">{{current}}</text>
		</view> -->


	</view>
</template>

<script>
	import navBar from "@/components/ui/nav-bar.vue"
	import listItem from "@/components/ui/list-item.vue"
	import auth from '@/common/mixin/auth.js'
	import badge from "@/components/ui/badge.vue"

	export default {
		mixins:[auth],
		components: {
			navBar,
			listItem,
			badge
		},
		data() {
			return {
				top: 0,
				friendRequestNum: 0,
				topList: [{
						id: "friend",
						title: "新的朋友",
						cover: "/static/images/mail/friend.png",
						path: "mail/apply-list/apply-list"
					},
					{
						id: "group",
						title: "群聊",
						cover: "/static/images/mail/group.png",
						path: "mail/group-list/group-list"
					},
					{
						id: "tag",
						title: "标签",
						cover: "/static/images/mail/tag.png",
						path: "mail/tag-list/tag-list"
					}
				],
				// list: new Map(),
				list: 
				[{
						"letter": "A",
						"data": [
							"AAAAA微商",
							"a ",
							"澳门风云"
						]
					},
					{
						"letter": "B",
						"data": [
							"宝贝",
						]
					},
					{
						"letter": "C",
						"data": [
							"草原",
						]
					},
					{
						"letter": "D",
						"data": []
					},
					{
						"letter": "E",
						"data": []
					},
					{
						"letter": "F",
						"data": []
					},
					{
						"letter": "G",
						"data": []
					},
					{
						"letter": "H",
						"data": [
							"黄飞鸿",
							"黄继光"
						]
					},
					{
						"letter": "I",
						"data": [
							"ICON",
							"icc",
							"icc",
							"icc",
							"icc"
						]
					}
				]
				,

				top: 0,
				scrollHeight: 0,
				scrollInto: '',
				current: ''
			}
		},
		methods: {
		},
		onLoad() {
			let res = uni.getSystemInfoSync()
			this.top = res.statusBarHeight + uni.upx2px(90)
			this.scrollHeight = res.windowHeight - this.top
		},
		onShow() {
		},
		computed: {
			// 每个索引的高度
			itemHeight() {
				let count = this.list.size
				// debugger;
				if (count < 1) {
					return 0
				}
				return this.scrollHeight / count
			},
			modalTop(){
				return (this.scrollHeight - uni.upx2px(150)) / 2
			}
		}
	}
</script>

<style>

</style>
