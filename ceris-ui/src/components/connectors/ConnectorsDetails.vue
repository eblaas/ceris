<template>
  <div class="pa-4">
    <v-toolbar>
      <v-tabs
          v-model="tab"
          center-active
          centered
      >
        <v-tab>State</v-tab>
        <v-tab>Datasets</v-tab>
        <v-tab>Metrics</v-tab>
      </v-tabs>
      <v-spacer></v-spacer>
      <connector-actions :connector="connector" @on-action="reload()"/>
    </v-toolbar>

    <v-window v-model="tab">
      <v-window-item>
        <connector-state :key="componentKey" :name="connector"/>
      </v-window-item>
      <v-window-item>
        <connector-datasets :key="componentKey" :connector="connector"/>
      </v-window-item>
      <v-window-item>
        <metrics-table :key="componentKey" :filter="{connector}"/>
      </v-window-item>
    </v-window>
  </div>
</template>

<script>
import ConnectorState from "./ConnectorState";
import MetricsTable from "../metrics/MetricsTable";
import ConnectorActions from "./ConnectorActions";
import ConnectorDatasets from "./ConnectorDatasets";

export default {
  name: "ConnectorsDetails",
  components: {
    ConnectorDatasets,
    ConnectorActions,
    MetricsTable,
    ConnectorState
  },
  data() {
    return {
      connector: this.$route.params.connector,
      topics: [],
      tab: null,
      componentKey: 1
    }
  },
  methods: {
    reload() {
      this.componentKey += 1
    }
  }
}
</script>
