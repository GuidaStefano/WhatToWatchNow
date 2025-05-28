import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getMovies } from '../services/api'; // Assuming api.js is in src/services

const MovieCard = ({ movie }) => (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow duration-300">
        <Link to={`/movie/${movie.id}`}>
            <img 
                src={movie.posterUrl || 'https://via.placeholder.com/300x450.png?text=No+Poster'} 
                alt={`${movie.title} Poster`}
                className="w-full h-64 object-cover" // Adjust height as needed
            />
        </Link>
        <div className="p-4">
            <h3 className="text-lg font-semibold mb-1">
                <Link to={`/movie/${movie.id}`} className="hover:text-blue-600">{movie.title}</Link>
            </h3>
            <p className="text-sm text-gray-600 mb-1">Release Year: {movie.releaseYear || 'N/A'}</p>
            <p className="text-sm text-gray-600 mb-2">Genres: {movie.genres && movie.genres.length > 0 ? movie.genres.join(', ') : 'N/A'}</p>
            {/* <p className="text-xs text-gray-500 truncate">{movie.description || ''}</p> */}
        </div>
    </div>
);

const HomePage = () => {
    const [movies, setMovies] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    
    const [searchTerm, setSearchTerm] = useState('');
    const [genreFilter, setGenreFilter] = useState('');
    const [yearFilter, setYearFilter] = useState('');
    // Add more filters if needed (e.g., actor)

    // Define available genres and years (can be dynamic in a real app)
    const genres = ["Action", "Comedy", "Drama", "Sci-Fi", "Horror", "Romance", "Thriller"]; // Example genres
    const currentYear = new Date().getFullYear();
    const years = Array.from({ length: 30 }, (_, i) => currentYear - i); // Last 30 years

    const fetchMovies = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const filters = {
                query: searchTerm, // Corresponds to 'query' in backend (title or description)
                genre: genreFilter,
                year: yearFilter ? parseInt(yearFilter) : null,
            };
            const response = await getMovies(filters);
            setMovies(response.data || []); // Assuming response.data is the array of movies
        } catch (err) {
            setError(`Failed to fetch movies: ${err.message}`);
            console.error("Fetch movies error:", err);
        } finally {
            setLoading(false);
        }
    }, [searchTerm, genreFilter, yearFilter]);

    useEffect(() => {
        fetchMovies();
    }, [fetchMovies]); // Re-fetch when fetchMovies (due to filter changes)

    const handleSearch = (e) => {
        e.preventDefault();
        fetchMovies(); // Filters are already updated by onChange handlers
    };
    
    const handleResetFilters = () => {
        setSearchTerm('');
        setGenreFilter('');
        setYearFilter('');
        // fetchMovies will be called by useEffect due to state changes if fetchMovies is in dependency array
        // or call fetchMovies() explicitly if not using it in dependency array for this specific case.
    };


    return (
        <div className="container mx-auto p-4">
            <h1 className="text-3xl font-bold text-center mb-8 text-gray-800">Discover Movies</h1>

            {/* Filter and Search Section */}
            <form onSubmit={handleSearch} className="mb-8 p-6 bg-gray-50 rounded-lg shadow">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
                    <div>
                        <label htmlFor="searchTerm" className="block text-sm font-medium text-gray-700">Search Title/Desc</label>
                        <input
                            type="text"
                            id="searchTerm"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                            placeholder="e.g., Inception, space..."
                        />
                    </div>
                    <div>
                        <label htmlFor="genreFilter" className="block text-sm font-medium text-gray-700">Genre</label>
                        <select
                            id="genreFilter"
                            value={genreFilter}
                            onChange={(e) => setGenreFilter(e.target.value)}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                        >
                            <option value="">All Genres</option>
                            {genres.map(g => <option key={g} value={g}>{g}</option>)}
                        </select>
                    </div>
                    <div>
                        <label htmlFor="yearFilter" className="block text-sm font-medium text-gray-700">Release Year</label>
                        <select
                            id="yearFilter"
                            value={yearFilter}
                            onChange={(e) => setYearFilter(e.target.value)}
                            className="mt-1 block w-full px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                        >
                            <option value="">All Years</option>
                            {years.map(y => <option key={y} value={y}>{y}</option>)}
                        </select>
                    </div>
                    <div className="flex space-x-2">
                        <button
                            type="submit"
                            className="w-full py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                        >
                            Search
                        </button>
                         <button
                            type="button"
                            onClick={handleResetFilters}
                            className="w-full py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
                        >
                            Reset
                        </button>
                    </div>
                </div>
            </form>

            {loading && <p className="text-center text-gray-600">Loading movies...</p>}
            {error && <p className="text-center text-red-500 bg-red-100 p-3 rounded-md">{error}</p>}
            
            {!loading && !error && movies.length === 0 && (
                <p className="text-center text-gray-600">No movies found. Try adjusting your filters.</p>
            )}

            {!loading && !error && movies.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
                    {movies.map(movie => (
                        <MovieCard key={movie.id} movie={movie} />
                    ))}
                </div>
            )}
        </div>
    );
};

export default HomePage;
