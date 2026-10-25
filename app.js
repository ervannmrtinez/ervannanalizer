const $ = (id) => document.getElementById(id);
const baseInput = $("apiBase"), tokenInput = $("apiToken"), statusEl = $("status");
function settings() {
  return { base: baseInput.value.trim().replace(/\/+$/, ""), token: tokenInput.value.trim() };
}
$("saveSettings").addEventListener("click", () => {
  statusEl.textContent = "Pengaturan dipakai untuk sesi halaman ini. Untuk keamanan, token tidak disimpan permanen.";
});
$("refresh").addEventListener("click", loadDevices);

async function loadDevices() {
  const {base, token} = settings();
  if (!base || !token) { statusEl.textContent = "URL API dan token wajib diisi."; return; }
  statusEl.textContent = "Mengambil data perangkat…";
  try {
    const res = await fetch(`${base}/api/devices`, {headers: {Authorization: `Bearer ${token}`}});
    if (!res.ok) throw new Error(res.status === 401 ? "Token tidak valid." : `API error (${res.status}).`);
    const devices = await res.json();
    const tbody = $("devices");
    tbody.replaceChildren();
    $("count").textContent = `${devices.length} perangkat`;
    if (!devices.length) {
      const tr = document.createElement("tr"), td = document.createElement("td");
      td.colSpan = 5; td.className = "empty"; td.textContent = "Belum ada data perangkat.";
      tr.append(td); tbody.append(tr);
    }
    for (const d of devices) {
      const tr = document.createElement("tr");
      const values = [
        d.device_name || d.device_id,
        Number.isFinite(d.latitude) && Number.isFinite(d.longitude)
          ? `${d.latitude.toFixed(5)}, ${d.longitude.toFixed(5)}` : "Belum tersedia",
        d.accuracy == null ? "—" : `±${Math.round(d.accuracy)} m`,
        d.battery_pct == null ? "—" : `${d.battery_pct}%`,
        d.last_seen ? new Date(d.last_seen).toLocaleString() : "—"
      ];
      values.forEach((value, i) => {
        const td = document.createElement("td");
        if (i === 1 && Number.isFinite(d.latitude) && Number.isFinite(d.longitude)) {
          const a = document.createElement("a");
          a.href = `https://maps.google.com/?q=${encodeURIComponent(`${d.latitude},${d.longitude}`)}`;
          a.target = "_blank"; a.rel = "noopener noreferrer"; a.textContent = value;
          td.append(a);
        } else td.textContent = value;
        tr.append(td);
      });
      tbody.append(tr);
    }
    statusEl.textContent = "Data berhasil dimuat.";
  } catch (err) {
    statusEl.textContent = `Gagal memuat perangkat: ${err.message} Pastikan API aktif dan CORS dikonfigurasi bila website beda origin.`;
  }
}
