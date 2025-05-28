import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { updateUserProfile, getReviewsByUser, deleteReview } from '../services/api'; // Removed getUserProfile
import { useAuth } from '../context/AuthContext'; // Import useAuth

const ProfilePage = () => {
    const navigate = useNavigate();
    const { currentUser, isAuthenticated, isLoading: isAuthLoading, fetchUser } = useAuth(); // Use context

    // State for profile data will now come from currentUser, but we might want a local copy for editing
    // const [profile, setProfile] = useState(null); // This will be derived from currentUser
    const [userReviews, setUserReviews] = useState([]);
    // const [isLoadingProfile, setIsLoadingProfile] = useState(true); // Handled by isAuthLoading
    const [isLoadingReviews, setIsLoadingReviews] = useState(true);
    const [error, setError] = useState('');

    // For profile update form
    const [isEditing, setIsEditing] = useState(false);
    const [editNickname, setEditNickname] = useState('');
    const [editProfilePicture, setEditProfilePicture] = useState('');
    const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
    const [updateError, setUpdateError] = useState('');
    
    // Populate edit form when currentUser changes
    useEffect(() => {
        if (currentUser) {
            setEditNickname(currentUser.nickname || '');
            setEditProfilePicture(currentUser.profilePicture || '');
        }
    }, [currentUser]);


    const fetchReviews = useCallback(async () => {
        if (currentUser && currentUser.id) {
            setIsLoadingReviews(true);
            try {
                const reviewsResponse = await getReviewsByUser(currentUser.id);
                setUserReviews(reviewsResponse.data.sort((a, b) => new Date(b.reviewDate) - new Date(a.reviewDate)));
            } catch (err) {
                console.error("Error fetching user reviews:", err);
                setError(prev => `${prev} Failed to load your reviews: ${err.message}.`);
            } finally {
                setIsLoadingReviews(false);
            }
        }
    }, [currentUser]);

    useEffect(() => {
        if (!isAuthLoading && !isAuthenticated) {
            navigate('/login'); // Redirect if not authenticated and auth check is complete
        } else if (isAuthenticated && currentUser) {
            // setProfile(currentUser); // No longer need separate profile state
            fetchReviews();
        }
    }, [isAuthLoading, isAuthenticated, currentUser, navigate, fetchReviews]);

    const handleProfileUpdate = async (e) => {
        e.preventDefault();
        setIsUpdatingProfile(true);
        setUpdateError('');
        try {
            const updatedData = {
                nickname: editNickname,
                profilePicture: editProfilePicture,
            };
            const response = await updateUserProfile(updatedData);
            // Update context's currentUser by re-fetching or directly setting if API returns full user
            await fetchUser(); // This will refresh currentUser in AuthContext
            setIsEditing(false);
        } catch (err) {
            setUpdateError(`Failed to update profile: ${err.response?.data?.message || err.message}`);
            console.error("Profile update error:", err);
        } finally {
            setIsUpdatingProfile(false);
        }
    };
    
    const handleReviewDelete = async (reviewId) => {
        if (!window.confirm("Are you sure you want to delete this review?")) {
            return;
        }
        try {
            await deleteReview(reviewId);
            // Refresh user reviews
            // fetchReviews will be called if currentUser is present (which it should be)
            if (currentUser && currentUser.id) {
                 fetchReviews();
            }
        } catch (err) {
            alert(`Failed to delete review: ${err.response?.data?.message || err.message}`);
            console.error("Delete review error on profile:", err);
        }
    };

    // Use isAuthLoading from context
    if (isAuthLoading) return <p className="text-center text-lg mt-8">Loading profile...</p>;
    if (!isAuthenticated || !currentUser) { // Should be redirected by useEffect, but as a fallback
        return <p className="text-center text-lg mt-8">Please login to view your profile.</p>;
    }
    if (error && !currentUser) return <p className="text-center text-red-500 text-lg mt-8 bg-red-100 p-4 rounded-md">{error}</p>;
    // If currentUser exists but there was a partial error (e.g. fetching reviews), show profile with error message for reviews.

    return (
        <div className="container mx-auto p-4">
            {/* Profile Details Section */}
            {currentUser && ( // Ensure currentUser is loaded before trying to display its properties
                 <div className="bg-white shadow-xl rounded-lg p-6 mb-10">
                    <div className="flex flex-col md:flex-row items-center md:items-start">
                        <img 
                            src={currentUser.profilePicture || 'https://via.placeholder.com/150.png?text=No+Image'} 
                            alt="Profile"
                            className="w-32 h-32 md:w-40 md:h-40 rounded-full object-cover mb-6 md:mb-0 md:mr-8 border-4 border-gray-200 shadow-sm"
                        />
                        <div className="flex-grow text-center md:text-left">
                            <h1 className="text-3xl font-bold text-gray-800 mb-1">{currentUser.nickname}</h1>
                            <p className="text-gray-600 mb-4">{currentUser.email}</p>
                        <button 
                            onClick={() => setIsEditing(!isEditing)}
                            className="bg-blue-500 hover:bg-blue-600 text-white py-2 px-4 rounded-md text-sm"
                        >
                            {isEditing ? 'Cancel Edit' : 'Edit Profile'}
                        </button>
                    </div>
                </div>

                {/* Edit Profile Form */}
                {isEditing && (
                    <form onSubmit={handleProfileUpdate} className="mt-8 pt-6 border-t border-gray-200">
                        <h2 className="text-xl font-semibold mb-4 text-gray-700">Edit Your Profile</h2>
                        {updateError && <p className="text-red-500 text-sm mb-3">{updateError}</p>}
                        <div className="mb-4">
                            <label htmlFor="editNickname" className="block text-sm font-medium text-gray-700">Nickname</label>
                            <input
                                type="text"
                                id="editNickname"
                                value={editNickname}
                                onChange={(e) => setEditNickname(e.target.value)}
                                className="mt-1 block w-full md:w-1/2 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                            />
                        </div>
                        <div className="mb-4">
                            <label htmlFor="editProfilePicture" className="block text-sm font-medium text-gray-700">Profile Picture URL</label>
                            <input
                                type="text"
                                id="editProfilePicture"
                                value={editProfilePicture}
                                onChange={(e) => setEditProfilePicture(e.target.value)}
                                className="mt-1 block w-full md:w-1/2 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                placeholder="https://example.com/image.png"
                            />
                        </div>
                        <button
                            type="submit"
                            disabled={isUpdatingProfile}
                            className="py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-green-600 hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 disabled:bg-gray-400"
                        >
                            {isUpdatingProfile ? 'Saving...' : 'Save Changes'}
                        </button>
                    </form>
                )}
            </div>

            {/* User's Reviews Section */}
            <div>
                <h2 className="text-2xl font-semibold mb-6 text-gray-700">Your Reviews</h2>
                {isLoadingReviews ? (
                    <p className="text-gray-600">Loading your reviews...</p>
                ) : userReviews.length > 0 ? (
                    <div className="space-y-6">
                        {userReviews.map(review => (
                            <div key={review.id} className="bg-white p-5 shadow rounded-lg">
                                <p className="font-semibold text-gray-800">Movie ID: {review.movieId}</p> {/* TODO: Link to movie or show title */}
                                <p className="text-yellow-500">Rating: {review.rating}/5</p>
                                <p className="text-gray-700 mt-2 mb-1">{review.comment}</p>
                                <p className="text-xs text-gray-400">
                                    Reviewed on: {new Date(review.reviewDate).toLocaleDateString()}
                                </p>
                                <button
                                    onClick={() => handleReviewDelete(review.id)}
                                    className="mt-2 text-xs text-red-500 hover:text-red-700"
                                >
                                    Delete This Review
                                </button>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="text-gray-600">You haven't written any reviews yet.</p>
                )}
            </div>
        </div>
    );
};

export default ProfilePage;
