<template>
  <div class="pa-4">
    <v-data-table
        dense
        item-key="ts"
        :headers="headers"
        :items="data"
        class="elevation-1"
        :hide-default-footer="true"
        disable-pagination
        :loading="loading"
    >
      <template v-slot:item.ts="{ item }">
        <v-chip x-small color="darker">{{ item.ts | timestamp }}</v-chip>
      </template>
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title>Sample Data for: {{ title }}</v-toolbar-title>
          <v-spacer></v-spacer>
        </v-toolbar>
      </template>
    </v-data-table>
  </div>
</template>

<script>

import {fetchMessages} from "@/api";

export default {
  name: "DatasetMessages" +
      "",
  data() {
    return {
      loading: false,
      data: [],
      headers: [
        {
          text: "Receive time",
          align: 'start',
          sortable: true,
          value: "ts",
        }
      ]
    };
  },
  methods: {},
  mounted() {
    this.loading = true;
    fetchMessages(this.$route.params.dataset).then((response) => {
          if (response.data && response.data.length) {
            const flatten = (object, prefix = '') => {
              return Object.keys(object).reduce((prev, element) => {
                return object[element] && typeof object[element] == 'object' && !Array.isArray(element)
                    ? {...prev, ...flatten(object[element], `${prefix}${element}.`)}
                    : {...prev, ...{[`${prefix}${element}`]: object[element]}}
              }, {})
            }
            let firstRow = response.data[0]
            let keyHeaders = Object.keys(flatten(firstRow.key))
            let value = firstRow.value.op ? firstRow.value.after || firstRow.value.before : firstRow.value

            this.headers = this.headers.concat(Object.keys(flatten(value)).map(col => ({
              text: col + (keyHeaders.includes(col) ? '  (key)' : ''),
              align: 'start',
              sortable: true,
              value: col,
            })))
            this.data = response.data.map(e => ({...{ts: e.ts}, ...(e.value.op ? e.value.after : e.value)}))
          }

        }
    ).finally(() => this.loading = false)
  },
  computed: {
    title() {
      return this.$route.params.dataset
    }
  },
}
</script>