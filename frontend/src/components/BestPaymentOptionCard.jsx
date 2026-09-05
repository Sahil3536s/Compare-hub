import React, { useState } from 'react';

export const BestPaymentOptionCard = ({ paymentOffers, onPreferenceChange }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [selectedBank, setSelectedBank] = useState('ALL');

  if (!paymentOffers || !paymentOffers.offers || paymentOffers.offers.length === 0) {
    return null;
  }

  const { standardPrice, bestEligiblePrice, maxSavings, bestOffer, offers, securityNote } = paymentOffers;

  const handleBankChange = (e) => {
    const bank = e.target.value;
    setSelectedBank(bank);
    if (onPreferenceChange) {
      onPreferenceChange({ preferredBank: bank });
    }
  };

  const filteredOffers = selectedBank === 'ALL'
    ? offers
    : offers.filter(o => o.bank === 'ALL' || o.bank?.toUpperCase() === selectedBank.toUpperCase() || o.paymentType === 'UPI');

  const statusBadge = (status) => {
    switch (status) {
      case 'ELIGIBLE':
        return (
          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 flex items-center gap-1">
            <span>?</span> Eligible
          </span>
        );
      case 'POSSIBLY_ELIGIBLE':
        return (
          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-200 flex items-center gap-1">
            <span>??</span> Check Card
          </span>
        );
      case 'NOT_ELIGIBLE':
      default:
        return (
          <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-slate-100 text-slate-500 border border-slate-200">
            Not Eligible
          </span>
        );
    }
  };

  const paymentTypeIcon = (type) => {
    switch (type) {
      case 'CREDIT_CARD':
        return '??';
      case 'DEBIT_CARD':
        return '??';
      case 'UPI':
        return '?';
      case 'WALLET':
        return '??';
      default:
        return '???';
    }
  };

  const hasSavings = maxSavings && Number(maxSavings) > 0;

  return (
    <div
      className="mt-2.5 rounded-2xl bg-linear-to-br from-indigo-50/90 via-slate-50 to-emerald-50/50 border border-indigo-100/90 p-3 shadow-2xs space-y-2 text-xs"
      data-testid="best-payment-option-card"
    >
      {/* Top Banner / Best Option Highlight */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold text-sm shadow-xs shrink-0">
            ??
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-extrabold text-[11px] text-indigo-950 uppercase tracking-wider">
                Best Payment Option
              </span>
              {hasSavings && (
                <span className="px-1.5 py-0.2 rounded-md bg-emerald-600 text-white font-black text-[10px] shadow-2xs">
                  Save ?{Number(maxSavings).toLocaleString('en-IN')}
                </span>
              )}
            </div>
            <div className="font-bold text-slate-800 text-xs mt-0.5">
              {bestOffer ? (
                <span>
                  Pay <span className="text-emerald-700 font-mono font-extrabold">?{Number(bestEligiblePrice).toLocaleString('en-IN')}</span> with {bestOffer.title || `${bestOffer.bank} ${bestOffer.paymentType}`}
                </span>
              ) : (
                <span>Standard checkout price ?{Number(standardPrice).toLocaleString('en-IN')}</span>
              )}
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setIsExpanded(!isExpanded)}
          className="text-[11px] font-bold text-indigo-700 hover:text-indigo-900 bg-white/90 hover:bg-white px-2.5 py-1 rounded-lg border border-indigo-200 transition shadow-2xs self-start sm:self-auto cursor-pointer"
          data-testid="toggle-payment-offers"
        >
          {isExpanded ? 'Hide Options ?' : `View ${offers.length} Offers ?`}
        </button>
      </div>

      {/* Expandable Comparison Section */}
      {isExpanded && (
        <div className="pt-2 border-t border-indigo-100/80 space-y-2.5 animate-fadeIn" data-testid="payment-offers-drawer">
          {/* Bank / Payment Method Quick Filter */}
          <div className="flex items-center justify-between gap-2 bg-white p-2 rounded-xl border border-slate-200/80">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wide">
              Filter by Bank:
            </span>
            <select
              value={selectedBank}
              onChange={handleBankChange}
              className="text-xs font-semibold bg-slate-50 border border-slate-200 rounded-lg px-2 py-0.5 text-slate-700 focus:outline-hidden focus:ring-1 focus:ring-indigo-500 cursor-pointer"
              data-testid="bank-filter-select"
            >
              <option value="ALL">All Available Offers</option>
              <option value="HDFC">HDFC Bank</option>
              <option value="ICICI">ICICI Bank</option>
              <option value="SBI">SBI Card</option>
              <option value="AXIS">Axis Bank</option>
              <option value="KOTAK">Kotak Mahindra</option>
            </select>
          </div>

          {/* Offer Comparison List */}
          <div className="space-y-1.5 max-h-56 overflow-y-auto pr-1">
            {filteredOffers.map((offer, idx) => (
              <div
                key={offer.id || idx}
                className={`p-2 rounded-xl border transition-all ${
                  offer.id === bestOffer?.id
                    ? 'bg-emerald-50/80 border-emerald-200 shadow-2xs'
                    : 'bg-white border-slate-200'
                }`}
                data-testid="payment-offer-item"
              >
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 font-bold text-slate-800 text-xs">
                    <span>{paymentTypeIcon(offer.paymentType)}</span>
                    <span className="line-clamp-1">{offer.title || offer.description}</span>
                  </div>
                  {statusBadge(offer.eligibilityStatus)}
                </div>

                <div className="flex items-center justify-between text-[11px] mt-1.5 pt-1 border-t border-slate-100 text-slate-600">
                  <div className="flex items-center gap-1.5">
                    <span className="text-emerald-700 font-bold font-mono">
                      -?{Number(offer.discountAmount).toLocaleString('en-IN')}
                    </span>
                    <span className="text-[10px] text-slate-400">
                      (Effective: ?{Number(offer.effectivePrice).toLocaleString('en-IN')})
                    </span>
                  </div>
                  <span className="text-[10px] text-slate-500 font-medium">
                    {offer.eligibilityReason || offer.terms}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Security & Privacy Reassurance Notice */}
          <div className="p-2 rounded-xl bg-slate-900 text-slate-300 text-[10px] flex items-start gap-1.5 leading-tight">
            <span className="shrink-0 text-xs">???</span>
            <span className="font-normal italic">
              {securityNote || 'CompareHub never collects or stores card numbers, CVVs, PINs, or OTPs. Only bank preferences are used to find matching discounts.'}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default BestPaymentOptionCard;
