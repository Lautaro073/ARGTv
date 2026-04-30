import axios from 'axios';

const CACHE = new Map();
const CACHE_TTL = 1000 * 60 * 60; // 1 hour

async function searchMediaStream(title, type = 'movie') {
  const cacheKey = `${type}:${title}`;
  const cached = CACHE.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.url;
  }

  const headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml',
  };

  // Try multiple sources
  const sources = [
    `https://www.cuevana3.ai/search?q=${encodeURIComponent(title)}`,
  ];

  for (const source of sources) {
    try {
      const response = await axios.get(source, { headers, timeout: 10000 });
      const html = response.data;
      
      // Look for iframe or video URLs
      const patterns = [
        /src=["']([^"']*\.m3u8[^"']*)["']/,
        /src=["']([^"']*\.mp4[^"']*)["']/,
        /file:\s*["']([^"']+\.m3u8[^"']*)["']/,
        /file:\s*["']([^"']+\.mp4[^"']*)["']/,
      ];

      for (const pattern of patterns) {
        const match = html.match(pattern);
        if (match && match[1]) {
          const streamUrl = match[1];
          if (streamUrl.includes('m3u8') || streamUrl.includes('mp4')) {
            CACHE.set(cacheKey, { url: streamUrl, timestamp: Date.now() });
            return streamUrl;
          }
        }
      }
    } catch (e) {
      console.log(`Source failed: ${source} - ${e.message}`);
    }
  }

  return null;
}

export async function getMovieStream(title) {
  return searchMediaStream(title, 'movie');
}

export async function getSeriesStream(title) {
  return searchMediaStream(title, 'series');
}