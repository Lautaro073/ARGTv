import express from 'express';
import axios from 'axios';
import { getStreamUrl, clearCache } from '../services/tvtvhd.js';

const router = express.Router();

const STREAM_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Referer': 'https://tvtvhd.com/',
};

router.get('/:slug', async (req, res) => {
  try {
    // Always get fresh URL (no cache for streams)
    clearCache();
    const streamUrl = await getStreamUrl(req.params.slug);
    res.json({ 
      url: streamUrl,
      headers: STREAM_HEADERS
    });
  } catch (error) {
    res.status(500).json({ error: 'Stream no disponible', message: error.message });
  }
});

// Proxy endpoint - streams through backend to avoid CORS
router.get('/:slug/proxy', async (req, res) => {
  try {
    clearCache();
    const streamUrl = await getStreamUrl(req.params.slug);
    
    // Fetch and forward the stream
    const response = await axios.get(streamUrl, {
      headers: STREAM_HEADERS,
      responseType: 'stream',
      timeout: 30000
    });
    
    res.setHeader('Content-Type', response.headers['content-type'] || 'application/vnd.apple.mpegurl');
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    response.data.pipe(res);
  } catch (error) {
    res.status(500).json({ error: 'Stream proxy error', message: error.message });
  }
});

export default router;