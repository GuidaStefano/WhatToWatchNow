import axios from 'axios';

const API_BASE_URL = '/api'; // Adjust if your Spring Boot backend is on a different port during development (e.g., http://localhost:8080/api)

// Create an axios instance
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to add JWT token to requests
apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem('userToken');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// --- Authentication ---
export const login = async (email, password) => {
    // try {
    //     const response = await apiClient.post('/login', { email, password }); // Spring Security's default is often /login with form data
    //     // For JSON, ensure your Spring Security is configured for JSON authentication or use a custom endpoint
    //     // If using form data for Spring Security's default /login:
    //     const params = new URLSearchParams();
    //     params.append('username', email); // Spring Security default parameter names
    //     params.append('password', password);
    //     const response = await axios.post('/login', params, { // Use a separate axios instance for form login if needed
    //         baseURL: '/', // Adjust if Spring Boot is on a different port
    //         headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    //     });
    //     // Assuming successful login might not return data directly but sets a session cookie
    //     // or if it does return a token in a custom setup:
    //     // if (response.data.token) {
    //     //     localStorage.setItem('userToken', response.data.token);
    //     // }
    //     // For this placeholder, we'll assume it works and doesn't directly return a token in response body for /login
    //     return response; // Caller should handle response (e.g. check for success status/headers)
    // } catch (error) {
    //     console.error("Login error:", error.response || error.message);
    //     throw error;
    // }
    console.log('login called with:', email, password);
    // Placeholder: Spring Security's /login is tricky with SPA if not returning token directly.
    // Usually, a successful POST to /login will set an HTTP-only session cookie.
    // If a token is returned in the body (custom setup), then handle it here.
    // For now, this is a simplified placeholder.
    const params = new URLSearchParams();
    params.append('username', email); // 'username' is the default for Spring Security
    params.append('password', password);
    // Use a non-global axios instance for login to avoid sending JSON by default if Spring Security expects form data
    return axios.post('/login', params, { baseURL: window.location.origin, headers: { 'Content-Type': 'application/x-www-form-urlencoded'} });
};

export const register = async (userData) => {
    // userData: { nickname, email, password }
    // try {
    //     const response = await apiClient.post('/users/register', userData);
    //     return response.data;
    // } catch (error) {
    //     console.error("Registration error:", error.response || error.message);
    //     throw error;
    // }
    console.log('register called with:', userData);
    return apiClient.post('/users/register', userData);
};

// --- Movies ---
export const getMovies = async (filters = {}) => {
    // filters: { query, genre, year, actor }
    // try {
    //     const response = await apiClient.get('/movies', { params: filters });
    //     return response.data;
    // } catch (error) {
    //     console.error("Error fetching movies:", error.response || error.message);
    //     throw error;
    // }
    console.log('getMovies called with filters:', filters);
    return apiClient.get('/movies', { params: filters });
};

export const getMovieById = async (id) => {
    // try {
    //     const response = await apiClient.get(`/movies/${id}`);
    //     return response.data;
    // } catch (error) {
    //     console.error(`Error fetching movie ${id}:`, error.response || error.message);
    //     throw error;
    // }
    console.log('getMovieById called with id:', id);
    return apiClient.get(`/movies/${id}`);
};

export const addMovie = async (movieData) => {
    // movieData: { title, genres, releaseYear, actors, description, posterUrl }
    // try {
    //     const response = await apiClient.post('/movies', movieData);
    //     return response.data;
    // } catch (error) {
    //     console.error("Error adding movie:", error.response || error.message);
    //     throw error;
    // }
    console.log('addMovie called with:', movieData);
    return apiClient.post('/movies', movieData);
};

// --- Reviews ---
export const addReview = async (movieId, reviewData) => {
    // reviewData: { rating, comment }
    // try {
    //     const response = await apiClient.post(`/movies/${movieId}/reviews`, reviewData);
    //     return response.data;
    // } catch (error) {
    //     console.error(`Error adding review for movie ${movieId}:`, error.response || error.message);
    //     throw error;
    // }
    console.log('addReview called for movieId:', movieId, 'with data:', reviewData);
    return apiClient.post(`/movies/${movieId}/reviews`, reviewData);
};

export const getReviewsForMovie = async (movieId) => {
    // try {
    //     const response = await apiClient.get(`/movies/${movieId}/reviews`);
    //     return response.data;
    // } catch (error) {
    //     console.error(`Error fetching reviews for movie ${movieId}:`, error.response || error.message);
    //     throw error;
    // }
    console.log('getReviewsForMovie called for movieId:', movieId);
    return apiClient.get(`/movies/${movieId}/reviews`);
};

export const deleteReview = async (reviewId) => {
    // try {
    //     const response = await apiClient.delete(`/reviews/${reviewId}`);
    //     return response.data; // Or response.status if no body
    // } catch (error) {
    //     console.error(`Error deleting review ${reviewId}:`, error.response || error.message);
    //     throw error;
    // }
    console.log('deleteReview called for reviewId:', reviewId);
    return apiClient.delete(`/reviews/${reviewId}`);
};

// --- User Profile ---
export const getUserProfile = async () => {
    // try {
    //     const response = await apiClient.get('/users/me');
    //     return response.data;
    // } catch (error) {
    //     console.error("Error fetching user profile:", error.response || error.message);
    //     throw error;
    // }
    console.log('getUserProfile called');
    return apiClient.get('/users/me');
};

export const updateUserProfile = async (profileData) => {
    // profileData: { nickname, profilePicture }
    // try {
    //     const response = await apiClient.put('/users/me', profileData);
    //     return response.data;
    // } catch (error) {
    //     console.error("Error updating user profile:", error.response || error.message);
    //     throw error;
    // }
    console.log('updateUserProfile called with:', profileData);
    return apiClient.put('/users/me', profileData);
};

export const getReviewsByUser = async (userId) => {
    // Note: The backend currently has /api/users/{userId}/reviews
    // This means we need the userId. If 'me' is implied, backend might need adjustment or use /users/me/reviews.
    // For now, assuming we pass the specific userId.
    // try {
    //     const response = await apiClient.get(`/users/${userId}/reviews`);
    //     return response.data;
    // } catch (error) {
    //     console.error(`Error fetching reviews for user ${userId}:`, error.response || error.message);
    //     throw error;
    // }
    console.log('getReviewsByUser called for userId:', userId);
    return apiClient.get(`/users/${userId}/reviews`);
};

export default {
    login,
    register,
    getMovies,
    getMovieById,
    addMovie,
    addReview,
    getReviewsForMovie,
    deleteReview,
    getUserProfile,
    updateUserProfile,
    getReviewsByUser
};
