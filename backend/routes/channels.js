import express from 'express';
import channels from '../data/channels.json' with { type: 'json' };
import channelsLocalTv from '../data/channels_localtv.json' with { type: 'json' };

const router = express.Router();

// Merge: use localTv channels data
const allChannels = channelsLocalTv.map(ch => ({
  name: ch.name,
  slug: ch.slug,
  logo: ch.logo_url || "",
  category: ch.category_id == 1 ? "deportes" : "general"
})).filter(ch => ch.name && ch.slug);

router.get('/', (req, res) => {
  res.json(allChannels);
});

router.get('/:slug', (req, res) => {
  const channel = allChannels.find(c => c.slug === req.params.slug);
  if (!channel) {
    return res.status(404).json({ error: 'Canal no encontrado' });
  }
  res.json(channel);
});

export default router;