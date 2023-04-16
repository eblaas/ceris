import Vue from "vue";
import VueClipboard from 'vue-clipboard2'

import App from "./App.vue";
import router from './routes/router'
import vuetify from './plugins/vuetify'
import {format, fromUnixTime, parseISO} from 'date-fns'

import store from './store'

Vue.config.productionTip = false;

Vue.filter('datetime', function (value) {
    if (!value) return ''
    value = value.toString()
    return format(parseISO(value), "yyyy-MM-dd kk:mm:ss")
})
Vue.filter('timestamp', function (value) {
    if (!value) return ''
    return format(fromUnixTime(value / 1000), "yyyy-MM-dd kk:mm:ss")
})

VueClipboard.config.autoSetContainer = true
Vue.use(VueClipboard)


new Vue({
    router,
    store,
    vuetify,
    render: (h) => h(App),
}).$mount("#app");
