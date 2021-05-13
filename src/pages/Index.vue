<template>
  <div class="gs-navbar">
    <navbar-logo></navbar-logo>
    <navbar-menu></navbar-menu>
  </div>
  <div class="gs-main">
    <Tools-bar></Tools-bar>
    <main-index></main-index>
  </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
import NavbarLogo from 'components/navbar/Logo.vue'
import NavbarMenu from 'components/navbar/Menu.vue'
import ToolsBar from 'components/toolsBar/Index.vue'
import MainIndex from 'components/main/Index.vue'
export default defineComponent({
  name: 'Index',
  components: {
    NavbarLogo,
    NavbarMenu,
    ToolsBar,
    MainIndex,
  },
  setup() {
    return {}
  },
})
</script>
<style lang="scss" scoped>
.gs-navbar {
  width: 208px;
  color: #fff;
  border-top-left-radius: 5px;
  border-bottom-left-radius: 5px;
}
.gs-main {
  flex: 1;
  border-top-right-radius: 5px;

  display: flex;
  flex-direction: column;
}
</style>
