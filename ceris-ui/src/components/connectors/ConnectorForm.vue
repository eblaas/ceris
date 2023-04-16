<template>
  <div style="min-height: 100px" class="pa-0">
    <v-form ref="form" autocomplete="off">
      <v-jsf class="pa-0" ref="f"
             v-if="schema"
             v-model="configuration"
             :schema="schema"
             :options="options"
             @change-child="onInputChange($event)">
        <template slot="allOf-0.topics-before">
          <v-select style="width: 30%;float: left;margin-right: 10px"
                    multiple
                    v-model="topicFilter"
                    :items="connectorFilterOptions"
                    label="Filter by source connector" @change="validate()"/>
        </template>
      </v-jsf>
    </v-form>
  </div>
</template>

<script>
import VJsf from '@koumoul/vjsf/lib/VJsf.js'
import '@koumoul/vjsf/lib/VJsf.css'

import {fetchTopics, validateConfiguration} from "@/api";
import Vue from 'vue'


export default {
  name: 'ConnectorForm',
  components: {VJsf},
  props: ['value', 'edit'],
  data() {
    return {
      valid: false,
      configuration: this.value,
      selectedTabs: [0, 0],
      schema: null,
      topic: [],
      topicFilter: [],
      options: {
        initialValidation: "all",
        "useValidator": true,
        "validator": function (m) {
          return () => (m.properties && m.hasError) ? "" : (m.error || null)
        }
      }
    }
  },
  methods: {
    validate() {
      this.$emit('update:loading', true)

      if (this.$refs.f && this.$refs.f.currentTab) {
        this.selectedTabs[0] = parseInt(this.$refs.f.currentTab)
      }

      return validateConfiguration(this.configuration)
          .then((schema) => {
            this.schema = null
            this.$nextTick(() => {
              this.schema = schema
              this.schema["x-props"] = {
                value: `tab-root-allOf-${this.selectedTabs[0]}`
              }
              this.schema.allOf[this.selectedTabs[0]]["x-props"] = {
                value: `tab-allOf-${this.selectedTabs[0]}-allOf-${this.selectedTabs[1]}`
              }

              if (this.schema.allOf[0].properties['topics'])
                Vue.set(this.schema.allOf[0].properties['topics'], 'anyOf', this.topicOptions)

              if (this.edit)
                Vue.set(this.schema.allOf[0].properties['name'], 'readOnly', true)

              this.$nextTick(() => {
                this.$emit('input', this.configuration)
                this.$emit('update:valid', schema.valid)
              })
            })
          }).finally(() => this.$emit('update:loading', false))
    },
    onInputChange(event) {

      if (event.fullKey) {
        const tabs = event.fullKey.match(/^allOf-(\d+)\.(?:allOf-(\d+)?)?/)
        if (tabs) {
          this.selectedTabs = [parseInt(tabs[1]), parseInt(tabs[2] || "0")]
        }
      }
      if (event.fullKey && (
          /.*\.transforms$/.test(event.fullKey) || /.*\.transforms\..*type$/.test(event.fullKey) ||
          /.*\.predicates$/.test(event.fullKey) || /.*\.predicates\..*type$/.test(event.fullKey)
      )) {
        this.validate()
      }
      this.$emit('update:changed', true)
      this.$emit('update:valid', false)
    }
  },
  created() {
    fetchTopics().then((response) => {
      this.topics = response.data
      this.validate()
    })
  },
  computed: {
    topicOptions() {
      return this.topics
          .filter(e => this.topicFilter.length ? this.topicFilter.includes(e.connector) : true)
          .flatMap(e => e.topics.map(t => ({
            title: t + " (" + e.connector + ")",
            connector: e.connector,
            const: t
          })))
    },
    connectorFilterOptions() {
      return this.topics.map(e => e.connector)
    }
  }
}
</script>