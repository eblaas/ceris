<template>
  <div class="pa-4">
    <v-card>
      <v-card-title>
        Schema: {{ dataset }}
        <v-spacer/>
        <router-link class="router-link-active" :to="`${dataset}/messages`" exact>Data</router-link>
      </v-card-title>
      <prism-editor
          class="config-editor"
          v-model="schema"
          :readonly="true"
          :highlight="highlighter"
          :line-numbers="true"
      />
    </v-card>
    <v-card-text>
    </v-card-text>
  </div>
</template>

<script>

import {PrismEditor} from "vue-prism-editor"
import "vue-prism-editor/dist/prismeditor.min.css"
import {highlight, languages} from "prismjs/components/prism-core"
import "prismjs/components/prism-json"
import "prismjs/themes/prism-solarizedlight.css"
import {fetchTopicSchema} from "../../api";

export default {
  name: "DatasetSchema",
  components: {
    PrismEditor
  },
  data() {
    return {
      schema: "",
    };
  },
  methods: {
    highlighter(code) {
      return highlight(code, languages.json)
    },
  },
  mounted() {
    fetchTopicSchema(this.dataset).then((response) => {
      try {
        response.data.keySchema.schema = JSON.parse(response.data.keySchema.schema)
      } catch (e) {
        //
      }
      try {
        response.data.valueSchema.schema = JSON.parse(response.data.valueSchema.schema)
      } catch (e) {
        //
      }
      this.schema = JSON.stringify(response.data, null, 2)
    })
  },
  computed: {
    dataset() {
      return this.$route.params.dataset
    }
  }

}
</script>

<style lang="scss">
.config-editor {
  background: #2d2d2d;
  color: #ccc;
  font-family: Fira code, Fira Mono, Consolas, Menlo, Courier, monospace;
  font-size: 12px;
  line-height: 1.5;
  padding: 5px;
}

.prism-editor__textarea:focus {
  outline: none;
}
</style>
