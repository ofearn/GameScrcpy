<template>
  <div class="gs-tools-bar q-electron-drag" @contextmenu.prevent="event.preventDefault()">
    <div class="gs-tools-bar-btns q-electron-drag--exception">
      <div>
        <q-icon name="bt-shezhi" />
      </div>
      <div @click="minimizeApp">
        <q-icon name="bt-zuixiaohua" />
      </div>
      <div class="gs-tools-bar-btns-close" @click="closeApp">
        <q-icon name="bt-guanbi" />
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent } from 'vue'
export default defineComponent({
  name: 'ToolsBar',
  setup() {
    const xw = (window as any).xw
    const minimizeApp = () => {
      xw.windows.minimize()
    }
    const closeApp = () => {
      xw.windows.close()
    }
    return { minimizeApp, closeApp }
  },
})
</script>
<style lang="scss" scoped>
.gs-tools-bar {
  height: 64px;
  line-height: 64px;
  color: #fff;
  position: relative;
  border-top-right-radius: 5px;
  &-btns {
    display: flex;
    position: absolute;
    right: 0;
    top: 0;
    height: 25px;
    line-height: 25px;
    border-top-right-radius: 5px;
    &-close {
      border-top-right-radius: 5px;
    }
    div {
      padding: 0 8px;
      cursor: pointer;
      &:hover {
        background: rgba($color: #fff, $alpha: 0.05);
      }
    }
  }
}
</style>
