import axios from 'axios';

const BASE_URL = 'https://tvtvhd.com';

const HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Referer': BASE_URL + '/'
};

const CACHE = new Map();
const CACHE_TTL = 1000 * 60 * 30; // 30 minutes

export function clearCache() {
  CACHE.clear();
}

export async function getStreamUrl(slug) {
  const cached = CACHE.get(slug);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.url;
  }

  try {
    const url = `${BASE_URL}/vivo/canales.php?stream=${slug}`;
    const response = await axios.get(url, { headers: HEADERS, timeout: 10000 });
    
    let streamUrl = null;
    
    // Pattern 1: playbackURL = "..."
    const match1 = response.data.match(/playbackURL\s*[=:]\s*["']([^"'\s]+m3u8[^"'\s]*)["']/);
    if (match1) streamUrl = match1[1];
    
    // Pattern 2: <source src="...">
    if (!streamUrl) {
      const match2 = response.data.match(/<source[^>]+src=["']([^"']+\.m3u8[^"']*)["']/);
      if (match2) streamUrl = match2[1];
    }
    
    // Pattern 3: file: "..."
    if (!streamUrl) {
      const match3 = response.data.match(/file:\s*["']([^"']+\.m3u8[^"']*)["']/);
      if (match3) streamUrl = match3[1];
    }

    if (streamUrl) {
      CACHE.set(slug, { url: streamUrl, timestamp: Date.now() });
      return streamUrl;
    }
    
    throw new Error('Stream URL no encontrada');
  } catch (error) {
    console.error(`Error getting stream for ${slug}:`, error.message);
    throw error;
  }
}