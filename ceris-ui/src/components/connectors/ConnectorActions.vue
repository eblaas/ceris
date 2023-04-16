<template>
  <v-menu
      open-on-hover
      bottom
  >
    <template v-slot:activator="{ on, attrs }">
      <v-btn
          color="primary"
          icon
          v-bind="attrs"
          v-on="on"
          v-if="isAdmin"
      >
        <v-icon>mdi-dots-horizontal-circle-outline</v-icon>
      </v-btn>
    </template>
    <div class="primary">
      <v-btn icon title="Delete" @click.native.stop @click="remove()">
        <v-icon small>mdi-delete</v-icon>
      </v-btn>
      <v-btn title="Edit" icon @click.native.stop @click="editConnector()">
        <v-icon small>mdi-pencil</v-icon>
      </v-btn>
      <v-btn title="Restart" icon @click.native.stop @click="restart()">
        <v-icon small>mdi-restart</v-icon>
      </v-btn>
      <v-btn title="Pause" icon @click.native.stop @click="pause()">
        <v-icon small>mdi-pause</v-icon>
      </v-btn>
      <v-btn title="Resume" icon @click.native.stop @click="resume()">
        <v-icon small>mdi-play</v-icon>
      </v-btn>
    </div>
  </v-menu>
</template>

<script>


import {deleteConnector, pauseConnector, restartConnector, resumeConnector} from "@/api";
import {mapActions, mapGetters} from "vuex";

export default {
  name: "ConnectorActions",
  props: {
    connector: null
  },
  data() {
    return {}
  },
  methods: {
    ...mapActions(['overlay', 'loadConnectors']),
    withOverlay(callback) {
      this.overlay(true)
      callback(this.connector)
          .finally(() => setTimeout(() => {
            this.$emit('on-action', this.connector)
            this.loadConnectors()
            this.overlay(false)
          }, 2000))
    },
    restart() {
      this.withOverlay(restartConnector)
    },
    pause() {
      this.withOverlay(pauseConnector)
    },
    resume() {
      this.withOverlay(resumeConnector)
    },
    remove() {
      this.withOverlay(deleteConnector)
    },
    editConnector() {
      this.$router.push(`/connectors/${this.connector}/edit`)
    }
  },
  computed: {
    ...mapGetters(['isAdmin'])
  }
}
</script>