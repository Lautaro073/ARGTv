import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: path.join(__dirname, '.env') });

const app = express();
const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';

app.use(cors());
app.use(express.json());

// Routes
import healthRoutes from './routes/health.js';
import channelsRoutes from './routes/channels.js';
import streamsRoutes from './routes/streams.js';
import moviesRoutes from './routes/movies.js';
import seriesRoutes from './routes/series.js';

app.use('/', healthRoutes);
app.use('/api/channels', channelsRoutes);
app.use('/api/streams', streamsRoutes);
app.use('/api/movies', moviesRoutes);
app.use('/api/series', seriesRoutes);

app.listen(PORT, HOST, () => {
  console.log(`ARGtv Backend running on http://${HOST}:${PORT}`);
});

export default app;