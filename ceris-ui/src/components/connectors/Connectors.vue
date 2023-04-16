<template>
  <div class="pa-4">
    <v-data-table
        :loading="loading.connectors"
        :headers="headers"
        :items="connectors"
        item-key="name"
        class="elevation-1"
        :search="search"
        hide-default-footer
        disable-pagination
        @click:row="showConnectorDetails"
        fixed-header
        height="80vh"
    >
      <template v-slot:item.type="{ item }">
        <v-chip x-small color="darker">{{ item.type }}</v-chip>
      </template>
      <template v-slot:item.name="{ item }">
        <router-link class="router-link-exact-active" :to="`connectors/${item.name}`">{{ item.name }}</router-link>
      </template>
      <template v-slot:item.tasksErrors="{ item }">
        <v-icon v-if="item.hasError" :connector="item" color="red">mdi-alert-outline</v-icon>
      </template>
      <template v-slot:item.actions="{ item }">
        <connector-actions @on-action="loadConnectors" :connector="item.name"/>
      </template>
      <template v-slot:top>
        <v-toolbar flat>
          <v-spacer></v-spacer>
          <v-text-field
              v-model="search"
              label="Search"
              class="mx-4"
              clearable
          ></v-text-field>
          <v-spacer></v-spacer>
          <v-btn v-if="isAdmin" small color="primary darken-2" title="Create a new connector" @click="newConnector()">
            New connector
          </v-btn>
          <v-btn icon color="primary" title="Reload" @click="loadConnectors()">
            <v-icon>mdi-reload</v-icon>
          </v-btn>
        </v-toolbar>
      </template>
    </v-data-table>
  </div>
</template>

<script>


import {mapActions, mapGetters} from "vuex";
import ConnectorActions from "./ConnectorActions";

export default {
  name: "Connectors",
  components: {ConnectorActions},
  data() {
    return {
      search: null,
      headers: [{
        text: 'Type',
        align: 'start',
        sortable: true,
        value: 'type',
      }, {
        text: 'Connector Name',
        align: 'start',
        sortable: true,
        value: 'name',
      }, {
        text: 'Status',
        align: 'start',
        sortable: true,
        value: 'state',
      }, {
        text: 'Tasks',
        align: 'start',
        sortable: true,
        value: 'tasksCount',
      }, {
        text: 'Tasks Status',
        align: 'start',
        sortable: true,
        value: 'tasksState',
      }, {
        text: 'Errors',
        align: 'center',
        sortable: true,
        value: 'tasksErrors',
      }, {
        text: 'Actions',
        align: 'center',
        sortable: false,
        value: 'actions',
      }]
    };
  },
  methods: {
    ...mapActions(['loadConnectors', 'overlay']),
    showConnectorDetails(connector) {
      this.$router.push(`connectors/${connector.name}`)
    },
    newConnector() {
      this.$router.push('connectors/new')
    }
  },
  mounted() {
    this.loadConnectors()
  },
  computed: {
    ...mapGetters(['isAdmin', 'connectors', 'loading']),
  }
}
</script>