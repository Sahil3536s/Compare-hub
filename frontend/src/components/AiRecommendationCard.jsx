import React from 'react';
import DecisionAdvisorCard from './DecisionAdvisorCard';

export const AiRecommendationCard = ({ recommendation }) => {
  if (!recommendation) {
    return null;
  }

  return (
    <DecisionAdvisorCard
      recommendation={recommendation}
      title="AI Decision Advisor"
    />
  );
};

export default AiRecommendationCard;
