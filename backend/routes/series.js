import express from 'express';
import axios from 'axios';
import * as tmdb from '../services/tmdb.js';

const router = express.Router();

const CINEPRO_BASE = "https://argtv.onrender.com";

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

// CinePro stream para series
router.get('/:id/stream', async (req, res) => {
  try {
    const { id } = req.params;
    const season = req.query.season || "1";
    const episode = req.query.episode || "1";
    
    const response = await axios.get(`${CINEPRO_BASE}/stream/tv/${id}/${season}/${episode}`, {
      timeout: 30000
    });
    
    const sources = response.data.sources || [];
    
    const bestSource = sources
      .filter(s => s.type === 'hls')
      .sort((a, b) => {
        const qualA = parseInt(a.quality?.replace('p', '') || '0');
        const qualB = parseInt(b.quality?.replace('p', '') || '0');
        return qualB - qualA;
      })[0];
    
    if (bestSource) {
      res.json({
        url: bestSource.url,
        quality: bestSource.quality,
        headers: bestSource.headers
      });
    } else {
      res.status(404).json({ error: 'No stream available' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Stream error', message: error.message });
  }
});

export default router;