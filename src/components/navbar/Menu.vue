<template>
  <q-list padding class="rounded-borders text-w">
    <q-item clickable v-ripple :active="link === 'inbox'" @click="link = 'inbox'" active-class="my-menu-link">
      <q-item-section avatar>
        <q-icon name="bt-shouji" />
      </q-item-section>

      <q-item-section>我的设备</q-item-section>
    </q-item>

    <q-item clickable v-ripple :active="link === 'outbox'" @click="link = 'outbox'" active-class="my-menu-link">
      <q-item-section avatar>
        <q-icon name="bt-33beifen" />
      </q-item-section>

      <q-item-section>我的主机</q-item-section>
    </q-item>

    <q-item clickable v-ripple :active="link === 'trash'" @click="link = 'trash'" active-class="my-menu-link">
      <q-item-section avatar>
        <q-icon name="bt-jiaoben" />
      </q-item-section>

      <q-item-section>应用脚本</q-item-section>
    </q-item>
    <q-item clickable v-ripple :active="link === 'help'" @click="link = 'help'" active-class="my-menu-link">
      <q-item-section avatar>
        <q-icon name="bt-log" />
      </q-item-section>

      <q-item-section>运行日志</q-item-section>
    </q-item>
    <q-item clickable v-ripple :active="link === 'settings'" @click="link = 'settings'" active-class="my-menu-link">
      <q-item-section avatar>
        <q-icon name="bt-shezhi" />
      </q-item-section>

      <q-item-section>设置中心</q-item-section>
    </q-item>
  </q-list>
</template>
<script lang="ts">
import { defineComponent, ref } from 'vue'
export default defineComponent({
  name: 'NavbarMenu',
  setup() {
    return {
      link: ref('inbox'),
    }
  },
})
</script>
<style lang="scss" scoped>
.my-menu-link {
  color: white;
  background: linear-gradient(to right, #148eff, transparent);
}
</style>
