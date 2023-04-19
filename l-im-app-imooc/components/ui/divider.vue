<template>
	<view :style="getStyle"></view>
	
</template>

	
<script>
	export default {
		props: {
			color:{
				type:String,
				default:"#EFEDE9"
			}
		},
		computed: {
			getStyle(){
				return `height: 18rpx;background-color: ${this.color}rpx`
			}
		},
		methods: {
		},
	}
</script>

<style>
</style>
