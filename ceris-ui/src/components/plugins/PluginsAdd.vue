<template>
  <div class="text-center">
    <v-dialog
        v-model="dialog"
        width="500"
        persistent
    >
      <template v-slot:activator="{ on, attrs }">
        <v-btn
            title="Download Plugin from Confluent Hub"
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
          Install Plugin from Confluent Hub
          <v-btn icon class="ml-5" title="Search at Confluent Hub" href="https://www.confluent.io/hub/" target="_blank">
            <v-icon color="blue">mdi-cloud-search-outline</v-icon>
          </v-btn>
        </v-card-title>
        <v-card-text>
          <v-alert border="left" outlined type="warning">
            Licenses are automatically accepted during installation.
          </v-alert>
          <v-form ref="form" v-model="valid">
            <v-text-field v-model="pluginId"
                          label="Plugin with format: <owner>/<plugin>:<version>"
                          type="text"
                          :rules="pluginRules"
                          required/>
          </v-form>
        </v-card-text>
        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn text :disabled='loading' @click="dialog = false">
            Cancel
          </v-btn>
          <v-btn :disabled='!valid || loading'
                 color="primary"
                 text
                 @click="save()"
          >
            Accept License and Install
          </v-btn>
        </v-card-actions>
        <v-progress-linear v-if="loading" indeterminate color="blue"/>
      </v-card>
    </v-dialog>
  </div>
</template>
<script>

import {installPlugin} from "../../api";

export default {
  name: "PluginsAdd",
  components: {},
  props: {
    success: null,
  },
  data() {
    return {
      dialog: false,
      error: null,
      pluginId: null,
      valid: false,
      loading: false,
      pluginRules: [
        v => !!v || 'Plugin is required',
        v => (v && /.+\/.+:.+/.test(v)) || 'Format: <owner>/<plugin>:<version>',
      ],
    };
  },
  methods: {
    save() {
      this.loading = true
      installPlugin({pluginId: this.pluginId}).then(() => {
        this.dialog = false
        this.$refs.form.reset()
        this.success()
      }).finally(() => this.loading = false)
    }
  }
}
</script>
