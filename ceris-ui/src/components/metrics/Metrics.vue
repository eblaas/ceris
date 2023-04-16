<template>
  <div class="pa-4">
    <metrics-table/>
  </div>
</template>

<script>

import MetricsTable from "./MetricsTable";

export default {
  name: "Metrics",
  components: {MetricsTable},

  data() {
    return {}
  }
}
</script>