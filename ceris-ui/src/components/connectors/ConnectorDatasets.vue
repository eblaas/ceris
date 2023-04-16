<template>
  <v-list flat>
    <v-list-item v-for="topic in topics" :key="topic">
      <v-list-item-icon>
        <v-icon>mdi-table</v-icon>
      </v-list-item-icon>
      <v-list-item-content>
        <v-list-item-title>
          {{ topic }}
        </v-list-item-title>
        <v-list-item-action-text>
          <router-link :to="`/datasets/${topic}`" exact>Schema</router-link>
          <span class="ma-1"></span>
          <router-link :to="`/datasets/${topic}/messages`" exact>Data</router-link>
        </v-list-item-action-text>
      </v-list-item-content>
    </v-list-item>
  </v-list>
</template>

<script>
import {fetchConnectorTopics} from "../../api";

export default {
  name: "ConnectorDatasets",
  props: {
    connector: null
  },
  data() {
    return {
      topics: [],
    }
  },
  mounted() {
    this.topics = fetchConnectorTopics(this.connector)
        .then(resp => this.topics = resp.data[this.connector]?.topics || [])
  }
}
</script>
