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

// Proxy con manejo de headers - evitar token expirecido
router.get('/:slug/proxy', async (req, res) => {
  try {
    clearCache();
    const streamUrl = await getStreamUrl(req.params.slug);
    
    const response = await axios.get(streamUrl, {
      headers: STREAM_HEADERS,
      responseType: 'stream',
      timeout: 30000,
      maxRedirects: 5
    });
    
    res.setHeader('Content-Type', 'application/x-mpegURL');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', '*');
    
    response.data.pipe(res);
  } catch (error) {
    console.error('Proxy error:', error.message);
    res.status(500).json({ error: 'Proxy error', message: error.message });
  }
});

// Obtener URLs frescas siempre
router.get('/:slug/fresh', async (req, res) => {
  try {
    clearCache();
    const streamUrl = await getStreamUrl(req.params.slug);
    res.json({ url: streamUrl });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

export default router;