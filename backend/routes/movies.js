import express from 'express';
import * as tmdb from '../services/tmdb.js';

const router = express.Router();

const VIDSRC_BASE = "https://vidplay.site";

router.get('/', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const movies = await tmdb.getPopularMovies(page);
    res.json(movies);
  } catch (error) {
    res.status(500).json({ error: 'Error fetching movies', message: error.message });
  }
});

router.get('/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.status(400).json({ error: 'Query required' });
    const movies = await tmdb.searchMovies(q);
    res.json(movies);
  } catch (error) {
    res.status(500).json({ error: 'Error searching movies', message: error.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const movie = await tmdb.getMovieDetails(req.params.id);
    res.json(movie);
  } catch (error) {
    res.status(500).json({ error: 'Error fetching movie', message: error.message });
  }
});

// VidSrc embed URL para WebView
router.get('/:id/embed', async (req, res) => {
  try {
    const { id } = req.params;
    const embedUrl = `${VIDSRC_BASE}/embed/movie/${id}`;
    res.json({ url: embedUrl, type: 'movie' });
  } catch (error) {
    res.status(500).json({ error: 'Error getting embed', message: error.message });
  }
});

export default router;