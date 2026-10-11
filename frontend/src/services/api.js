// All backend calls live here. Paths are relative (/api/...) so they work with the Vite proxy
// in development and with the nginx proxy in Docker.
const BASE = '/api';

export class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(BASE + path, options);
  } catch {
    throw new ApiError('Cannot reach the backend. Is it running?', 0);
  }
  if (!res.ok) {
    let message = `Request failed (HTTP ${res.status})`;
    try {
      const body = await res.json();
      if (body && body.message) message = body.message;
    } catch {
      /* body was not JSON */
    }
    throw new ApiError(message, res.status);
  }
  return res.json();
}

export const api = {
  stats: () => request('/dashboard/stats'),
  listFiles: (search = '') =>
    request(`/files${search.trim() ? `?search=${encodeURIComponent(search.trim())}` : ''}`),
  getFile: (id) => request(`/files/${id}`),
  verifyFile: (id) => request(`/files/${id}/verify`, { method: 'POST' }),
  deleteFile: (id) => request(`/files/${id}`, { method: 'DELETE' }),
  listNodes: () => request('/nodes'),
  setNodeOnline: (id, online) => request(`/nodes/${id}/${online ? 'online' : 'offline'}`, { method: 'POST' }),
  downloadUrl: (id) => `${BASE}/files/${id}/download`,
};

/** Uploads one file with XHR so the UI can show real upload progress. */
export function uploadFile(file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open('POST', `${BASE}/files/upload`);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100));
    };
    xhr.onload = () => {
      let body = {};
      try {
        body = JSON.parse(xhr.responseText);
      } catch {
        /* not JSON */
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(body);
      } else {
        reject(new ApiError(body.message || `Upload failed (HTTP ${xhr.status})`, xhr.status));
      }
    };
    xhr.onerror = () => reject(new ApiError('Network error during upload. Is the backend running?', 0));
    const form = new FormData();
    form.append('file', file);
    xhr.send(form);
  });
}
