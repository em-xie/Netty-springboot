<template>
	<div class="" @longpress="long">
		<view v-if="showTime" class="flex align-center justify-center pb-4 pt-2">
			<text class="font-sm text-light-muted">{{showTime}}</text>
		</view>

		<view v-if="item.isRemove" ref="isRemove" class="flex align-center justify-center pb-4 pt-1 chat-animate">
			<text class="font-sm text-light-muted">你撤回了一条消息</text>
		</view>

		<!-- 气泡 -->
		<view v-else class="flex align-start  position-relative mb-3"
			:class="!isself ? 'justify-start' : 'justify-end'">
			<!-- 好友 -->
			<template v-if="!isself">
				<avater size="70" :src="item.avatar" clickType="navigate"></avater>
				<text v-if="hasLabelClass" class="iconfont text-white font-md position-absolute chat-left-icon"
					style="">&#xe609;</text>
			</template>

			<div style="max-width: 500rpx;" class="p-2 rounded " :class="labelClass" :style="labelStyle">
				<!-- 文字 -->
				<text v-if="item.type === 'text'" class="font-md ">{{item.data}}</text>
			</div>

			<!-- 本人 -->
			<template v-if="isself">
				<text v-if="hasLabelClass" class="iconfont text-chat-item font-md position-absolute chat-right-icon"
					style="">&#xe640;</text>
				<avater size="70" :src="item.avatar" clickType="navigate"></avater>
			</template>
		</view>
	</div>


</template>

<script>
	import avater from "@/components/ui/avater.vue"
	import fImage from "@/components/ui/fImage.vue"
	import $T from "@/common/lib/time.js"
	import {
		mapState,
		mapActions
	} from 'vuex'

	// #ifdef APP-PLUS-NVUE
	const animation = weex.requireModule('animation')
	// #endif

	export default {
		components: {
			avater,
			fImage
		},
		props: {
			item: Object,
			index: Number,
			pretime: [Number, String]
		},
		computed: {
			// 是否为本人
			isself() {
				var lim = this.imsdk
				var sdk = lim.im;
				let uid = sdk.getUserId();
				// console.log(uid)
				return this.item.userId === uid;
			},
			// 显示的时间
			showTime() {
				return $T.getChatTime(this.item.createTime, this.pretime)
			},
			// 是否需要气泡样式
			hasLabelClass() {
				return this.item.type === 'text'
			},
			// 气泡的样式
			labelClass() {
				let label = this.hasLabelClass ? 'bg-chat-item mr-3' : 'mr-3'
				return this.isself ? label : 'bg-white ml-3'
			},
			labelStyle(){
			}
		},
		methods: {
			long(e) {
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
				this.$emit('long', {
					x,
					y,
					index: this.index
				})
			},
			preview(url) {
				this.$emit("preview", url)
			},
			loadImage(e) {
				let w = e.detail.width
				let h = e.detail.height
				// 按照宽度来
				let maxW = uni.upx2px(500)

				//按照高度来
				let maxH = uni.upx2px(350)
				if (h <= maxH) {
					this.w = w <= maxW ? w : maxW;
					this.h = h;
					return;
				}
				this.h = maxH
				let w2 = maxH * (w / h)
				this.w = w2 <= maxW ? w2 : maxW
			}
		},
		destroyed() {
		},
		data() {
			return {
				w: 100,
				h: 100
			}
		},
		mounted() {
			// 注册全局事件
		}
	}
</script>

<style scoped>
	.chat-left-icon {
		left: 80rpx;
	}

	.chat-right-icon {
		right: 80rpx;
	}

	.chat-animate {
		/* #ifndef APP-PLUS-NVUE */
		opacity: 0;
		/* #endif */
	}
</style>
