import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getUserProfile as fetchUserProfileAPI, login as loginAPI } from '../services/api'; // Assuming loginAPI is also needed here for login function
import { useNavigate } from 'react-router-dom';


const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [currentUser, setCurrentUser] = useState(null);
    const [isLoading, setIsLoading] = useState(true); // For initial auth check
    const [token, setToken] = useState(localStorage.getItem('userToken'));
    const navigate = useNavigate();

    const fetchAndSetUser = useCallback(async (currentToken) => {
        if (currentToken) {
            try {
                // Ensure the token is set in localStorage for the apiClient interceptor to pick it up
                localStorage.setItem('userToken', currentToken);
                const response = await fetchUserProfileAPI();
                setCurrentUser(response.data);
                localStorage.setItem('userNickname', response.data.nickname); // Keep nickname in sync
            } catch (error) {
                console.error("Failed to fetch user profile:", error);
                setCurrentUser(null);
                localStorage.removeItem('userToken');
                localStorage.removeItem('userNickname');
                setToken(null);
            }
        } else {
            setCurrentUser(null);
            localStorage.removeItem('userToken');
            localStorage.removeItem('userNickname');
        }
        setIsLoading(false);
    }, []);

    useEffect(() => {
        fetchAndSetUser(token);
        
        // Listen to custom authChange event (e.g., after login/logout from non-context components)
        const handleAuthChange = () => {
            const newToken = localStorage.getItem('userToken');
            setToken(newToken); // This will trigger the fetchAndSetUser via the other useEffect
            if (newToken) {
                fetchAndSetUser(newToken);
            } else {
                 setCurrentUser(null); // Clear user if token is removed
            }
        };
        window.addEventListener('authChange', handleAuthChange);
        return () => window.removeEventListener('authChange', handleAuthChange);

    }, [token, fetchAndSetUser]);


    const login = async (email, password) => {
        try {
            // Use the login function from api.js (handles x-www-form-urlencoded)
            const response = await loginAPI(email, password); // from '../services/api'
            
            if (response.status === 200 || response.status === 204) { // Successful form login
                // Spring Security's default form login sets an HTTP-only session cookie.
                // For SPAs, it's common to have a subsequent call to /api/users/me or similar
                // to get user details and confirm session validity.
                // Or, if the /login endpoint was customized to return a JWT token:
                // const { token: apiToken, user } = response.data;
                // localStorage.setItem('userToken', apiToken);
                // setToken(apiToken); // This will trigger useEffect to fetch user
                // setCurrentUser(user); // Or set directly if login returns user
                
                // For form-based login, we assume success means session is active.
                // We need a way to confirm and get user data. A "mock" token can represent the session.
                localStorage.setItem('userToken', 'session_active_after_login'); // Indicates session established
                // Fetch user profile immediately to confirm and get data
                await fetchAndSetUser('session_active_after_login'); // Pass the representative token
                return true; // Indicate success
            }
            return false; // Indicate failure if status is not success
        } catch (error) {
            console.error("AuthContext login error:", error);
            // Ensure cleanup if login fails
            localStorage.removeItem('userToken');
            localStorage.removeItem('userNickname');
            setToken(null);
            setCurrentUser(null);
            throw error; // Re-throw for the component to handle
        }
    };

    const logout = () => {
        // For session-based auth, ideally call a /logout endpoint on backend
        // For token-based, just clear client-side
        localStorage.removeItem('userToken');
        localStorage.removeItem('userNickname');
        setToken(null);
        setCurrentUser(null);
        navigate('/login'); // Navigate to login page
        window.dispatchEvent(new CustomEvent('authChange')); // Notify other components like Header
    };
    
    const register = async (userData) => {
        // Registration doesn't usually log the user in immediately,
        // so just call the API and let the user log in separately.
        // If auto-login after register is desired, this function would be more complex.
        // For now, it's just a passthrough or can be removed if register is handled directly by component.
        // return registerAPI(userData); // Assuming registerAPI from services/api
        console.log("AuthContext: Register - This would typically call api.register");
        // The component will handle actual registration call.
    };


    const value = {
        currentUser,
        isAuthenticated: !!currentUser, // True if currentUser object exists
        isLoading,
        login,
        logout,
        register, // If providing register through context
        fetchUser: () => fetchAndSetUser(localStorage.getItem('userToken')) // Allow manual refresh of user
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
