<template>
  <div class="pa-0">
    <prism-editor
        class="config-editor"
        v-model="configurationString"
        :highlight="highlighter"
        :line-numbers="true"
        @input="onChange"
    ></prism-editor>
    <v-alert class="my-2" style="white-space: pre-line" v-if="error" border="left" outlined type="error">
      {{ error }}
    </v-alert>
  </div>
</template>

<script>

import {PrismEditor} from "vue-prism-editor"
import "vue-prism-editor/dist/prismeditor.min.css"
import {highlight, languages} from "prismjs/components/prism-core"
import "prismjs/components/prism-json"
import "prismjs/themes/prism-solarizedlight.css"
import {validateConfiguration} from "@/api";

export default {
  name: "ConnectorJson",
  components: {
    PrismEditor
  },
  props: ['value', 'edit'],
  data() {
    return {
      dialog: false,
      error: null,
      configurationString: JSON.stringify(this.value, null, 2),
      originalName: this.edit ? this.value.name : null,
      configuration: null,
    };
  },
  methods: {
    highlighter(code) {
      return highlight(code, languages.json)
    },
    onChange() {
      this.$emit('update:valid', false)
      this.$emit('update:changed', true)
    },
    validate() {
      try {
        this.configuration = JSON.parse(this.configurationString)
        if (Object.keys(this.configuration).some(key => this.configuration[key] && typeof this.configuration[key] === 'object'))
          throw Error("Nested objects not supported")
        if (this.edit && this.originalName != this.configuration.name)
          throw Error("Name can't be changed.")
      } catch (e) {
        this.error = `Invalid JSON:  ${e}`
        return
      }
      this.$emit('update:loading', true)
      validateConfiguration(this.configuration, false)
          .then((response) => {
            let errors = response.data.configs.flatMap(d => d.value.errors.length ? [`${d.definition.name} : ${d.value.errors.join(",")}`] : [])
            if (errors.length) {
              this.error = errors.join("\n")
              this.$emit('update:valid', false)
            } else {
              this.error = null
              this.$emit('input', this.configuration)
              this.$emit('update:valid', true)
            }
          }).finally(() => this.$emit('update:loading', false))
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
