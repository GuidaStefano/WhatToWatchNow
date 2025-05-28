import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom'; // Added Link
import { getMovieById, getReviewsForMovie, addReview, deleteReview } from '../services/api'; // Removed getUserProfile
import { useAuth } from '../context/AuthContext'; // Import useAuth

const MovieDetailsPage = () => {
    const { id: movieId } = useParams();
    const navigate = useNavigate(); // Still used for navigation if needed, but not for auth redirect here.
    const { isAuthenticated, currentUser, isLoading: isAuthLoading } = useAuth(); // Use context for auth state

    const [movie, setMovie] = useState(null);
    const [reviews, setReviews] = useState([]);
    const [isLoadingMovie, setIsLoadingMovie] = useState(true);
    const [isLoadingReviews, setIsLoadingReviews] = useState(true);
    const [error, setError] = useState('');

    // For new review form
    const [reviewRating, setReviewRating] = useState(0);
    const [reviewComment, setReviewComment] = useState('');
    const [isSubmittingReview, setIsSubmittingReview] = useState(false);
    const [reviewError, setReviewError] = useState('');


    const fetchMovieData = useCallback(async () => {
        setIsLoadingMovie(true);
        try {
            const movieResponse = await getMovieById(movieId);
            setMovie(movieResponse.data);
        } catch (err) {
            console.error(`Error fetching movie ${movieId}:`, err);
            setError(`Failed to load movie details: ${err.message}`);
        } finally {
            setIsLoadingMovie(false);
        }
    }, [movieId]);

    const fetchReviewsData = useCallback(async () => {
        setIsLoadingReviews(true);
        try {
            const reviewsResponse = await getReviewsForMovie(movieId);
            setReviews(reviewsResponse.data.sort((a, b) => new Date(b.reviewDate) - new Date(a.reviewDate)));
        } catch (err) {
            console.error(`Error fetching reviews for movie ${movieId}:`, err);
            // setError(`Failed to load reviews: ${err.message}`); // Don't overwrite movie error
        } finally {
            setIsLoadingReviews(false);
        }
    }, [movieId]);

    useEffect(() => {
        // fetchCurrentUser is no longer needed here, AuthContext handles it.
        fetchMovieData();
        fetchReviewsData();
    }, [fetchMovieData, fetchReviewsData]); // Removed fetchCurrentUser
    
    // Handle new review submission
    const handleReviewSubmit = async (e) => {
        e.preventDefault();
        // Auth check using context
        if (!isAuthenticated || !currentUser) { 
            setReviewError("You must be logged in to submit a review. Please login first.");
            // Optionally, redirect to login: navigate('/login', { state: { from: location } });
            return;
        }
        if (reviewRating < 1 || reviewRating > 5) { // Assuming 0 is placeholder
            setReviewError("Please select a valid rating (1-5 stars).");
            return;
        }
        if (!reviewComment.trim()) {
            setReviewError("Please enter a comment for your review.");
            return;
        }

        setIsSubmittingReview(true);
        setReviewError('');
        try {
            await addReview(movieId, { rating: reviewRating, comment: reviewComment });
            setReviewRating(0);
            setReviewComment('');
            fetchReviewsData(); // Refresh reviews list
        } catch (err) {
            setReviewError(`Failed to submit review: ${err.response?.data?.message || err.message}`);
            console.error("Submit review error:", err);
        } finally {
            setIsSubmittingReview(false);
        }
    };

    const handleReviewDelete = async (reviewId) => {
        // Auth check using context
        if (!isAuthenticated || !currentUser) { 
            alert("Authentication error. Please ensure you are logged in.");
            return;
        }
        if (!window.confirm("Are you sure you want to delete this review? This action cannot be undone.")) {
            return;
        }

        try {
            await deleteReview(reviewId);
            fetchReviewsData(); // Refresh reviews list
        } catch (err) {
            alert(`Failed to delete review: ${err.response?.data?.message || err.message}`);
            console.error("Delete review error:", err);
        }
    };


    // Handle auth loading state
    if (isAuthLoading || isLoadingMovie) { // Check isAuthLoading from context
        return <p className="text-center text-lg mt-8">Loading movie details...</p>;
    }
    if (error) return <p className="text-center text-red-500 text-lg mt-8 bg-red-100 p-4 rounded-md">{error}</p>;
    if (!movie) return <p className="text-center text-lg mt-8">Movie not found. It might have been removed or the ID is incorrect.</p>;

    return (
        <div className="container mx-auto p-4">
            {/* Movie Details Section */}
            <div className="bg-white shadow-xl rounded-lg overflow-hidden md:flex">
                <img 
                    src={movie.posterUrl || 'https://via.placeholder.com/400x600.png?text=No+Poster'} 
                    alt={`${movie.title} Poster`}
                    className="w-full md:w-1/3 h-auto object-cover" // Adjust width as needed
                />
                <div className="p-6 md:w-2/3">
                    <h1 className="text-3xl md:text-4xl font-bold mb-3 text-gray-800">{movie.title} ({movie.releaseYear})</h1>
                    <p className="text-gray-600 mb-4 text-sm"><strong>Genres:</strong> {movie.genres?.join(', ') || 'N/A'}</p>
                    <p className="text-gray-600 mb-4 text-sm"><strong>Actors:</strong> {movie.actors?.join(', ') || 'N/A'}</p>
                    <p className="text-gray-700 leading-relaxed mb-6">{movie.description || 'No description available.'}</p>
                    {/* Add more details like director, runtime etc. if available */}
                </div>
            </div>

            {/* Reviews Section */}
            <div className="mt-10">
                <h2 className="text-2xl font-semibold mb-6 text-gray-700">User Reviews</h2>

                {/* Add Review Form (only if authenticated) */}
                {isAuthenticated && currentUser ? ( // Use context's isAuthenticated and currentUser
                    <form onSubmit={handleReviewSubmit} className="mb-8 p-6 bg-gray-50 rounded-lg shadow-md">
                        <h3 className="text-xl font-semibold mb-4 text-gray-800">Leave a Review</h3>
                        {reviewError && <p className="text-red-600 text-sm mb-3 p-2 bg-red-100 rounded">{reviewError}</p>}
                        <div className="mb-4">
                            <label htmlFor="rating" className="block text-sm font-medium text-gray-700 mb-1">Your Rating</label>
                            <select 
                                id="rating" 
                                value={reviewRating} 
                                onChange={(e) => setReviewRating(parseInt(e.target.value))}
                                className="mt-1 block w-full max-w-xs px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                required
                            >
                                <option value="0" disabled>Choose a rating...</option>
                                {[1, 2, 3, 4, 5].map(r => <option key={r} value={r}>{r} Star{r > 1 ? 's' : ''}</option>)}
                            </select>
                        </div>
                        <div className="mb-4">
                            <label htmlFor="comment" className="block text-sm font-medium text-gray-700 mb-1">Comment</label>
                            <textarea
                                id="comment"
                                value={reviewComment}
                                onChange={(e) => setReviewComment(e.target.value)}
                                rows="4"
                                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                placeholder="Share your thoughts about the movie..."
                                required
                            ></textarea>
                        </div>
                        <button
                            type="submit"
                            disabled={isSubmittingReview}
                            className="py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:bg-gray-400"
                        >
                            {isSubmittingReview ? 'Submitting...' : 'Submit Review'}
                        </button>
                    </form>
                )}
                 {!isAuthenticated && !isAuthLoading && ( // Show login prompt if not authenticated and auth check is complete
                    <p className="mb-8 p-4 bg-blue-50 text-blue-700 rounded-md shadow">
                        Please <Link to={`/login?redirect=/movie/${movieId}`} className="font-semibold underline hover:text-blue-800">login</Link> to leave a review.
                    </p>
                )}


                {isLoadingReviews ? (
                    <p className="text-gray-600">Loading reviews...</p>
                ) : reviews.length > 0 ? (
                    <div className="space-y-6">
                        {reviews.map(review => (
                            <div key={review.id} className="bg-white p-5 shadow rounded-lg">
                                <div className="flex justify-between items-start">
                                    <div>
                                        <p className="font-semibold text-gray-800">Rating: {review.rating}/5</p>
                                        {/* TODO: Replace review.userId with user's nickname if available */}
                                        {/* <p className="text-sm text-gray-500">By: User {review.userId.substring(0,8)}...</p> */}
                                    </div>
                                    {/* Use context's isAuthenticated and currentUser */}
                                    {isAuthenticated && currentUser && currentUser.id === review.userId && ( 
                                        <button 
                                            onClick={() => handleReviewDelete(review.id)}
                                            className="text-xs text-red-600 hover:text-red-800 font-semibold py-1 px-2 rounded-md border border-red-300 hover:bg-red-50 transition-colors"
                                            aria-label="Delete your review"
                                        >
                                            Delete My Review
                                        </button>
                                    )}
                                </div>
                                <p className="text-gray-700 mt-2 mb-1">{review.comment}</p>
                                <p className="text-xs text-gray-400">
                                    Reviewed on: {new Date(review.reviewDate).toLocaleDateString()}
                                </p>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="text-gray-600">No reviews yet for this movie.</p>
                )}
            </div>
        </div>
    );
};

export default MovieDetailsPage;
