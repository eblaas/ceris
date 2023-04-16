<template>
  <div v-if="state">
    <v-card>
      <v-card-title>
        Connector: {{ state.name }}
      </v-card-title>
      <v-card-subtitle>
        <div>Status: {{ state.connector.state }}</div>
        <div>Worker: {{ state.connector.worker_id }}</div>
      </v-card-subtitle>
      <v-card-text v-if="state.connector.trace">
        <v-expansion-panels>
          <v-expansion-panel focusable>
            <v-expansion-panel-header>
              <v-row>
                <v-icon color="red" class="mr-3">mdi-alert-outline</v-icon>
                <span class="grey--text">{{ state.connector.trace.split("\n")[0] }}</span></v-row>
            </v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-textarea
                  filled
                  auto-grow
                  label="Trace"
                  shaped
                  class="trace text-caption"
                  readonly
                  :value="state.connector.trace"
              ></v-textarea>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-text>
    </v-card>
    <v-card v-for="(task, index) in state.tasks" :key="index">
      <v-card-title>
        Task id: {{ task.id }}
      </v-card-title>
      <v-card-subtitle>
        <div>Status: {{ task.state }}</div>
        <div>Worker: {{ task.worker_id }}</div>
      </v-card-subtitle>
      <v-card-text v-if="task.trace">
        <v-expansion-panels>
          <v-expansion-panel focusable>
            <v-expansion-panel-header>
              <v-row>
                <v-icon color="red" class="mr-3">mdi-alert-outline</v-icon>
                <span class="grey--text">{{ task.trace.split("\n")[0] }}</span></v-row>
            </v-expansion-panel-header>
            <v-expansion-panel-content>
              <v-textarea
                  filled
                  auto-grow
                  label="Trace"
                  shaped
                  class="trace text-caption"
                  readonly
                  :value="task.trace"
              ></v-textarea>
            </v-expansion-panel-content>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-text>
    </v-card>
  </div>
</template>

<script>

import {fetchConnectorState} from "@/api";

export default {
  name: "ConnectorState",
  props: {
    name: null,
  },
  data() {
    return {
      state: null,
      intervalId: null,
    }
  },
  methods: {
    loadState() {
      fetchConnectorState(this.name).then((state) => this.state = state.data)
    }
  },
  mounted() {
    this.loadState()
    this.intervalId = setInterval(() => this.loadState(), 3000)
  },
  beforeDestroy() {
    clearInterval(this.intervalId)
  }
}
</script>

<style lang="scss">

.trace textarea {
  line-height: 1.5;
}
</style>