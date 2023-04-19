import Vue from 'vue'
import App from './App'
import store from './store';

Vue.prototype.$store = store
Vue.config.productionTip = false

const sdk = require("@/common/imSdk/lim-sdk.js");
const lim = require("@/common/imSdk/sdk-bridge.js");

uni.lim = lim;
App.mpType = 'app'

Vue.prototype.imsdk = lim


const app = new Vue({
	store,
    ...App
})
app.$mount()
