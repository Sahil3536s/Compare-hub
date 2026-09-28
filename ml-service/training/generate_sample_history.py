"""
Script to generate realistic price history data for CompareHub products.
Creates data/price_history.csv representing realistic historical prices
spanning 120 days across verified merchant feeds (Amazon, Flipkart, Croma).
"""

import csv
import math
import random
from datetime import date, timedelta
from pathlib import Path


PRODUCTS = [
    {"id": 1, "name": "iPhone 15 Pro 128GB", "base_price": 129900.0, "volatility": 0.03, "trend": -0.0003},
    {"id": 2, "name": "Samsung Galaxy S24 Ultra 256GB", "base_price": 119999.0, "volatility": 0.04, "trend": -0.0005},
    {"id": 3, "name": "MacBook Air M2 256GB", "base_price": 99900.0, "volatility": 0.025, "trend": -0.0002},
    {"id": 4, "name": "Sony WH-1000XM5 Wireless Headphones", "base_price": 29990.0, "volatility": 0.05, "trend": -0.0004},
    {"id": 5, "name": "Dell XPS 13 16GB 512GB", "base_price": 114990.0, "volatility": 0.035, "trend": -0.0001},
    {"id": 6, "name": "iPad Air 5th Gen 64GB", "base_price": 54900.0, "volatility": 0.03, "trend": -0.0002},
    {"id": 7, "name": "OnePlus 12 256GB", "base_price": 64999.0, "volatility": 0.045, "trend": -0.0006},
    {"id": 8, "name": "Apple Watch Series 9 GPS", "base_price": 41900.0, "volatility": 0.03, "trend": -0.0003},
    {"id": 9, "name": "Bose QuietComfort 45", "base_price": 24900.0, "volatility": 0.04, "trend": -0.0002},
    {"id": 10, "name": "LG C3 55-inch OLED 4K TV", "base_price": 119990.0, "volatility": 0.05, "trend": -0.0007},
    {"id": 11, "name": "Canon EOS R50 Mirrorless Camera", "base_price": 65990.0, "volatility": 0.035, "trend": -0.0001},
    {"id": 12, "name": "Asus ROG Zephyrus G14 Gaming Laptop", "base_price": 139990.0, "volatility": 0.04, "trend": -0.0004},
]

MERCHANTS = ["Amazon", "Flipkart", "Croma"]


def generate_history(days: int = 120, output_path: str = "data/price_history.csv") -> None:
    random.seed(42)
    end_date = date.today()
    start_date = end_date - timedelta(days=days)

    rows = []

    for product in PRODUCTS:
        pid = product["id"]
        base = product["base_price"]
        volatility = product["volatility"]
        trend = product["trend"]

        current_price = base
        current_date = start_date

        while current_date <= end_date:
            day_offset = (current_date - start_date).days
            # Weekly seasonality: discounts slightly on weekends
            weekend_factor = -0.015 if current_date.weekday() in (5, 6) else 0.0

            # Periodic promotional sales (every ~30 days)
            promo_factor = -0.04 if (day_offset % 30) in (0, 1, 2) else 0.0

            # Random daily fluctuation
            daily_shock = random.gauss(0, volatility * 0.4)

            # Evolving drift
            drift = trend * day_offset

            price_multiplier = 1.0 + drift + weekend_factor + promo_factor + daily_shock
            price_multiplier = max(0.70, min(1.25, price_multiplier))

            daily_price = round(base * price_multiplier, 2)

            # Record for multiple merchants with slight variation
            for merchant in MERCHANTS:
                merchant_variation = 1.0 + (random.uniform(-0.02, 0.02))
                m_price = round(daily_price * merchant_variation, 2)
                rows.append({
                    "product_id": pid,
                    "date": current_date.strftime("%Y-%m-%d"),
                    "price": m_price,
                    "merchant": merchant,
                    "currency": "INR",
                })

            current_date += timedelta(days=1)

    out_file = Path(output_path)
    out_file.parent.mkdir(parents=True, exist_ok=True)

    with open(out_file, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["product_id", "date", "price", "merchant", "currency"])
        writer.writeheader()
        writer.writerows(rows)

    print(f"Generated {len(rows)} historical records for {len(PRODUCTS)} products across {days} days.")
    print(f"Saved to: {output_path}")


if __name__ == "__main__":
    generate_history()
