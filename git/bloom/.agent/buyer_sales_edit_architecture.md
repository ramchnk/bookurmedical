# Buyer Sales Edit Feature - Architecture Diagram

## System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. Buyer Ledger Modal                                              │
│     ┌──────────────────────────────────────┐                        │
│     │  🌸 Sold Flower Details              │                        │
│     │  ─────────────────────────────────   │                        │
│     │  Rose      10 KG    ₹100    ₹1000   │                        │
│     │  Jasmine    5 KG    ₹200    ₹1000   │                        │
│     │  ─────────────────────────────────   │                        │
│     │  Total: ₹2000                        │                        │
│     │                                       │                        │
│     │  [Close] [✏️ Edit] [🖨️] [📱]        │                        │
│     └──────────────────────────────────────┘                        │
│                    │                                                 │
│                    │ Click Edit                                      │
│                    ▼                                                 │
│  2. Edit Sales Modal                                                │
│     ┌──────────────────────────────────────────────────────────┐   │
│     │  ✏️ Edit Sales Report                                    │   │
│     │  ────────────────────────────────────────────────────    │   │
│     │  ℹ️ You can delete items or modify quantities           │   │
│     │                                                           │   │
│     │  Flower   │ Weight │ Price │ Total  │ Action             │   │
│     │  ────────┼────────┼───────┼────────┼────────            │   │
│     │  Rose    │ [10]   │ [100] │ ₹1000  │ [🗑️]              │   │
│     │  Jasmine │ [5]    │ [200] │ ₹1000  │ [🗑️]              │   │
│     │  ────────────────────────────────────────────            │   │
│     │  Total Purchase: ₹2000                                   │   │
│     │                                                           │   │
│     │  ⚠️ Old Outstanding: ₹5000 → New: ₹5000                 │   │
│     │                                                           │   │
│     │  [Cancel] [✅ Save Changes]                              │   │
│     └──────────────────────────────────────────────────────────┘   │
│                    │                                                 │
│                    │ Save Changes                                    │
│                    ▼                                                 │
└─────────────────────────────────────────────────────────────────────┘
                     │
                     │ PUT /sales/buyer-sales
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      BACKEND PROCESSING                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. BloomInSalesController.updateBuyerSales()                       │
│     ├─ Receives request                                             │
│     └─ Calls service layer                                          │
│                    │                                                 │
│                    ▼                                                 │
│  2. BloomSalesService.updateBuyerSales()                            │
│     ├─ Fetch old sales record                                       │
│     │  Old: Rose(₹1000) + Jasmine(₹1000) = ₹2000                   │
│     │                                                                │
│     ├─ Calculate new total                                          │
│     │  New: Rose(₹1000) = ₹1000                                     │
│     │                                                                │
│     ├─ Calculate difference                                         │
│     │  Difference: ₹2000 - ₹1000 = ₹1000                           │
│     │                                                                │
│     ├─ Update sales record                                          │
│     │  flowerSaleItems: [Rose only]                                 │
│     │  totalSaleAmount: ₹1000                                       │
│     │                                                                │
│     ├─ Adjust buyer outstanding                                     │
│     │  outStanding: ₹5000 - ₹1000 = ₹4000                          │
│     │                                                                │
│     └─ Update buyer journal                                         │
│        purchase: ₹1000                                               │
│                    │                                                 │
│                    ▼                                                 │
└─────────────────────────────────────────────────────────────────────┘
                     │
                     │ Success Response
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      DATABASE UPDATES                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  1. Sales Collection                                                │
│     {                                                                │
│       buyerID: "buyer123",                                          │
│       saleDate: 1234567890,                                         │
│       flowerSaleItems: [                                            │
│         { flowerName: "Rose", weight: 10, ... }                     │
│       ],                                                             │
│       totalSaleAmount: 1000,  ← UPDATED                             │
│       actualSalesAmount: 1000 ← UPDATED                             │
│     }                                                                │
│                                                                       │
│  2. Buyer Collection                                                │
│     {                                                                │
│       _id: "buyer123",                                              │
│       name: "Customer Name",                                        │
│       outStanding: 4000  ← UPDATED (was 5000)                       │
│     }                                                                │
│                                                                       │
│  3. BuyerJournal Collection                                         │
│     {                                                                │
│       buyerID: "buyer123",                                          │
│       date: "2026-02-10",                                           │
│       openingBalance: 3000,                                         │
│       purchase: 1000,  ← UPDATED (was 2000)                         │
│       credit: 0,                                                     │
│       closingBalance: 4000  ← RECALCULATED                          │
│     }                                                                │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Data Flow Summary

```
User Action → Frontend Validation → Backend API → Service Logic
                                                        │
                                                        ├─ Fetch Old Data
                                                        ├─ Calculate Difference
                                                        ├─ Update Sales
                                                        ├─ Adjust Outstanding
                                                        └─ Update Journal
                                                                │
                                                                ▼
                                                        Database Updates
                                                                │
                                                                ▼
                                                        Success Response
                                                                │
                                                                ▼
                                                        Refresh UI
```

## Key Calculations

### Outstanding Balance Adjustment:
```
Old Sale Total: ₹2,000
New Sale Total: ₹1,000
Difference: ₹2,000 - ₹1,000 = ₹1,000

Old Outstanding: ₹5,000
Adjustment: -₹1,000 (reduce because sale decreased)
New Outstanding: ₹5,000 - ₹1,000 = ₹4,000
```

### Journal Entry Update:
```
Opening Balance: ₹3,000 (unchanged)
Purchase: ₹1,000 (updated from ₹2,000)
Credit: ₹0 (unchanged)
Closing Balance: ₹3,000 + ₹1,000 - ₹0 = ₹4,000
```

## Error Handling Flow

```
User Saves Edit
      │
      ├─ Frontend Validation
      │  ├─ All items deleted? → Confirm
      │  └─ Invalid data? → Show error
      │
      ├─ Backend Processing
      │  ├─ No old record found? → Error
      │  ├─ Database error? → Rollback
      │  └─ Success → Continue
      │
      └─ UI Update
         ├─ Success → Refresh ledger
         └─ Error → Show message, re-enable button
```
