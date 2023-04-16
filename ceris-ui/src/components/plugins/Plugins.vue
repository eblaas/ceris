<template>
  <div class="pa-4">
    <v-data-table
        :loading="loading.plugins"
        :headers="headers"
        :items="pluginsFiltered"
        item-key="class"
        class="elevation-1"
        sort-by="type,ASC"
        single-expand
        hide-default-footer
        disable-pagination
        @click:row="(item, slot) => slot.expand(!slot.isExpanded)"
        show-expand
        fixed-header
        height="80vh"
    >
      <template v-slot:item.type="{ item }">
        <v-chip x-small color="darker">{{ item.type }}</v-chip>
      </template>
      <template v-slot:item.manifest.documentation="{ item }">
        <a v-if="item.manifest" target="_blank" :href=item.manifest.documentation>Doc</a>
      </template>
      <template v-slot:item.manifest.license="{ item }">
        <a v-if="item.manifest" target="_blank" :href=item.manifest.license.url>{{ item.manifest.license.name }}</a>
      </template>
      <template v-slot:item.location="{ item }">
        <v-btn v-if="isAdmin" color="primary" icon title="New connector" @click.native.stop @click="newConnector(item)">
          <v-icon small>mdi-plus-thick</v-icon>
        </v-btn>
        <v-btn v-if="isAdmin && item.id" color="primary" icon title="Delete" @click.native.stop @click="deletePlugin(item)">
          <v-icon small>mdi-delete</v-icon>
        </v-btn>
      </template>
      <template v-slot:expanded-item="{ headers, item }">
        <td :colspan="headers.length">
          <v-list dense>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>Class</v-list-item-title>
                <v-list-item-subtitle>{{ item.class }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>Location</v-list-item-title>
                <v-list-item-subtitle>{{ item.location }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>Description</v-list-item-title>
                <v-list-item-subtitle v-if="item.manifest" class="text-wrap">{{
                    item.manifest.description
                  }}
                </v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>PluginId</v-list-item-title>
                <v-list-item-subtitle v-if="item.manifest">{{ item.manifest.pluginId }}</v-list-item-subtitle>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </td>
      </template>
      <template v-slot:top>
        <v-toolbar flat>
          <v-spacer></v-spacer>
          <v-autocomplete
              dense
              multiple
              chips
              deletable-chips
              attach
              :items="pluginTypes"
              v-model="filterTypes"
          />
          <v-spacer></v-spacer>
          <PluginsAdd :success="loadPlugins"></PluginsAdd>
        </v-toolbar>
        <v-dialog v-model="dialogDelete" max-width="500px">
          <v-card>
            <v-card-title class="text-h5">Are you sure you want to delete this item?</v-card-title>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn color="blue-darken-1" variant="text" @click="dialogDelete = false">Cancel</v-btn>
              <v-btn color="blue-darken-1" variant="text" @click="deleteItemConfirm">OK</v-btn>
              <v-spacer></v-spacer>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </template>
    </v-data-table>
  </div>
</template>

<script>

import PluginsAdd from "./PluginsAdd";
import {uninstallPlugin} from "../../api";
import {mapActions, mapGetters} from "vuex";

export default {
  name: "Plugins",
  components: {
    PluginsAdd
  },
  data() {
    return {
      dialogDelete: false,
      pluginToDelete: null,
      expanded: [],
      search: null,
      filterTypes: ['source', 'sink'],
      headers: [{
        text: 'Type',
        align: 'start',
        sortable: true,
        value: 'type',
      }, {
        text: 'Plugin Name',
        align: 'start',
        sortable: true,
        value: 'name',
      }, {
        text: 'Version',
        align: 'start',
        sortable: true,
        value: 'version',
      }, {
        text: 'License',
        align: 'start',
        sortable: false,
        value: 'manifest.license',
      }, {
        text: 'Doc',
        align: 'start',
        sortable: false,
        value: 'manifest.documentation',
      }, {
        text: 'Actions',
        align: 'start',
        sortable: false,
        value: 'location',
      }, {
        text: '',
        value: 'data-table-expand'
      }]
    };
  },
  methods: {
    ...mapActions(['overlay', 'loadPlugins']),
    deletePlugin: function (item) {
      this.pluginToDelete = item
      this.dialogDelete = true
    },
    newConnector: function (item) {
      this.$router.push({path: '/connectors/new', query: {'c': item.class}})
    },
    deleteItemConfirm: function () {
      this.dialogDelete = false
      this.overlay(true)
      uninstallPlugin(this.pluginToDelete)
          .then(() => this.loadPlugins().then(() => this.overlay(false)))
    }
  },
  mounted() {
    this.loadPlugins()
  },
  computed: {
    ...mapGetters(['isAdmin', 'plugins', 'pluginsCount', 'loading']),
    pluginsFiltered() {
      return this.plugins.filter((p) => this.filterTypes.includes(p.type))
    },
    pluginTypes() {
      return Array.from(new Set(this.plugins.map((p) => p.type)))
    }
  }
}
</script>