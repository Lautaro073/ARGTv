import express from 'express';
import * as tmdb from '../services/tmdb.js';

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const series = await tmdb.getPopularSeries(page);
    res.json(series);
  } catch (error) {
    res.status(500).json({ error: 'Error fetching series', message: error.message });
  }
});

router.get('/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Query required' });
    const series = await tmdb.searchSeries(q);
    res.json(series);
  } catch (error) {
    res.status(500).json({ error: 'Error searching series', message: error.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const series = await tmdb.getSeriesDetails(req.params.id);
    res.json(series);
  } catch (error) {
    res.status(500).json({ error: 'Error fetching series', message: error.message });
  }
});

router.get('/:id/season/:season', async (req, res) => {
  try {
    const episodes = await tmdb.getSeriesEpisodes(req.params.id, req.params.season);
    res.json({ season: req.params.season, episodes });
  } catch (error) {
    res.status(500).json({ error: 'Error fetching episodes', message: error.message });
  }
});

router.get('/:id/:season/:episode/play', async (req, res) => {
  res.status(501).json({ error: 'Playback no implementado', message: 'Necesita scraping de embed URLs' });
});

export default router;