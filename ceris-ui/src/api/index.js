// api/index.js

import axios from 'axios'
import store from '@/store'
import Vue from 'vue'

import {generateSchema} from "../utils"
import router from "../routes/router";

const client = axios.create({baseURL: window.host})

client.interceptors.request.use((config) => {
    if (store.getters.token) {
        config.headers["Authorization"] = `Bearer ${store.getters.token}`
    }
    return config
})

client.interceptors.response.use(function (response) {
    return response;
}, function (error) {
    if (error.response && error.response.status == 401) {
        router.push('/login')
    } else if (error.response && error.response.status > 300) {
        EventBus.$emit("api_error", {msg: error.response.data.message})
    } else {
        EventBus.$emit("api_error", {msg: error.message})
    }
    return Promise.reject(error);
});

// connect

export const EventBus = new Vue()

export function login(data) {
    return client.post("/auth/login", data)
}

export function me() {
    return client.get("/api/connect/me")
}

export function fetchConnectors() {
    return client.get(`/api/connect/connectors?expand=status`)
}

export function saveConnector(configuration) {
    return client.put(`/api/connect/connectors/${configuration.name}/config`, configuration)
}

export function deleteConnector(connector) {
    return client.delete(`/api/connect/connectors/${connector}`)
}

export function restartConnector(connector) {
    return client.post(`/api/connect/connectors/${connector}/restart?includeTasks=true`, {})
}

export function pauseConnector(connector) {
    return client.put(`/api/connect/connectors/${connector}/pause`)
}

export function resumeConnector(connector) {
    return client.put(`/api/connect/connectors/${connector}/resume`)
}

export function fetchConnectorTopics(connector) {
    return client.get(`/api/connect/connectors/${connector}/topics`)
}

export function fetchPlugins() {
    return client.get("/api/connect/connector-plugins")
}

export function installPlugin(pluginInstall) {
    return client.post("/api/connect/connector-plugins", pluginInstall)
}

export function uninstallPlugin(plugin) {
    return client.delete(`/api/connect/connector-plugins/${plugin.id}`)
}

export function fetchConnectorConfig(name) {
    return client.get(`/api/connect/connectors/${name}/config`)
}

export function fetchPluginsStore() {
    return client.get("/api/connect/plugins-store")
}

export function fetchStatus() {
    return client.get("/api/connect/status")
}

export function fetchMessages(topic) {
    return client.get(`/api/connect/topics/${topic}/messages`)
}

export function fetchTopics() {
    return client.get("/api/connect/topics")
}

export function fetchTopicSchema(topic) {
    return client.get(`/api/connect/topics/${topic}/schema`)
}

export function fetchMetrics(filter) {
    return client.get(`/api/connect/metrics`, {params: filter})
}

export function fetchSecrets() {
    return client.get(`/api/connect/secrets`)
}

export function fetchConnectorState(name) {
    return client.get(`/api/connect/connectors/${name}/status`)
}

export function validateConfiguration(configuration, schema = true) {
    const connectorClass = configuration["connector.class"]
    if (!connectorClass) {
        return Promise.reject({error: "connector missing class"})
    }
    const className = connectorClass.split('.').pop()
    const data = {...configuration}
    const hasTopic = ('topics' in configuration) || ('topics.regex' in configuration)
    if (!hasTopic) {
        data['topics'] = ".*"
    }
    return client.put(`/api/connect/connector-plugins/${className}/config/validate`, data)
        .then((response) => schema ? generateSchema(configuration, response.data, hasTopic) : response)
}
