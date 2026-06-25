<template>
  <div class="map-wrapper" :style="{ height: height + 'px' }">
    <div ref="mapContainer" class="map-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

// Fix default marker icons
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const props = defineProps({
  height: { type: Number, default: 350 },
  storeLat: { type: Number, default: 39.9087 },
  storeLng: { type: Number, default: 116.4605 },
  storeName: { type: String, default: '' },
  riderLat: { type: Number, default: null },
  riderLng: { type: Number, default: null },
  riderName: { type: String, default: '' },
  customerLat: { type: Number, default: null },
  customerLng: { type: Number, default: null },
  static: { type: Boolean, default: false },
})

const mapContainer = ref(null)
let map = null
let storeMarker = null
let riderMarker = null
let customerMarker = null
let routeLine = null

const riderIcon = L.divIcon({
  html: '<div style="background:#f56c6c;border:2px solid #fff;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;font-size:16px">🛵</div>',
  iconSize: [32, 32], iconAnchor: [16, 16]
})

const storeIcon = L.divIcon({
  html: '<div style="background:#FF8C00;border:2px solid #fff;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;font-size:16px">🏪</div>',
  iconSize: [32, 32], iconAnchor: [16, 16]
})

const customerIcon = L.divIcon({
  html: '<div style="background:#67c23a;border:2px solid #fff;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;font-size:16px">📍</div>',
  iconSize: [32, 32], iconAnchor: [16, 16]
})

function initMap() {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value).setView([props.storeLat, props.storeLng], 14)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap',
    maxZoom: 18,
  }).addTo(map)

  // Store marker
  storeMarker = L.marker([props.storeLat, props.storeLng], { icon: storeIcon })
    .addTo(map).bindPopup(props.storeName || '商家')

  // Customer marker
  if (props.customerLat && props.customerLng) {
    customerMarker = L.marker([props.customerLat, props.customerLng], { icon: customerIcon })
      .addTo(map).bindPopup('收货地址')
    // Draw route
    routeLine = L.polyline([[props.storeLat, props.storeLng], [props.customerLat, props.customerLng]], {
      color: '#FF8C00', weight: 3, dashArray: '8, 8'
    }).addTo(map)
    map.fitBounds([[props.storeLat, props.storeLng], [props.customerLat, props.customerLng]], { padding: [40, 40] })
  }
}

function updateRider(lat, lng, name) {
  if (!map) return
  if (riderMarker) {
    riderMarker.setLatLng([lat, lng])
  } else {
    riderMarker = L.marker([lat, lng], { icon: riderIcon })
      .addTo(map).bindPopup(name || '骑手')
  }
  // If rider has moved, update view
  if (!props.static) {
    map.panTo([lat, lng], { animate: true })
  }
}

watch(() => [props.riderLat, props.riderLng], ([lat, lng]) => {
  if (lat && lng) updateRider(lat, lng, props.riderName)
})

onMounted(() => {
  setTimeout(initMap, 100)
})

onBeforeUnmount(() => {
  if (map) { map.remove(); map = null }
})
</script>

<style scoped>
.map-wrapper { border-radius: 8px; overflow: hidden; border: 1px solid #EBE8E2; }
.map-container { width: 100%; height: 100%; }
</style>
