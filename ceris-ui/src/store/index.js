import Vue from 'vue'
import Vuex from 'vuex'

import {fetchConnectors, fetchPlugins, fetchSecrets,} from '@/api'
import {fetchStatus, login, me} from "../api";
import router from "../routes/router";

Vue.use(Vuex)

const store = new Vuex.Store({
    state: {
        connectors: [],
        plugins: [],
        secrets: [],
        status: [],
        token: null,
        user: null,
        loading: {
            connectors: false,
            plugins: false,
            overlay: false,
        }
    },
    actions: {
        loadConnectors({commit}) {
            commit('setLoading', {key: 'connectors', loading: true})
            return fetchConnectors()
                .then((response) => commit('updateConnectors', response.data))
                .finally(() => commit('setLoading', {key: 'connectors', loading: false}))
        },
        loadPlugins({commit}) {
            commit('setLoading', {key: 'plugins', loading: true})
            return fetchPlugins()
                .then((response) => commit('updatePlugins', response.data))
                .finally(() => commit('setLoading', {key: 'plugins', loading: false}))
        },
        loadSecrets({commit}) {
            return fetchSecrets()
                .then((response) => commit('updateSecrets', response.data))
        },
        loadStatus({commit}) {
            return fetchStatus()
                .then((response) => commit('updateStatus', response.data))
        },
        login({commit}, data) {
            return login(data)
                .then((response) => commit('setToken', response.data.token))

        },
        logout({commit}) {
            commit('setToken', null)
            commit('setUser', null)
            router.push('/login')
        },
        getUser({commit}) {
            return me()
                .then((response) => commit('setUser', response.data))
        },
        overlay({commit}, enabled) {
            commit("setLoading", {key: 'overlay', loading: enabled})
        }
    },
    mutations: {
        updatePlugins(state, data) {
            state.plugins = data.map((plugin) => {
                plugin.name = plugin.manifest?.title || plugin.class.split(".").pop()
                plugin.package = plugin.class.split(".").slice(0, -1).join('.')
                plugin.schema = null
                return plugin
            })
        },
        updateConnectors(state, data) {
            state.connectors = Object.keys(data).map((name) => ({
                type: data[name].status.type,
                name: name,
                state: data[name].status.connector.state,
                hasError: data[name].status.connector.state == "FAILED"
                    || data[name].status.tasks.some(t => t.state == "FAILED"),
                tasksCount: data[name].status.tasks.length,
                tasksState: [...new Set(
                    data[name].status.tasks.map(t => t.state))].join(',')
            }))
        },
        updateSecrets(state, data) {
            state.secrets = data
        },
        updateStatus(state, data) {
            state.status = data
        },
        setLoading(state, {key, loading}) {
            Vue.set(state.loading, key, loading)
        },
        setToken(state, token) {
            Vue.set(state, "token", token)
            localStorage.setItem('token', token)
        },
        setUser(state, user) {
            Vue.set(state, "user", user)
        }
    },
    getters: {
        connectors: state => state.connectors,
        plugins: state => state.plugins,
        status: state => state.status,
        connectorPlugins: state => state.plugins.filter(
            (p) => p.type === 'source' || p.type === 'sink'),
        secrets: state => state.secrets,
        token: state => state.token || localStorage.getItem('token'),
        isAdmin: state => state.user && state.user.role == 'ADMIN',
        loading: state => state.loading
    }
})

export default store
