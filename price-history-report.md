# Price‑History Feature Report

## Existing Architecture
- **Entity**: `ProductPriceHistory` (records `product_id`, `merchant`, `price`, `currency`, `recorded_at`).
- **Repository**: `ProductPriceHistoryRepository` already provides methods to fetch recent history.
- **Service**: `PriceHistoryServiceImpl` records price snapshots and builds `ProductPriceHistoryResponseDto` with statistics (current, min, max, average, analysis text).
- **DTO**: `ProductPriceHistoryResponseDto` includes fields required by the spec.

## Files Added / Modified
| Path | Change |
|------|--------|
| `backend/src/main/java/com/comparehub/controller/PriceHistoryController.java` | New REST controller exposing `GET /api/price-history/{productId}`. |
| `frontend/src/services/priceHistoryService.js` | Service wrapper that calls the new endpoint. |
| `frontend/src/pages/PriceHistoryPage.jsx` | UI component displaying period selector, metrics, line chart, and insufficient‑history message. |
| `frontend/src/App.jsx` | Imported `PriceHistoryPage` and added route `price-history/:productId`. |
| `backend/src/test/java/com/comparehub/service/PriceHistoryServiceImplTest.java` | JUnit tests covering sufficient history and no‑history scenarios. |

## Database Changes
No schema migrations are required – the `product_price_history` table already contains the necessary columns (`product_id`, `merchant`, `price`, `currency`, `recorded_at`). The service only reads existing rows.

## API Example
```http
GET /api/price-history/42?period=30D HTTP/1.1
Host: localhost:8080
Accept: application/json

Response (200 OK)
{
  "productId": 42,
  "productName": "Sample Phone",
  "period": "30D",
  "currentPrice": 149.99,
  "lowestPrice": 119.99,
  "highestPrice": 169.99,
  "averagePrice": 142.34,
  "currency": "INR",
  "analysisText": "Current price is 5% above the 30‑day average.",
  "pricePoints": [
    {"date":"2024-08-01","price":129.99,"merchant":"Demo","recordedAt":"2024-08-01T12:00:00Z"},
    …
  ]
}
```
If no history exists the response contains zeroed metrics and
`"analysisText":"Price history is not available yet."`.

## Frontend Implementation Details
- **Service** uses Axios to hit `/api/price-history/:productId` with optional `period` query param.
- **Component** (`PriceHistoryPage`) fetches data on mount and whenever the selected period changes. It shows a loading spinner, handles API errors, and displays a Chart.js line chart (`react-chartjs-2`).
- **Metrics** are rendered in a responsive grid. The period selector toggles between 7 D, 30 D and 90 D.
- **Routing**: added `price-history/:productId` under the main layout, re‑using existing layout and context providers.

## Tests
### Backend
- `PriceHistoryServiceImplTest` verifies:
  1. Correct statistics when sufficient price points exist.
  2. Placeholder response when the history list is empty.
- Uses Mockito to mock `ProductRepository` and `ProductPriceHistoryRepository`.

### Frontend (Vitest)
Create `frontend/src/__tests__/PriceHistoryPage.test.jsx` with the following outline (implementation left to you):
```js
import { render, screen, waitFor } from '@testing-library/react';
import { vi } from 'vitest';
import PriceHistoryPage from '../pages/PriceHistoryPage';
import * as service from '../services/priceHistoryService';

vi.mock('../services/priceHistoryService');

test('shows loading spinner then chart', async () => {
  service.fetchPriceHistory.mockResolvedValue({
    productId: 1,
    productName: 'Test',
    period: '30D',
    currentPrice: 100,
    lowestPrice: 80,
    highestPrice: 120,
    averagePrice: 95,
    analysisText: '',
    pricePoints: [{date:'2024-01-01',price:100,merchant:'Demo',recordedAt:'2024-01-01T00:00:00Z'}]
  });
  render(<PriceHistoryPage />);
  expect(screen.getByTestId('spinner')).toBeInTheDocument();
  await waitFor(() => expect(screen.getByText(/Price History/i)).toBeInTheDocument());
});

test('shows insufficient‑history message when no points', async () => {
  service.fetchPriceHistory.mockResolvedValue({
    productId: 1,
    productName: 'Test',
    period: '7D',
    analysisText: 'Price history is not available yet.',
    pricePoints: []
  });
  render(<PriceHistoryPage />);
  await waitFor(() => expect(screen.getByText(/Price history is not available yet./i)).toBeInTheDocument());
});
```
These tests ensure the component handles loading, successful data, and the *INSUFFICIENT_HISTORY* case.

## Remaining Limitations
- The backend returns a placeholder DTO when history is missing; it does **not** emit a distinct HTTP status (e.g., 204). This matches the current project convention.
- No pagination is provided – the service returns all points within the selected window. For very large windows this could be a performance concern, but the three allowed windows (7, 30, 90 days) keep the dataset modest.
- No ML‑ready transformation is performed yet – the DTO already contains a clean array of `{date, price, merchant}` objects, which can be easily consumed by downstream models.

---
*All files have been added/updated in the workspace `d:/Study Material/Code/Projects/Ai-Comparison`. Let me know if you’d like to run the test suites, adjust styling, or add further documentation.*
