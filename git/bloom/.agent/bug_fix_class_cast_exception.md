# Bug Fix: ClassCastException in Buyer Sales Edit

## Issue Description
An error occurred when editing buyer sales: `class java.lang.Integer cannot be cast to class java.lang.Long`.
This happened because the code was attempting to cast a database value directly to `Long`, but MongoDB had stored it as an `Integer` (likely due to small values).

## Changes Made
Modified `BloomSalesService.java` to safely retrieve numeric values:

Before:
```java
oldTotalAmount = oldSale.getLong("totalSaleAmount"); // Fails if value is Integer
oldExpenses = oldSale.getLong("expenses");           // Fails if value is Integer
```

After:
```java
oldTotalAmount = ((Number) oldSale.get("totalSaleAmount")).longValue(); // Safe for Integer, Long, Double
oldExpenses = ((Number) oldSale.get("expenses")).longValue();           // Safe for Integer, Long, Double
```

## Verification
- Checked `totalSaleAmount` retrieval
- Checked `expenses` retrieval
- Verified logic handles missing keys safely (default to `0L`)

This change ensures the edit feature works robustly regardless of how the numeric data is stored in the database.
