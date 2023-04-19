<template>
	<view>

		<!-- 导航栏 -->
		<nav-bar :title="detail.name" :noreadnum="999" showBack>
			<icon-button slot="right" :icon="'\ue6fd'" ></icon-button>
		</nav-bar>

		<!-- 聊天内容区域 -->
		<scroll-view scroll-y class="bg-light position-fixed left-0 right-0 px-3"
			style="bottom: 105rpx;box-sizing: border-box;" :style="chatBodyBottom" :show-scrollbar="false"
			:scroll-into-view="scrollIntoView" :scroll-with-animation="true" @click="clickPage">

			<!-- 聊天信息列表组件 -->
			<view v-for="(item,index) in list" :key="index" :id="'chatItem_'+index">
				<chat-item :item="item" :index="index" ref="chatItem"
					:pretime=" index > 0 ? list[index-1].createTime : 0" @long="long" 
					:shownickname="currentChatItem.shownickname"></chat-item>
			</view>

		</scroll-view>

		<!-- #ifdef APP-PLUS-NVUE -->
		<div v-if="mode === 'action' || mode === 'emoticon'" class="position-fixed top-0 right-0 left-0"
			:style="'bottom:'+maskBottom+'px;'" @click="clickPage"></div>
		<!-- #endif -->

		<!-- 底部输入框 -->
		<view class="position-fixed left-0 right-0 border-top flex align-center"
			style="background-color: #F7F7F6;height: 105rpx;" :style="'bottom:'+KeyboardHeight+'px;'">
			<icon-button :icon="'\ue606'" @click="changeVoiceOrText"></icon-button>
			<view class="flex-1">
				<textarea fixed class="bg-white rounded p-2 font-md" style="height: 50rpx;max-width: 450rpx;"
					:adjust-position="false" v-model="text" @focus="mode = 'text'" />
			</view>
			<!-- 表情 -->
			<!-- <icon-button :icon="'\ue605'"></icon-button> -->
			<template v-if="text.length === 0">
				<!-- 扩展菜单 -->
				<icon-button :icon="'\ue603'" @click="openActionOrEmoticon('action')"></icon-button>
			</template>
			<view v-else class="flex-shrink">
				<!-- 发送按钮 -->
				<main-button name="发送" @click="send('text')"></main-button>
			</view>

		</view>

		<!-- 扩展菜单 -->
		<popup ref="action" bottom transformOrigin="center bottom" @hide="KeyboardHeight = 0" :mask="false">
			<view style="height: 580rpx;" class="border-top border-light-secondary bg-light">
				<swiper :indicator-dots="actionLists.length > 1" style="height: 510rpx;">
					<swiper-item class="row pl-5" v-for="(item,index) in actionLists" :key="index">
						<view class="col-3 flex flex-column align-center justify-center" style="height: 255rpx;"
							v-for="(item2,index2) in item" :key="index2" @click="actionEvent(item2)">
							<image :src="item2.icon" mode="widthFix" style="width: 100rpx;height: 100rpx;"></image>
							<text class="font-sm text-muted mt-2">{{item2.name}}</text>
						</view>
					</swiper-item>
				</swiper>
			</view>
		</popup>


		<!-- 弹出层 -->
		<popup ref="extend" :bodyWidth="240" :bodyHeight="450" :tabbarHeight="105">
			<view class="flex flex-column" style="width: 240rpx;" :style="getMenusStyle">
				<view class="flex-1 flex align-center" hover-class="bg-light" v-for="(item,index) in menusList"
					:key="index" @click="clickEvent(item.event)">
					<text class="font-md pl-3">{{item.name}}</text>
				</view>
			</view>
		</popup>

	</view>
</template>

