import React from 'react';

export const ProductAlternativesSection = ({ alternatives, baseProductTitle, onSelectAlternative }) => {
  if (!alternatives || alternatives.length === 0) return null;

  const getBadge = (categoryType) => {
    switch (categoryType) {
      case 'CHEAPER_ALTERNATIVE':
        return <span className="px-2 py-0.5 rounded-md text-[10px] font-black bg-emerald-100 text-emerald-800">?? Cheaper Option</span>;
      case 'BETTER_VALUE':
        return <span className="px-2 py-0.5 rounded-md text-[10px] font-black bg-amber-100 text-amber-900">?? Best Value</span>;
      case 'SIMILAR_PRICE_BETTER_FEATURE':
        return <span className="px-2 py-0.5 rounded-md text-[10px] font-black bg-indigo-100 text-indigo-900">? Feature Upgrade</span>;
      case 'PREMIUM_ALTERNATIVE':
        return <span className="px-2 py-0.5 rounded-md text-[10px] font-black bg-purple-100 text-purple-900">? Premium</span>;
      default:
        return <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-100 text-slate-700">Alternative</span>;
    }
  };

  return (
    <div className="mt-8 rounded-3xl bg-slate-50 border border-slate-200/80 p-6 space-y-4" data-testid="product-alternatives-section">
      <div className="flex items-center justify-between">
        <div>
          <span className="text-[11px] font-bold text-indigo-600 uppercase tracking-wider">
            Value Comparison
          </span>
          <h3 className="text-lg font-black text-slate-900">
            Consider These Alternatives
          </h3>
        </div>
        <span className="text-xs text-slate-500 font-medium">
          {alternatives.length} suggestions
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {alternatives.map((alt) => (
          <div
            key={alt.id}
            className="bg-white rounded-2xl border border-slate-200 p-4 flex flex-col justify-between hover:border-indigo-300 hover:shadow-sm transition"
          >
            <div>
              <div className="flex items-center justify-between gap-2 mb-2">
                {getBadge(alt.categoryType)}
                {alt.priceDifference && Number(alt.priceDifference) < 0 && (
                  <span className="text-[11px] font-mono font-bold text-emerald-600">
                    -?{Math.abs(Number(alt.priceDifference)).toLocaleString('en-IN')}
                  </span>
                )}
              </div>
              <h4 className="font-bold text-sm text-slate-900 line-clamp-1">{alt.productName}</h4>
              <div className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                <span className="font-mono font-bold text-slate-800">?{Number(alt.price).toLocaleString('en-IN')}</span>
                <span>•</span>
                <span>? {alt.rating}</span>
              </div>

              {alt.highlights && alt.highlights.length > 0 && (
                <div className="mt-2.5 pt-2 border-t border-slate-100 space-y-1 text-[11px] text-slate-600">
                  {alt.highlights.slice(0, 2).map((h, i) => (
                    <div key={i} className="flex items-start gap-1">
                      <span className="text-emerald-500 font-bold">?</span>
                      <span>{h}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="mt-3 pt-2 border-t border-slate-100 flex items-center justify-end">
              <a
                href={alt.productUrl || '#'}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs font-bold text-indigo-600 hover:text-indigo-800"
              >
                View Details ?
              </a>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ProductAlternativesSection;
