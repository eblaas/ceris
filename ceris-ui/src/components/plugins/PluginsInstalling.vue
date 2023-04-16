<template>
  <div class="text-center">
    <v-dialog
        v-model="dialog"
        width="600"
        persistent
    >
      <template v-slot:activator="{ on, attrs }">
        <v-btn
            title="Install Plugin"
            icon
            color="primary"
            v-bind="attrs"
            v-on="on"
        >
          <v-icon>mdi-cloud-download-outline</v-icon>
        </v-btn>
      </template>

      <v-card>
        <v-card-title class="text-h5 lighten-2">
          Install Plugin {{ plugin.name }}
        </v-card-title>
        <v-card-text>
          <v-alert border="left" outlined type="warning">
            Plugin License:
            <a target="_blank" :href="plugin.license.url">{{ plugin.license.name }}</a>
          </v-alert>
          <v-progress-linear
              v-if="installing"
              color="deep-purple accent-4"
              indeterminate
              rounded
              height="6"
          ></v-progress-linear>
          <v-select
              v-if="showJarSelector"
              v-model="databaseSelect"
              :items="databases"
              label="Select database"
              hint="JDBC drivers to install"
              persistent-hint
              multiple
          ></v-select>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text :disabled='installing' @click="dialog = false">
            Cancel
          </v-btn>
          <v-btn :disabled='installing'
                 color="primary"
                 text
                 @click="install"
          >
            Accept License and Install
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
<script>

import {installPlugin} from "../../api";

const DATABASES = [
  {text: "Postgres", value: "org.postgresql:postgresql:42.5.1"},
  {text: "MySql", value: "com.mysql:mysql-connector-j:8.0.32"},
  {text: "MSSql", value: "com.microsoft.sqlserver:mssql-jdbc:11.2.3.jre17"},
  {text: "Oracle", value: "com.oracle.database.jdbc:ojdbc11:21.8.0.0"},
  {text: "DB2", value: "com.ibm.db2:jcc:11.5.8.0"}
]
export default {
  name: "PluginsInstalling",
  components: {},
  props: {
    plugin: null,
  },
  data() {
    return {
      dialog: false,
      installing: false,
      databaseSelect: [],
      databases: DATABASES
    };
  },
  methods: {
    install() {
      console.log("Install plugin", this.plugin)
      this.installing = true
      installPlugin({pluginId: this.plugin.pluginId, jars: this.databaseSelect}).then(() => {
        this.$store.dispatch('loadPlugins')
      }).finally(() => {
        this.dialog = false
        this.installing = false
      })
    }
  },
  computed: {
    showJarSelector() {
      return this.plugin.pluginId.includes('jdbc')
    }
  }
}
</script>
