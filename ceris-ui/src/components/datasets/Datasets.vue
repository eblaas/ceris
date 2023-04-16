<template>
  <div class="pa-4">
    <v-data-table
        dense
        :headers="headers"
        :items="datasets"
        item-key="dataset"
        :search="search"
        sort-by="dataset"
        group-by="connector"
        hide-default-footer
        disable-pagination
        :show-group-by="false"
        fixed-header
        height="80vh"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-spacer></v-spacer>
          <v-text-field
              v-model="search"
              label="Search"
              class="mx-4"
          ></v-text-field>
          <v-spacer></v-spacer>
        </v-toolbar>
      </template>
      <template v-slot:item.messages="{ item }">
        <router-link :to="`datasets/${item.dataset}/messages`" exact>Load</router-link>
      </template>
      <template v-slot:item.definition="{ item }">
        <router-link :to="`datasets/${item.dataset}`" exact>Show</router-link>
      </template>
    </v-data-table>
  </div>
</template>

<script>

import {fetchTopics} from "@/api";

export default {
  name: "Datasets",
  data() {
    return {
      pageSize: "all",
      search: null,
      datasets: [],
      headers: [{
        text: 'Connector',
        align: 'start',
        sortable: true,
        value: 'connector',
      }, {
        text: 'Topic Name',
        align: 'start',
        sortable: true,
        value: 'dataset',
      }, {
        text: 'Schema Definition',
        align: 'start',
        sortable: false,
        value: 'definition',
      }, {
        text: 'Sample Data',
        align: 'start',
        sortable: false,
        value: 'messages',
      }, {
        text: 'DB Table',
        align: 'start',
        sortable: false,
        value: 'table',
      }, {
        text: 'DB Schema',
        align: 'start',
        sortable: false,
        value: 'schema',
      }, {
        text: ' DB Database',
        align: 'start',
        sortable: false,
        value: 'database',
      }]
    };
  },
  methods: {},
  mounted() {
    fetchTopics().then((response) => {
      this.datasets = response.data.flatMap((entry) => entry.topics.map((topic) => {
        let parts = topic.split('.')
        let table = parts.length > 0 ? parts.pop() : null
        let schema = parts.length > 0 ? parts.pop() : null
        let database = parts.length > 0 ? parts.pop() : null
        return {
          connector: entry.connector,
          dataset: topic,
          table: table,
          schema: schema,
          database: database
        }
      }))
    })
  }
}
</script>