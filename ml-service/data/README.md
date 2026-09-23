# Training Data

This directory contains training data for the CompareHub ML price prediction model.

## Format

Expected CSV columns:

```
product_id,date,price,merchant,currency
1,2024-01-15,58999.00,Amazon,INR
1,2024-01-16,58499.00,Amazon,INR
```

## Data Source

Export historical price data from PostgreSQL:

```sql
COPY (
  SELECT 
    product_id,
    recorded_at::date AS date,
    price,
    merchant,
    currency
  FROM product_price_history
  ORDER BY product_id, recorded_at
) TO '/tmp/price_history.csv' WITH CSV HEADER;
```

## Important

- Training data files are NOT committed to version control.
- Do NOT store sensitive pricing data in this directory without proper data governance.
- Minimum 20 observations per product are needed for meaningful predictions.
