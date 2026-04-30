import express from 'express';
import * as tmdb from '../services/tmdb.js';
import { getMovieStream } from '../services/media.js';

const router = express.Router();

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

router.get('/:id/play', async (req, res) => {
  try {
    const { id } = req.params;
    const movie = await tmdb.getMovieDetails(id);
    const streamUrl = await getMovieStream(movie.title);
    if (streamUrl) {
      res.json({ url: streamUrl, title: movie.title });
    } else {
      res.status(404).json({ error: 'Stream no encontrado', message: 'No se pudo encontrar un stream para esta película' });
    }
  } catch (error) {
    res.status(500).json({ error: 'Error playing movie', message: error.message });
  }
});

export default router;