<template>
  <div class="pa-4">
    <v-data-table
        :loading="loading"
        :headers="headers"
        :items="plugins"
        item-key="pluginId"
        class="elevation-1"
        :footer-props="{'items-per-page-options': [pageSize,pageSize * 2]}"
        :items-per-page="pageSize"
        :search="search"
        single-expand
        @click:row="(item, slot) => slot.expand(!slot.isExpanded)"
        show-expand
        fixed-header
        height="75vh"
    >
      <template v-slot:item.icon="{ item }">
        <div class="white pa-1 ma-1" style="max-width: 45px">
          <v-img
              max-height="40"
              max-width="40"
              :src=item.icon
          ></v-img>
        </div>
      </template>
      <template v-slot:item.type="{ item }">
        <v-chip v-for="t in item.type" :key="t" x-small color="darker">{{ t }}</v-chip>
      </template>
      <template v-slot:item.documentation="{ item }">
        <a v-if="item.documentation" target="_blank" :href="item.documentation">Doc</a>
      </template>
      <template v-slot:item.license="{ item }">
        <a v-if="item.license" target="_blank" :href="item.license.url">{{ item.license.name }}</a>
      </template>
      <template v-slot:item.action="{ item }">
        <plugins-installing v-if="isAdmin" :plugin="item"></plugins-installing>
      </template>
      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          <v-list dense>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>PluginId</v-list-item-title>
                <v-list-item-subtitle>{{ item.pluginId }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>Description</v-list-item-title>
                <v-list-item-subtitle class="text-wrap">{{ item.description }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>Version</v-list-item-title>
                <v-list-item-subtitle>{{ item.version }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item v-if="item.tags">
              <v-list-item-content>
                <v-list-item-subtitle class="text-wrap">
                  <v-chip class="ma-1" v-for="t in item.tags" :key="t" x-small color="darker">{{ t }}</v-chip>
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </td>
      </template>
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title></v-toolbar-title>
          <v-spacer></v-spacer>
          <v-text-field
              v-model="search"
              label="Search"
              class="mx-4"
          ></v-text-field>
          <v-spacer></v-spacer>
        </v-toolbar>
      </template>
    </v-data-table>
  </div>
</template>

<script>

import PluginsInstalling from "./PluginsInstalling";
import {fetchPluginsStore} from "@/api";
import {mapGetters} from "vuex";

export default {
  name: "PluginsStore",
  components: {PluginsInstalling},
  data() {
    return {
      pageSize: 30,
      search: null,
      plugins: [],
      expanded: [],
      loading: false,
      headers: [{
        text: '',
        align: 'start',
        sortable: false,
        value: 'icon',
        width: "10%"
      }, {
        text: 'Name',
        align: 'start',
        sortable: true,
        value: 'title',
        width: "50%"
      }, {
        text: 'Type',
        align: 'start',
        value: 'type',
        width: "10%"
      }, {
        text: 'Owner',
        align: 'start',
        sortable: true,
        value: 'owner',
        width: "10%"
      }, {
        text: 'License',
        align: 'start',
        value: 'license',
        width: "10%"
      }, {
        text: 'Doc',
        align: 'start',
        value: 'documentation',
        width: "5%"
      }, {
        text: 'Action',
        value: 'action',
        width: "5%"
      }, {
        text: '',
        value: 'data-table-expand',
        width: "5%"
      }]
    };
  },
  methods: {},
  mounted() {
    this.loading = true
    fetchPluginsStore().then((response) => this.plugins = response.data).finally(() => this.loading = false)
  },
  computed: {
    ...mapGetters(['isAdmin'])
  }
}
</script>