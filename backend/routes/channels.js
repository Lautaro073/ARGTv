import express from 'express';
import channels from '../data/channels.json' with { type: 'json' };

const router = express.Router();

router.get('/', (req, res) => {
  res.json(channels);
});

router.get('/:slug', (req, res) => {
  const channel = channels.find(c => c.slug === req.params.slug);
  if (!channel) {
    return res.status(404).json({ error: 'Canal no encontrado' });
  }
  res.json(channel);
});

export default router;