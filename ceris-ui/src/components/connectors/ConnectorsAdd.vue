<template>
  <div class="pa-4 mb-10">
    <v-stepper v-model="steps" alt-labels>
      <v-stepper-header>
        <v-stepper-step step="1" :complete="steps > 1">
          Select plugin
        </v-stepper-step>
        <v-divider></v-divider>
        <v-stepper-step step="2" :complete="steps > 2">
          Configuration
        </v-stepper-step>
        <v-divider></v-divider>
        <v-stepper-step step="3">
          Finish
        </v-stepper-step>
      </v-stepper-header>
      <v-stepper-items>
        <v-stepper-content step="1" class="pa-0">
          <v-card class="pa-2">
            <v-select :items="pluginOptions" label="Select connector plugin" @change="finishStep1">
              <template slot="item" slot-scope="data">
                <v-icon class="mr-3">mdi-database-{{ data.item.type == 'source' ? 'export' : 'import' }}</v-icon>
                <span class="cb-item">{{ data.item.text }}</span>
              </template>
            </v-select>
          </v-card>
          <v-footer fixed>
            <v-row class="pa-3" style="margin-left: 230px">
              <v-btn v-if="configuration" text @click="steps = 2">
                Next
              </v-btn>
            </v-row>
          </v-footer>
        </v-stepper-content>
        <v-stepper-content step="2" class="pa-0">
          <v-card>
            <ConnectorForm ref="connectorForm"
                           v-if="steps==2 && showForm"
                           :valid.sync="valid"
                           :loading.sync="loading"
                           v-model="configuration"/>
            <ConnectorJson ref="connectorJson"
                           v-if="steps==2 && ! showForm"
                           :valid.sync="valid"
                           :loading.sync="loading"
                           v-model="configuration"/>
          </v-card>
          <v-footer fixed>
            <v-row class="pa-3" style="margin-left: 230px">
              <v-btn text @click="steps = 1">
                Back
              </v-btn>
              <v-btn text color="primary" :disabled="loading || valid" @click="validateForm()"
                     title="Validate configuration">
                Validate
              </v-btn>
              <v-btn text color="primary" :disabled="!valid || loading" @click="finishStep2()" title="Create connector">
                Create
              </v-btn>
              <v-spacer/>
              <v-btn fab x-small color="primary darken-2" @click="showForm=!showForm" title="Switch input mode">
                <v-icon>{{ showForm ? 'mdi-code-json' : 'mdi-form-select' }}</v-icon>
              </v-btn>
            </v-row>
          </v-footer>
        </v-stepper-content>
        <v-stepper-content step="3" class="pa-0">
          <v-card>
            <v-card-title>Connector created, load current status ...</v-card-title>
            <v-divider/>
            <connector-state v-if="steps==3" :name="configuration.name"></connector-state>
            <v-divider/>
            <v-footer fixed>
              <v-row class="pa-3" style="margin-left: 230px">
                <v-btn text color="primary" @click="$router.push(`${configuration.name}`)">
                  Details Page
                </v-btn>
              </v-row>
            </v-footer>
          </v-card>
        </v-stepper-content>
      </v-stepper-items>
    </v-stepper>
  </div>
</template>
<script>

import {saveConnector} from "@/api";
import ConnectorForm from "./ConnectorForm";
import ConnectorJson from "./ConnectorJson";
import ConnectorState from "./ConnectorState";
import {mapActions, mapGetters} from "vuex";


export default {
  name: "ConnectorAdd",
  components: {ConnectorForm, ConnectorJson, ConnectorState},
  data() {
    return {
      showForm: true,
      steps: this.$route.query.c ? 2 : 1,
      valid: false,
      loading: false,
      configuration: this.$route.query.c ? {'connector.class': this.$route.query.c} : null
    }
  },
  methods: {
    ...mapActions(['overlay']),
    validateForm() {
      (this.showForm ? this.$refs.connectorForm : this.$refs.connectorJson).validate()
    },
    finishStep1(plugin) {
      this.configuration = {
        'connector.class': plugin.class,
      }
      this.steps = 2
    },
    finishStep2() {
      this.overlay(true)
      saveConnector(this.configuration)
          .then(() => setTimeout(() => {
            this.overlay(false)
            this.steps = 3
          }, 2000))
    },
  },
  mounted() {
  },
  computed: {
    ...mapGetters(['connectorPlugins']),
    pluginOptions() {
      return this.connectorPlugins.map(plugin => ({
        text: `${plugin.name} (${plugin.type})`,
        type: plugin.type,
        value: plugin
      }))
    }
  }
}
</script>
