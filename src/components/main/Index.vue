<template>
  <div class="gs-main-content">
    <main-devices></main-devices>
  </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import MainDevices from './Devices.vue'
export default defineComponent({
  name: 'MainIndex',
  components: {
    MainDevices,
  },
  setup() {
    return {}
  },
})
</script>
<style lang="scss" scoped>
.gs-main-content {
  background: #fff;
  flex: 1;
  border-bottom-right-radius: 5px;
}
</style>
