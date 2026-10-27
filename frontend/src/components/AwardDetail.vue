<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { get, http, send, message, isCancelled } from '../api'
import { imageError, awardFields } from '../validation'
import type { Entity } from '../types'
const props = defineProps<{
    id: number
    editable: boolean
    nextId?: number
    className: (id: number) => string
    userName: (id: number) => string
  }>(),
  emit = defineEmits<{ close: []; edit: [record: Entity]; changed: []; next: [id: number] }>()
const record = ref<Entity | null>(),
  loading = ref(false),
  error = ref(''),
  imageLoading = ref(false),
  imageFailure = ref(''),
  url = ref(''),
  replacing = ref(false),
  replaceError = ref('')
let controller: AbortController | undefined,
  imageController: AbortController | undefined,
  version = 0,
  imageVersion = 0
function clearImage() {
  if (url.value) URL.revokeObjectURL(url.value)
  url.value = ''
}
function cancel() {
  controller?.abort()
  imageController?.abort()
  version++
  imageVersion++
  clearImage()
}
async function fetchImage(keepOld = false) {
  imageController?.abort()
  imageController = new AbortController()
  const signal = imageController.signal,
    id = props.id,
    v = version,
    iv = ++imageVersion
  imageLoading.value = true
  imageFailure.value = ''
  if (!keepOld) clearImage()
  try {
    const r = await http.get<Blob>(`/award-image/one/${id}`, { responseType: 'blob', signal })
    if (signal.aborted || v !== version || iv !== imageVersion || id !== props.id) return
    const next = URL.createObjectURL(r.data)
    clearImage()
    url.value = next
  } catch (e) {
    if (!isCancelled(e) && v === version && iv === imageVersion) imageFailure.value = message(e)
  } finally {
    if (v === version && iv === imageVersion) imageLoading.value = false
  }
}
async function load() {
  cancel()
  const v = version,
    id = props.id
  controller = new AbortController()
  record.value = undefined
  error.value = ''
  replaceError.value = ''
  imageFailure.value = ''
  loading.value = true
  try {
    const data = await get<Entity | null>(`/award-record/one/${id}`, controller.signal)
    if (v !== version) return
    record.value = data
    if (data) await fetchImage()
  } catch (e) {
    if (!isCancelled(e) && v === version) error.value = message(e)
  } finally {
    if (v === version) loading.value = false
  }
}
async function replace(e: Event) {
  const input = e.target as HTMLInputElement,
    f = input.files?.[0]
  input.value = ''
  replaceError.value = imageError(f)
  if (replaceError.value || replacing.value) return
  replacing.value = true
  const id = props.id,
    v = version
  try {
    const data = new FormData()
    data.append('file', f!)
    await send('put', `/award-image/upload/${id}`, data)
    if (v === version && id === props.id) {
      await fetchImage(true)
      const latest = await get<Entity>(`/award-record/one/${id}`)
      if (v === version && id === props.id) {
        record.value = latest
        emit('changed')
      }
    }
  } catch (e) {
    if (v === version) replaceError.value = message(e)
  } finally {
    replacing.value = false
  }
}
watch(() => props.id, load, { immediate: true })
onBeforeUnmount(cancel)
</script>
<template>
  <el-dialog
    :model-value="true"
    title="奖状详情"
    width="820px"
    :close-on-click-modal="false"
    :before-close="
      () => {
        if (!replacing) emit('close')
      }
    "
    ><div v-if="loading && !record" class="loading-state">正在读取奖状资料…</div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-empty
      v-if="record === null"
      description="奖状不存在或不可访问"
    /><template v-if="record"
      ><div class="detail-heading">
        <el-tag>{{ record.awardType === 'CLASS' ? '班级奖状' : '个人奖状' }}</el-tag>
        <h2 data-testid="detail-title">{{ record.awardName }}</h2>
        <p class="muted">
          {{ className(record.classId) }} ·
          {{ record.awardType === 'PERSONAL' ? userName(record.studentUserId) : '集体荣誉' }}
        </p>
      </div>
      <dl class="details">
        <template v-for="[key, label] in awardFields.filter(([k]) => k !== 'awardName')" :key="key"
          ><dt>{{ label }}</dt>
          <dd>{{ record[key] || '—' }}</dd></template
        >
      </dl>
      <section class="image-section">
        <div class="section-title">
          <h3>奖状原图</h3>
          <el-button :loading="imageLoading" @click="fetchImage(true)">重新加载图片</el-button>
        </div>
        <p v-if="imageLoading" role="status">正在加载图片…</p>
        <el-alert v-if="imageFailure" :title="imageFailure" type="error" :closable="false" /><img
          v-if="url"
          :src="url"
          :alt="record.awardName + '的奖状图片'"
          data-testid="award-image"
          class="award-image"
        />
        <div v-if="editable" class="replace-image">
          <label for="replace-file">{{
            replacing ? '正在替换图片…' : '替换图片（JPEG / PNG / WebP，≤5MiB）'
          }}</label
          ><input
            id="replace-file"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            :disabled="replacing"
            @change="replace"
          /><el-alert v-if="replaceError" :title="replaceError" type="error" :closable="false" />
        </div></section></template
    ><template #footer
      ><el-button v-if="nextId" :disabled="replacing" @click="emit('next', nextId)"
        >下一张奖状</el-button
      ><el-button :disabled="replacing" @click="emit('close')">关闭详情</el-button
      ><el-button
        v-if="record && editable"
        type="primary"
        :disabled="replacing"
        @click="emit('edit', record)"
        >修改文字</el-button
      ></template
    ></el-dialog
  >
</template>
