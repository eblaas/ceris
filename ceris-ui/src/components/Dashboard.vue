<template>
  <v-app>
    <v-navigation-drawer
        width="200"
        app
        permanent
        color="#272727"
    >
      <v-list dense>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="title">
              <span class="font-weight-bold">Ceris</span>
            </v-list-item-title>
            <v-list-item-subtitle>Integration</v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-divider></v-divider>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-connection</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <router-link to="/connectors">Connectors</router-link>
              <span class="grey--text"> ({{ $store.state.connectors.length }})</span>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-toy-brick-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <router-link to="/plugins" exact>Plugins</router-link>
              <span class="grey--text"> ({{ connectorPlugins.length }})</span>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-toy-brick-search-outline</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <router-link to="/pluginstore" exact>Plugins Store</router-link>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-table</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <router-link to="/datasets" exact>Datasets</router-link>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-gauge</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <router-link to="/metrics" exact>Metrics</router-link>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-action>
            <v-icon>mdi-key-variant</v-icon>
          </v-list-item-action>
          <v-list-item-content>
            <v-list-item-title>
              <secrets/>
            </v-list-item-title>
          </v-list-item-content>
        </v-list-item>
      </v-list>
      <template v-slot:append>
        <v-divider></v-divider>
        <div class="text-center pa-2">
          <v-speed-dial direction="top" open-on-hover>
            <template v-slot:activator>
              <v-btn color="blue darken-2" dark fab x-small>
                <v-icon>mdi-account-circle</v-icon>
              </v-btn>
            </template>
            <v-btn dark small color="primary darken-2" @click="logout()">
              Logout
            </v-btn>
          </v-speed-dial>
        </div>
      </template>
    </v-navigation-drawer>
    <v-main>
      <v-app-bar app fixed short class="darken-4">
        <v-breadcrumbs :items="crumbs" divider=">">
          <template v-slot:item="{item}">
            <router-link :to="item.to">
              <v-breadcrumbs-item>
                {{ item.text }}
              </v-breadcrumbs-item>
            </router-link>
          </template>
        </v-breadcrumbs>
        <v-spacer/>
        <v-chip v-for="item in componentStatus" :key="item.component"
                class="ma-2"
                :color="item.color"
                text-color="white"
                outlined
        >
          <v-avatar left>
            <v-icon :color="item.color">{{ item.icon }}</v-icon>
          </v-avatar>
          {{ item.desc }}
        </v-chip>
      </v-app-bar>
      <v-overlay v-model="loading.overlay" z-index="100">
        <v-progress-circular
            color="primary"
            indeterminate
            size="64"
        ></v-progress-circular>
      </v-overlay>
      <router-view/>
      <v-snackbar
          v-model="showError"
          multi-line
          timeout="8000"
      >
        {{ errorMessage }}
        <template v-slot:action="{ attrs }">
          <v-btn
              color="red"
              text
              v-bind="attrs"
              @click="showError = false"
          >
            Close
          </v-btn>
        </template>
      </v-snackbar>
    </v-main>
  </v-app>
</template>

<script>
import {EventBus} from "@/api";
import Secrets from "./Secrets";
import {mapActions, mapGetters} from "vuex";

export default {
  name: "App",
  components: {
    Secrets
  },
  data: () => ({
    drawer: true,
    selectedItem: null,
    showError: false,
    errorMessage: "",
    intervalId: null
  }),
  methods: {
    ...mapActions(['loadConnectors', 'loadPlugins', 'loadSecrets', 'loadStatus', 'logout'])
  },

  computed: {
    ...mapGetters(['connectorPlugins', 'loading', 'status']),
    componentStatus: function () {
      return this.status.map((status) => {
        status.color = status.up ? "green" : "red"
        status.icon = status.up ? "mdi-checkbox-marked-circle" : "mdi-alert-outline"
        return status
      })
    },
    crumbs: function () {
      let pathArray = this.$route.path.split("/")
      pathArray.shift()
      let breadcrumbs = pathArray.reduce((breadcrumbArray, path, idx) => {
        breadcrumbArray.push({
          to: breadcrumbArray[idx - 1]
              ? breadcrumbArray[idx - 1].to + "/" + path
              : "/" + path,
          text: decodeURI(path),
          disabled: false,
        });
        return breadcrumbArray;
      }, [])
      return breadcrumbs;
    }
  },
  created() {
    this.loadConnectors()
    this.loadPlugins()
    this.loadStatus()
    this.loadSecrets()
    this.intervalId = setInterval(() => {
      this.loadConnectors()
      this.loadStatus()
    }, 100000)

    EventBus.$on('api_error', (error) => {
      this.showError = true
      this.errorMessage = error.msg.substr(0, 200)
    })
  },
  beforeDestroy() {
    clearInterval(this.intervalId)
    EventBus.$off('api_error')
  }
}
</script>

<style>
#app .router-link-active,
#app .router-link-exact-active {
  color: #2196f3;
}

#app a {
  color: #827f7f;
}
html {
  /*overflow: hidden !important;*/
  overflow-y: auto !important;
}

</style>