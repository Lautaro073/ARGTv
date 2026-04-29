import express from 'express';
import { getStreamUrl } from '../services/tvtvhd.js';

const router = express.Router();

router.get('/:slug', async (req, res) => {
  try {
    const streamUrl = await getStreamUrl(req.params.slug);
    res.json({ url: streamUrl });
  } catch (error) {
    res.status(500).json({ error: 'Stream no disponible', message: error.message });
  }
});

export default router;