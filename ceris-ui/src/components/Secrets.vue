<template>
  <div class="text-left">
    <v-dialog
        v-model="dialog"
        width="600"
    >
      <template v-slot:activator="{ on, attrs }">
        <a href="/" onclick="return false;" v-bind="attrs" v-on="on">Secrets</a>
      </template>

      <v-card>
        <v-card-title class="text-h5 lighten-2">
          Secret Env Variables
        </v-card-title>
        <v-card-text>
          <v-list dense>
            <v-list-item v-for="(item, i) in $store.state.secrets" :key="i">
              <v-list-item-icon>
                <v-btn small
                       icon
                       v-clipboard:copy="item"
                       v-clipboard:success="close">
                  <v-icon small>mdi-content-copy</v-icon>
                </v-btn>
              </v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title v-text="item"></v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>
<script>

export default {
  name: "Secrets",
  components: {},
  data() {
    return {
      dialog: false
    };
  },
  methods: {
    close: function () {
      this.dialog = false;
    }
  }
}
</script>
