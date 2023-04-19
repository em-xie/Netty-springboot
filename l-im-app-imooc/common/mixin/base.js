import $Time from "@/common/lib/time.js"
export default {
	filters: {
		formatTime(value) {
			return $Time.gettime(value);
		}
	},
}