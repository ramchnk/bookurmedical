# Buyer Sales Edit Feature - Implementation Plan

## Problem Statement
Users need the ability to edit past buyer sales reports when buyers dispute items they didn't purchase. When a sale is edited:
- The sale record must be updated
- The buyer's outstanding balance must be recalculated
- The buyer's ledger must reflect the changes

## Solution Architecture

### 1. Frontend Changes

#### A. Add Edit Icon to Buyer Ledger Modal (`buyer.js`)
- Add an edit icon/button next to each flower item in the `showBuyerLedgerModal` function
- Store the sale date and buyer ID for editing context

#### B. Create Edit Sales Modal (HTML)
- New modal to display editable flower items
- Allow users to:
  - Delete individual flower items
  - Modify quantity/price of items
  - See real-time recalculation of totals

#### C. Edit Sales JavaScript Logic (`buyer.js`)
- Function to load sales data into edit modal
- Function to handle item deletion
- Function to handle item modification
- Function to save edited sales and trigger backend update

### 2. Backend Changes

#### A. New API Endpoint - Edit/Update Buyer Sales
**Endpoint**: `PUT /sales/buyer-sales`
**Purpose**: Update existing buyer sales record

**Request Payload**:
```json
{
  "saleDate": 1234567890,
  "buyerID": "buyer123",
  "flowerSaleItems": [
    {
      "flowerName": "Rose",
      "weight": 10,
      "saleAmount": 100,
      "totalAmount": 1000
    }
  ],
  "expenses": 50
}
```

**Logic**:
1. Find existing sale record by saleDate + buyerID
2. Calculate old total amount
3. Update sale record with new flower items
4. Calculate new total amount
5. Calculate difference (old - new)
6. Update buyer's outstanding balance by the difference
7. Update buyer journal entry

#### B. Service Layer Updates
- `BloomInSalesController.java` - Add PUT endpoint
- `SalesService.java` - Add `updateBuyerSale()` method
- `BloomBuyerService.java` - Add method to adjust outstanding

### 3. Data Flow

```
User clicks edit → Load sale data → Edit modal opens
User modifies items → Click save → Calculate difference
Backend updates sale → Updates outstanding → Updates journal
Frontend refreshes → Shows updated data
```

### 4. Edge Cases to Handle

1. **Complete deletion**: If all items removed, set sale amount to 0
2. **Negative outstanding**: Handle cases where edit creates negative balance
3. **Concurrent edits**: Prevent multiple edits to same sale simultaneously
4. **Audit trail**: Log all edits for accountability
5. **GPay/Discount**: Preserve payment and discount information

### 5. Implementation Steps

1. ✅ Create edit modal HTML structure
2. ✅ Add edit icon to buyer ledger modal
3. ✅ Implement frontend edit logic
4. ✅ Create backend PUT endpoint
5. ✅ Implement service layer logic
6. ✅ Test edit flow end-to-end
7. ✅ Add validation and error handling

## Files to Modify

### Frontend
- `/webapp/dashboard/index.html` - Add edit modal
- `/webapp/dashboard/assets/js/buyer.js` - Add edit functions

### Backend
- `/bloomserver/src/main/java/com/smartstockhub/bloom/restcontroller/BloomInSalesController.java`
- `/bloomserver/src/main/java/com/smartstockhub/bloom/service/SalesService.java`
- `/bloomserver/src/main/java/com/smartstockhub/bloom/service/BloomBuyerService.java`

## Success Criteria

- ✅ User can edit past sales from buyer ledger
- ✅ Outstanding balance updates correctly
- ✅ Ledger reflects changes immediately
- ✅ No data inconsistencies
- ✅ Audit trail maintained
