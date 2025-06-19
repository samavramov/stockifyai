// src/apiService.js

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

async function apiFetch(endpoint, options = {}) {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  if (!response.ok) {
    const errorBody = await response.text();
    let errorMessage = `API Error: ${response.statusText}`;
    try {
      const errorJson = JSON.parse(errorBody);
      if (errorJson.error) errorMessage = errorJson.error;
    } catch (e) {
      if (errorBody) errorMessage = errorBody;
    }
    throw new Error(errorMessage);
  }
  
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

// --- User & Session Functions ---
export const checkAuthStatus = () => apiFetch('/me');
export const getUserData = (email) => apiFetch(`/api/getUser?email=${encodeURIComponent(email)}`);
/**
 * Calls the backend logout endpoint.
 */
export const logoutUser = () => apiFetch('/logout', { method: 'GET' });

// --- Stock Data Functions ---
export const getAllSentiments = () => apiFetch('/api/sentiments');
export const getFollowedSymbols = (email) => apiFetch(`/api/getFollowedStocks?email=${encodeURIComponent(email)}`);

// --- Stock Following Functions ---
export const followStock = (email, stockSymbol) => {
  return apiFetch('/api/followStock', {
    method: 'POST',
    body: JSON.stringify({ email, stockSymbol }),
  });
};

export const unfollowStock = (email, stockSymbol) => {
  return apiFetch('/api/unfollowStock', {
    method: 'POST',
    body: JSON.stringify({ email, stockSymbol }),
  });
};