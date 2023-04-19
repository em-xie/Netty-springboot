<template>

	<view>
		<!-- 导航栏 -->
		<nav-bar title="微信(10)">
		</nav-bar>

		<!-- 置顶列表 -->
		<block v-for="(item,index) in list" :key="index">
			<media-list v-if="item.istop" :item="item" :index="index" @long="long"></media-list>
		</block>

		<!-- 非置顶列表 -->
		<block v-for="(item,index) in list" :key="index">
			<media-list v-if="!item.istop" :item="item" :index="index" @long="long"></media-list>
		</block>

		<!-- 弹出层 -->
		<popup ref="extend" :bodyWidth="240" :bodyHeight="getMenusHeight">
			<view class="flex flex-column" style="width: 240rpx;" :style="getMenusStyle">
				<view class="flex-1 flex align-center" hover-class="bg-light" v-for="(item,index) in menus" :key="index"
					@click="clickEvent(item.event)">
					<text class="font-md pl-3">{{item.name}}</text>
				</view>
			</view>
		</popup>
	</view>

</template>

<script>
	import navBar from "@/components/ui/nav-bar.vue"
	import mediaList from "@/components/ui/media-list.vue"
	import popup from "@/components/ui/popup.vue"
	import auth from '@/common/mixin/auth.js'

	export default {
		mixins:[auth],
		components: {
			navBar,
			mediaList,
			popup
		},
		data() {
			return {
				propIndex: -1,
				list: [{
						avatar: "/static/images/userpic.png",
						nickname: "lld",
						id: "lld",
						updateTime: 1657554934224,
						data: "我是lld",
						istop: true,
						noreadnum: 1
					},
					{
						avatar: "/static/images/userpic.png",
						nickname: "lld2",
						id: "lld2",
						updateTime: 1654962934000,
						data: "我是lld2",
						istop: false,
						noreadnum: 0
					},
					{
						avatar: "../../../static/images/userpic.png",
						nickname: "lld3",
						id: "lld3",
						updateTime: 1626018934000,
						data: "我是lld3",
						istop: true,
						noreadnum: 1
					}
				],

				menus: [{
						name: "设为置顶",
						event: "setTop"
					},
					{
						name: "删除该聊天",
						event: "delChat"
					}
				]
			}
		},
		onLoad() {

		},
		onShow() {
			
		},
		computed:{
			// 动态获取菜单高度
			getMenusHeight(){
				let H = 100
				return this.menus.length * H
			},
			// 获取菜单的样式
			getMenusStyle(){
				return `height: ${this.getMenusHeight}rpx;`
			}
		},
		methods: {
			openExtend() {
				this.$refs.extend.show();
			},
			long({
				x,
				y,
				index
			}) {
				// 初始化 索引
				this.propIndex = index
				// 拿到当前对象
				let item = this.list[index]
				// 判断之前是否处于置顶状态
				this.menus[0].name = item.istop ? '取消置顶' : '设为置顶'
				this.$refs.extend.show(x, y)
			},
			// 分发菜单事件
			clickEvent(event){
				switch (event){
					case "setTop": // 置顶/取消置顶会话
					this.setTop()
						break;
					case "delChat": // 删除当前会话
					this.delChat()
						break;
				}
				this.$refs.extend.hide()
			},
			// 置顶/取消置顶会话
			setTop(){
				let item = this.list[this.propIndex]
				item.istop = !item.istop
			},
			// 删除当前会话
			delChat(){
				this.list.splice(this.propIndex,1);
			}
		},


	}
</script>

<style>


</style>
