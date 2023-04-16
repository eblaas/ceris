<template>
  <v-data-table
      dense
      :loading="loading"
      :headers="headers"
      :search="search"
      :items="metrics"
      item-key="dataset"
      class="elevation-1 fill-height overflow-y-hidden"
      hide-default-footer
      disable-pagination
      sort-by="connector"
      group-by="type"
      show-group-by
      fixed-header
      height="75vh"
  >
    <template v-slot:top>
      <v-toolbar flat>
        <v-spacer/>
        <v-text-field
            v-model="search"
            label="Search"
            class="mx-4"
        ></v-text-field>
        <v-spacer/>
        <v-btn icon color="primary" title="Reload" @click="reload">
          <v-icon>mdi-reload</v-icon>
        </v-btn>
      </v-toolbar>
    </template>
  </v-data-table>
</template>

<script>

import {fetchMetrics} from "@/api";

export default {
  name: "MetricsTable",
  props: {
    filter: {}
  },
  data() {
    return {
      search: null,
      loading: false,
      metrics: [],
      headers: [{
        text: 'Metric Type',
        align: 'start',
        sortable: true,
        groupable: true,
        value: 'type',
      }, {
        text: 'Metric Name',
        align: 'start',
        sortable: true,
        groupable: true,
        value: 'name',
      }, {
        text: 'Connector',
        align: 'start',
        sortable: true,
        groupable: true,
        value: 'connector',
      }, {
        text: 'Task',
        align: 'start',
        sortable: false,
        groupable: false,
        value: 'task',
      }, {
        text: 'Value',
        align: 'start',
        sortable: false,
        groupable: false,
        value: 'value',
      }]
    };
  },
  methods: {
    reload() {
      this.loading = true
      fetchMetrics(this.filter)
          .then((response) => this.metrics = response.data)
          .finally(() => this.loading = false)
    }
  },
  mounted() {
    this.reload()
  },
}
</script>