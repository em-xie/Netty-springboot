<template>
	<text class="badge bg-danger text-white rounded-circle font-sm" :class="badgeClass" :style="badgeStyle">{{value}}</text>
</template>

<script>
	export default {
		props: {
			badgeClass: {
				type: String,
				default: ""
			},
			badgeStyle:{
				type:String,
				default:""
			},
			value:{
				type:[Number,String],
				default:""
			}
		},
	}
</script>

<style scoped>
	.badge{
		padding-left: 14rpx;padding-right: 14rpx;padding-bottom: 3rpx;padding-top: 3rpx;
	}
</style>
