<template>

	<view :class="item.istop ? 'bg-light' : 'bg-white'" hover-class="bg-hover-light">
		<div class="flex align-stretch" @click="onClick" @longpress="long">

			<view class="flex align-center justify-center position-relative" style="width: 145rpx;">
				<avater :src="item.avatar" size="92"></avater>
				<badge badgeClass="position-absolute" badgeStyle="top:15rpx;right:15rpx" v-if="item.noreadnum"
					:value="item.noreadnum"></badge>
			</view>

			<view class="flex flex-column border-bottom flex-1 py-3 pr-3 border-light-secondary">
				<view class="flex align-center justify-between mb-1">
					<text class="font-md">{{item.nickname}}</text>
					<text class="font-sm text-light-muted">{{item.updateTime|formatTime}}</text>
				</view>
				<text class="font-sm text-ellipsis text-light-muted">{{item.data}}</text>
			</view>

		</div>
	</view>
</template>

<script>
	import base from "@/common/mixin/base.js"
	import avater from "@/components/ui/avater.vue"
	import badge from "@/components/ui/badge.vue"

	export default {
			mixins:[base],
			components: {
				avater,
				badge
			},
			props: {
				item: Object,
				index:Number
			},
			methods:{
				onClick(){
					uni.navigateTo({
						url: '/pages/chat/chat/chat?params='+encodeURIComponent(JSON.stringify({
							id:this.item.id
						}))
					});
				},
				long(e){
					let x = 0
					let y = 0
					// #ifdef APP-PLUS-NVUE
					if (Array.isArray(e.changedTouches) && e.changedTouches.length > 0) {
						x = e.changedTouches[0].screenX
						y = e.changedTouches[0].screenY
					}
					// #endif
					// #ifdef H5
					x = e.changedTouches[0].pageX
					y = e.changedTouches[0].pageY
					// #endif
					// #ifdef MP
					x = e.detail.x
					y = e.detail.y
					// #endif
					console.log(this.index)
					this.$emit('long',{
						x,
						y,
						index:this.index
					})
				}
			}
		}
</script>

<style>
</style>
