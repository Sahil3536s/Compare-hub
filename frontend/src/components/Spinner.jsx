export default function Spinner({ size = 24 }) {
  return (
    <div
      className="animate-spin rounded-full border-2 border-gray-300 border-t-gray-900"
      style={{
        width: `${size}px`,
        height: `${size}px`,
      }}
      role="status"
      aria-label="Loading"
    />
  );
}