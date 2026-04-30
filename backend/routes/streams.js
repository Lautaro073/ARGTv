import express from 'express';
import { getStreamUrl } from '../services/tvtvhd.js';

const router = express.Router();

const STREAM_HEADERS = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Referer': 'https://tvtvhd.com/',
};

router.get('/:slug', async (req, res) => {
  try {
    const streamUrl = await getStreamUrl(req.params.slug);
    res.json({ 
      url: streamUrl,
      headers: STREAM_HEADERS
    });
  } catch (error) {
    res.status(500).json({ error: 'Stream no disponible', message: error.message });
  }
});

export default router;