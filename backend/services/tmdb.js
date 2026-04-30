import axios from 'axios';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.join(__dirname, '.env') });

const TMDB_BASE = 'https://api.themoviedb.org/3';
const IMAGE_BASE = 'https://image.tmdb.org/t/p/w500';
const API_KEY = process.env.TMDB_API_KEY || 'fe5aa75be4a65a41051c36068f3d2d50';

console.log('TMDB_API_KEY loaded:', API_KEY ? 'YES' : 'NO');

const mapMovie = (tmdb) => ({
  id: tmdb.id,
  title: tmdb.title,
  originalTitle: tmdb.original_title,
  overview: tmdb.overview,
  poster: tmdb.poster_path ? IMAGE_BASE + tmdb.poster_path : '',
  backdrop: tmdb.backdrop_path ? IMAGE_BASE + tmdb.backdrop_path : '',
  year: tmdb.release_date?.split('-')[0] || '',
  rating: tmdb.vote_average,
  type: 'movie'
});

const mapSeries = (tmdb) => ({
  id: tmdb.id,
  title: tmdb.name,
  originalTitle: tmdb.original_name,
  overview: tmdb.overview,
  poster: tmdb.poster_path ? IMAGE_BASE + tmdb.poster_path : '',
  backdrop: tmdb.backdrop_path ? IMAGE_BASE + tmdb.backdrop_path : '',
  firstAirDate: tmdb.first_air_date,
  rating: tmdb.vote_average,
  type: 'serie'
});

export async function getPopularMovies(page = 1) {
  const response = await axios.get(`${TMDB_BASE}/movie/popular`, {
    params: { api_key: API_KEY, language: 'es-419', page }
  });
  return response.data.results.map(mapMovie);
}

export async function getMovieDetails(id) {
  const response = await axios.get(`${TMDB_BASE}/movie/${id}`, {
    params: { api_key: API_KEY, language: 'es-419' }
  });
  return mapMovie(response.data);
}

export async function searchMovies(query, page = 1) {
  const response = await axios.get(`${TMDB_BASE}/search/movie`, {
    params: { api_key: API_KEY, language: 'es-419', query, page }
  });
  return response.data.results.map(mapMovie);
}

export async function getPopularSeries(page = 1) {
  const response = await axios.get(`${TMDB_BASE}/tv/popular`, {
    params: { api_key: API_KEY, language: 'es-419', page }
  });
  return response.data.results.map(mapSeries);
}

export async function getSeriesDetails(id) {
  const response = await axios.get(`${TMDB_BASE}/tv/${id}`, {
    params: { api_key: API_KEY, language: 'es-419' }
  });
  return mapSeries(response.data);
}

export async function getSeriesEpisodes(id, seasonNumber) {
  const response = await axios.get(`${TMDB_BASE}/tv/${id}/season/${seasonNumber}`, {
    params: { api_key: API_KEY, language: 'es-419' }
  });
  return response.data.episodes.map(ep => ({
    episodeNumber: ep.episode_number,
    title: ep.name,
    overview: ep.overview,
    still: ep.still_path ? IMAGE_BASE + ep.still_path : '',
    airDate: ep.air_date
  }));
}

export async function searchSeries(query, page = 1) {
  const response = await axios.get(`${TMDB_BASE}/search/tv`, {
    params: { api_key: API_KEY, language: 'es-419', query, page }
  });
  return response.data.results.map(mapSeries);
}