import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * Reusable ScrollToTop component for React Router.
 * Ensures every new route navigation or query parameter change
 * starts cleanly at scroll position (0, 0) without inherited scroll offsets.
 */
export const ScrollToTop = () => {
  const { pathname, search } = useLocation();

  useEffect(() => {
    // Reset window scroll position to top instantly
    window.scrollTo({
      top: 0,
      left: 0,
      behavior: 'instant',
    });
  }, [pathname, search]);

  return null;
};

export default ScrollToTop;
