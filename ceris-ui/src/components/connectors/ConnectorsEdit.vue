<template>
  <div class="pa-4">
    <v-card>
      <ConnectorForm ref="connectorForm" v-if="configuration && showForm"
                     edit="true"
                     :valid.sync="valid"
                     :changed.sync="changed"
                     :loading.sync="loading"
                     v-model="configuration"/>
      <ConnectorJson ref="connectorJson" v-if="configuration && !showForm"
                     :valid.sync="valid"
                     edit="true"
                     :changed.sync="changed"
                     :loading.sync="loading"
                     v-model="configuration"/>
      <v-card-actions>
        <v-footer fixed>
          <v-row class="pa-3" style="margin-left: 230px">
            <v-btn text @click="$router.go(-1)">
              Cancel
            </v-btn>
            <v-btn text color="primary" :disabled="loading || valid" @click="validateForm()">
              Validate
            </v-btn>
            <v-btn text color="primary" :disabled="!valid || loading || !changed" @click="save()">
              Update
            </v-btn>
            <v-spacer/>
            <v-btn fab x-small color="primary darken-2" @click="showForm=!showForm">
              <v-icon>{{ showForm ? 'mdi-code-json' : 'mdi-form-select' }}</v-icon>
            </v-btn>
          </v-row>
        </v-footer>
      </v-card-actions>
    </v-card>
  </div>
</template>
<script>

import {fetchConnectorConfig, saveConnector} from "@/api"
import ConnectorForm from "./ConnectorForm"
import ConnectorJson from "./ConnectorJson"


export default {
  name: "ConnectorEdit",
  components: {ConnectorForm, ConnectorJson},
  data() {
    return {
      showForm: true,
      valid: false,
      changed: false,
      loading: false,
      configuration: null
    }
  },
  methods: {
    validateForm() {
      (this.showForm ? this.$refs.connectorForm : this.$refs.connectorJson).validate()
    },

    save() {
      saveConnector(this.configuration).then(() => this.$router.go(-1))
    },
  },
  mounted() {
    fetchConnectorConfig(this.$route.params.connector)
        .then((response) => {
          // convert string values to boolean
          Object.keys(response.data).forEach((field) => {
            if (response.data[field] === "true") {
              response.data[field] = true
            }
            if (response.data[field] === "false") {
              response.data[field] = false
              //delete response.data[field]
            }
          })
          this.configuration = response.data
        })
  },
}
</script>