<script>
	// #ifdef APP-PLUS-NVUE
	const dom = weex.requireModule('dom')
	// #endif

	const sdk = uni.lim.im;

	import navBar from "@/components/ui/nav-bar.vue"
	import iconButton from "@/components/ui/icon-button.vue"
	import chatItem from '@/components/ui/chat-item.vue';
	import popup from "@/components/ui/popup.vue"
	import mainButton from '@/components/ui/main-button.vue';


	import {
		mapState,
		mapMutations
	} from 'vuex'

	import auth from '@/common/mixin/auth.js';
	import $H from '@/common/lib/request.js';
	import $C from '@/common/lib/config.js';
	
	export default {
		mixins: [auth],
		components: {
			navBar,
			iconButton,
			chatItem,
			popup,
			mainButton
		},
		data() {
			return {
				scrollIntoView: "",
				toId: "",
				// 模式 text输入文字，emoticon表情，action操作，audio音频
				mode: "text",
				// 扩展菜单列表
				actionList: [
					[{
						name: "相册",
						icon: "/static/images/extends/pic.png",
						event: "uploadImage"
					}, {
						name: "拍摄",
						icon: "/static/images/extends/video.png",
						event: "uploadVideo"
					}, {
						name: "收藏",
						icon: "/static/images/extends/shoucan.png",
						event: "openFava"
					}, {
						name: "名片",
						icon: "/static/images/extends/man.png",
						event: "sendCard"
					}, {
						name: "语音通话",
						icon: "/static/images/extends/phone.png",
						event: ""
					}, {
						name: "位置",
						icon: "/static/images/extends/path.png",
						event: ""
					}]
				],
				emoticonList: [],
				// 键盘高度
				KeyboardHeight: 0,
				menusList: [],
				navBarHeight: 0,
				list: [{
					userId: 'lld',
					avatar: "/static/images/userpic.png",
					data: "你好啊你好啊你好啊你好啊你好啊你好啊你好啊",
					nickname: "lld",
					type: "text",
					createTime: 1657465771,
					isRemove: false
				}, {
					userId: 'lld2',
					avatar: "/static/images/userpic.png",
					data: "1234567890",
					nickname: "lld2",
					type: "text",
					createTime: 1658329771,
					isRemove: false
				}, {
					userId: 'lld',
					avatar: "/static/images/userpic.png",
					data: "你好啊你好啊你好啊你好啊你好啊你好啊你好啊",
					nickname: "lld",
					type: "text",
					createTime: 1657465771,
					isRemove: false
				}, {
					userId: 'lld2',
					avatar: "/static/images/userpic.png",
					data: "1234567890",
					nickname: "lld2",
					type: "text",
					createTime: 1658329771,
					isRemove: false
				}],
				// 当前操作的气泡索引
				propIndex: -1,
				// 输入文字
				text: "",

				detail: {
					id: 0,
					name: "",
					avatar: "",
					chat_type: "user"
				}
			}
		},
		mounted() {
			var statusBarHeight = 0
			// #ifdef APP-PLUS-NVUE
			statusBarHeight = plus.navigator.getStatusbarHeight()
			// #endif
			this.navBarHeight = statusBarHeight + uni.upx2px(90)

			// 监听键盘高度变化
			uni.onKeyboardHeightChange(res => {
				if (this.mode !== 'action' && this.mode !== 'emoticon') {
					this.KeyboardHeight = res.height
				}
				if (this.KeyboardHeight > 0) {
					this.pageToBottom()
				}
			})

			this.pageToBottom()
		},
		computed: {
			...mapState({
			}),
			// 当前会话配置信息
			currentChatItem() {
				return {}
			},
			// 获取蒙版的位置
			maskBottom() {
				return this.KeyboardHeight + uni.upx2px(105)
			},
			// 动态获取菜单高度
			getMenusHeight() {
				let H = 100
				return this.menusList.length * H
			},
			// 获取菜单的样式
			getMenusStyle() {
				return `height: ${this.getMenusHeight}rpx;`
			},
			// 判断是否操作本人信息
			isdoSelf() {
				// 获取本人id（假设拿到了）
				let id = 1
				let user_id = this.propIndex > -1 ? this.list[this.propIndex].user_id : 0
				return user_id === id
			},
			// 聊天区域bottom
			chatBodyBottom() {
				return `bottom:${uni.upx2px(105) + this.KeyboardHeight}px;top:${this.navBarHeight}px;`
			},
			// 获取操作或者表情列表
			actionLists() {
				return (this.mode === 'emoticon' || this.mode === 'action') ? this[this.mode + 'List'] : []
			},
			// 所有信息的图片地址
			imageList() {
				let arr = []
				this.list.forEach((item) => {
					if (item.type === 'emoticon' || item.type === 'image') {
						arr.push(item.data)
					}
				})
				return arr
			}
		},
		watch: {
			mode(newValue, oldValue) {
				if (newValue !== 'action' && newValue !== 'emoticon') {
					this.$refs.action.hide()
				}
				if (newValue !== 'text') {
					uni.hideKeyboard()
				}
			}
		},
		onLoad(e) {
			var toIdObj = JSON.parse(decodeURIComponent(e.params))
			this.toId = toIdObj.id;
			// // 监听接收聊天信息
			uni.$on('P2PMessage', this.onMessage)
		},
		destroyed() {
			// // 销毁监听接收聊天消息
			uni.$off('P2PMessage',this.onMessage)
		},
		methods: {
			onMessage(message) {
				if(message.fromId != this.toId){
					return;
				}
				console.log('[聊天页] 监听接收聊天信息', message);
				var messageBody = JSON.parse(message.messageBody);
				if(messageBody.type == 1){
					let msg = {
						userId:message.fromId,
						avatar: "/static/images/userpic.png",
						data: messageBody.content == '' ? '空消息' : messageBody.content,
						nickname:message.fromId,
						type: "text",
						createTime: message.messageTime / 1000,
						isRemove: false
					}
					this.list.push(msg)
				}
				this.pageToBottom()
			},
			__init() {},
			// 打开扩展菜单或者表情包
			openActionOrEmoticon(mode = 'action') {
				this.mode = mode
				this.$refs.action.show()

				uni.hideKeyboard()
				this.KeyboardHeight = uni.upx2px(580)
			},
			// 发送
			send(type, data = '', options = {}) {
				// 组织数据格式
				if (type == 'text') {
					sdk.sendP2PMessage(sdk.createP2PTextMessage(this.toId, this.text))
					let uid = sdk.getUserId();
					let msg = {
						userId:uid,
						avatar: "/static/images/userpic.png",
						data: this.text,
						nickname:uid,
						type: "text",
						createTime: Date.parse(new Date()) / 1000,
						isRemove: false
					}
					this.list.push(msg)
				}
				// 发送文字成功，清空输入框
				if (type === 'text') {
					this.text = ''
				}
				// 置于底部
				this.pageToBottom()
			},
			// 回到底部
			pageToBottom() {
				// #ifdef APP-PLUS-NVUE
				let chatItem = this.$refs.chatItem
				let lastIndex = chatItem.length > 0 ? chatItem.length - 1 : 0
				if (chatItem[lastIndex]) {
					dom.scrollToElement(chatItem[lastIndex], {})
				}
				// #endif
				// #ifndef APP-NVUE
				setTimeout(() => {
					let lastIndex = this.list.length - 1
					this.scrollIntoView = 'chatItem_' + lastIndex
				}, 300)
				// #endif
			},
			// 长按消息气泡
			long({
				x,
				y,
				index
			}) {
				// 初始化 索引
				this.propIndex = index
				// 组装菜单
				let menus = [{
					name: "发送给朋友",
					event: 'sendToChatItem'
				}, {
					name: "收藏",
					event: 'fava'
				}, {
					name: "删除",
					event: 'delete'
				}]
				let item = this.list[this.propIndex]
				menus.push({
					name: "撤回",
					event: 'removeChatItem'
				})
				// #ifndef H5
				if (item.type === 'text') {
					menus.unshift({
						name: "复制",
						event: 'copy',
					})
				}
				// #endif
				this.menusList = menus
				// 显示扩展菜单
				this.$refs.extend.show(x, y)
			},
			// 操作菜单方法分发
			clickEvent(event) {
				// 关闭菜单
				this.$refs.extend.hide()
			},
			// 扩展菜单
			actionEvent(e) {
				switch (e.event) {
					case 'uploadImage': // 选择相册
						uni.chooseImage({
							count: 9,
							success: (res) => {
								// 发送到服务器
								// 渲染到页面
								res.tempFilePaths.forEach((item) => {
									this.send('image', item)
								})
							}
						})
						break;
					case 'uploadVideo': // 发送短视频
						uni.chooseVideo({
							maxDuration: 10,
							success: (res) => {
								this.send('video', res.tempFilePath)
								// 渲染页面
								// 发送到服务端（获取视频封面，返回url）
								// 修改本地的发送状态
							}
						})
						break;
				}
			},
			// 点击页面
			clickPage() {
				this.mode = ''
			},
			// 切换音频录制和文本输入
			changeVoiceOrText() {
				this.mode = this.mode !== 'audio' ? 'audio' : 'text'
			}
		}
	}
</script>

<style>

</style>
