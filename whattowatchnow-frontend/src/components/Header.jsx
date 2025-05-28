import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext'; // Import useAuth

const Header = () => {
    const { isAuthenticated, currentUser, logout, isLoading } = useAuth(); // Use context

    // Don't render anything or render a loading state if auth status is still loading
    // This prevents flicker or showing wrong auth state initially
    if (isLoading) {
        return (
            <header className="bg-gray-800 text-white p-4 shadow-md">
                <div className="container mx-auto flex justify-between items-center">
                    <Link to="/" className="text-2xl font-bold hover:text-gray-300">WhatToWatchNow</Link>
                    <div className="text-sm">Loading user...</div>
                </div>
            </header>
        );
    }

    return (
        <header className="bg-gray-800 text-white p-4 shadow-md">
            <div className="container mx-auto flex justify-between items-center">
                <Link to="/" className="text-2xl font-bold hover:text-gray-300">WhatToWatchNow</Link>
                
                {/* Placeholder for Search Bar */}
                <div className="relative flex-grow max-w-xs mx-4">
                    {/* Search functionality will be added later */}
                </div>

                <nav className="flex items-center space-x-4">
                    <Link to="/" className="hover:text-gray-300">Home</Link>
                    {isAuthenticated && currentUser ? (
                        <>
                            {/* Use currentUser from context */}
                            <Link to="/profile" className="hover:text-gray-300">{currentUser.nickname || 'Profile'}</Link>
                            <button onClick={logout} className="bg-red-500 hover:bg-red-600 px-3 py-2 rounded">Logout</button>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="hover:text-gray-300">Login</Link>
                            <Link to="/register" className="bg-blue-500 hover:bg-blue-600 px-3 py-2 rounded">Register</Link>
                        </>
                    )}
                </nav>
            </div>
        </header>
    );
};

export default Header;
