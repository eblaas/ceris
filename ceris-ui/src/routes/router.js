import Vue from 'vue'
import Router from 'vue-router'
import Connectors from "../components/connectors/Connectors"
import ConnectorsAdd from "../components/connectors/ConnectorsAdd"
import ConnectorsEdit from "../components/connectors/ConnectorsEdit"
import ConnectorsDetails from "../components/connectors/ConnectorsDetails"
import Dashboard from "../components/Dashboard"
import Plugins from "../components/plugins/Plugins"
import Datasets from "../components/datasets/Datasets"
import DatasetMessages from "../components/datasets/DatasetMessages"
import Metrics from "../components/metrics/Metrics"
import PluginsStore from "../components/plugins/PluginsStore"
import store from '@/store'
import DatasetSchema from "../components/datasets/DatasetSchema";
import Login from "../components/users/Login";

Vue.use(Router)

export default new Router({
    routes: [
        {
            path: '/',
            component: Dashboard,
            children: [
                {
                    path: '/',
                    redirect: '/connectors'
                }, {
                    path: '/connectors',
                    component: Connectors
                }, {
                    path: '/connectors/new',
                    component: ConnectorsAdd
                }, {
                    path: '/connectors/:connector',
                    component: ConnectorsDetails
                }, {
                    path: '/connectors/:connector/edit',
                    component: ConnectorsEdit
                }, {
                    path: '/plugins',
                    component: Plugins
                },
                {
                    path: '/datasets',
                    component: Datasets
                },
                {
                    path: '/datasets/:dataset/messages',
                    component: DatasetMessages
                },
                {
                    path: '/datasets/:dataset',
                    component: DatasetSchema
                },
                {
                    path: '/metrics',
                    component: Metrics
                },
                {
                    path: '/pluginstore',
                    component: PluginsStore
                }
            ],
            beforeEnter(to, from, next) {
                store.dispatch('getUser')
                    .then(() => next())
                    .catch(() => next('/login'))
            }
        },
        {
            path: '/login',
            name: 'login',
            component: Login
        },
    ]
})