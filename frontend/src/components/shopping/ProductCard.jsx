import React from 'react';
import DemoBadge from './DemoBadge';

/**
 * Card component for a normalized product offer.
 * Props:
 *  - offer: NormalizedProductOfferDto (see hook typings)
 */
export const ProductCard = ({ offer }) => {
  const {
    productName,
    brand,
    merchant,
    price,
    originalPrice,
    discount,
    rating,
    delivery,
    availability,
    attributes,
    imageUrl,
    dataSource,
    live,
    productId,
  } = offer;

  const hasDiscount = originalPrice && originalPrice > price;

  const viewDealUrl = offer.url || '#'; // fallback if no URL provided

  return (
    <div className="border rounded-lg shadow-sm p-4 bg-white flex flex-col h-full">
      <div className="flex items-center mb-3">
        {imageUrl && (
          <img src={imageUrl} alt={productName} className="w-16 h-16 object-cover rounded mr-3" />
        )}
        <div className="flex-1">
          <h3 className="text-lg font-semibold line-clamp-2">{productName}</h3>
          {brand && <p className="text-sm text-gray-500">{brand}</p>}
        </div>
        {!live && <DemoBadge />}
      </div>

      <div className="flex-1">
        {attributes && Object.entries(attributes).map(([key, val]) => (
          <p key={key} className="text-xs text-gray-600">
            <strong>{key}:</strong> {val}
          </p>
        ))}
      </div>

      <div className="mt-3">
        <div className="flex items-baseline space-x-2">
          <span className="text-xl font-bold text-primary-600">${price.toFixed(2)}</span>
          {hasDiscount && (
            <>
              <span className="line-through text-sm text-gray-500">${originalPrice.toFixed(2)}</span>
              <span className="text-sm text-green-600">{discount}% off</span>
            </>
          )}
        </div>
        {rating && (
          <p className="text-sm text-yellow-600 mt-1">Rating: {rating} ★</p>
        )}
        {delivery && (
          <p className="text-sm text-gray-700">Delivery: {delivery}</p>
        )}
        {availability && (
          <p className="text-sm text-gray-700">Availability: {availability}</p>
        )}
        {merchant && (
          <p className="text-sm text-gray-600 mt-1">Seller: {merchant}</p>
        )}
        {dataSource && (
          <p className="text-xs text-gray-400 mt-1">Source: {dataSource}</p>
        )}
      </div>

      <div className="mt-4 pt-2 border-t">
        <a
          href={viewDealUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="block w-full text-center bg-indigo-600 text-white py-2 rounded hover:bg-indigo-700 transition"
        >
          View Deal
        </a>
      </div>
    </div>
  );
};

export default ProductCard;
