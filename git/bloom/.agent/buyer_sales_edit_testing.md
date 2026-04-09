# Buyer Sales Edit Feature - Testing Guide

## 🧪 How to Test the Feature

### Prerequisites:
1. Backend server running
2. Frontend accessible
3. At least one buyer with sales records

---

## Test Scenario 1: Edit Item Quantity

**Steps:**
1. Go to Buyer page
2. Click "View Ledger" for any buyer
3. Click the eye icon on any sale date
4. Click the yellow **"Edit"** button
5. Change the weight of any flower item
6. Observe the total recalculating in real-time
7. Check the "Old Outstanding" vs "New Outstanding" preview
8. Click **"Save Changes"**
9. Verify success message appears
10. Check that ledger refreshes with new amounts

**Expected Result:**
- ✅ Total updates automatically
- ✅ Outstanding preview shows correct difference
- ✅ Save succeeds
- ✅ Ledger shows updated amounts
- ✅ Buyer's outstanding balance is adjusted

---

## Test Scenario 2: Delete an Item

**Steps:**
1. Open edit modal for a sale with multiple items
2. Click the red **trash icon** on one item
3. Confirm deletion in the dialog
4. Observe the item disappears
5. Check totals recalculate
6. Click **"Save Changes"**
7. Verify the item is gone from the ledger

**Expected Result:**
- ✅ Item removed from table
- ✅ Totals recalculate correctly
- ✅ Outstanding adjusts by the deleted amount
- ✅ Ledger no longer shows the deleted item

---

## Test Scenario 3: Delete All Items

**Steps:**
1. Open edit modal
2. Delete all flower items one by one
3. Try to save
4. Confirm the warning about zero sale amount
5. Verify sale is set to zero

**Expected Result:**
- ✅ Warning appears before saving
- ✅ User can cancel or proceed
- ✅ If proceeded, sale amount becomes zero
- ✅ Outstanding reduces by full original amount

---

## Test Scenario 4: Modify Price

**Steps:**
1. Open edit modal
2. Change the price of an item
3. Observe total recalculating
4. Save changes
5. Verify new price appears in ledger

**Expected Result:**
- ✅ Price change reflects in total
- ✅ Outstanding adjusts correctly
- ✅ Ledger shows new price

---

## Test Scenario 5: Cancel Edit

**Steps:**
1. Open edit modal
2. Make some changes
3. Click **"Cancel"** button
4. Verify no changes were saved

**Expected Result:**
- ✅ Modal closes
- ✅ No changes saved
- ✅ Ledger remains unchanged

---

## Test Scenario 6: Error Handling

**Steps:**
1. Stop the backend server
2. Try to edit and save a sale
3. Observe error handling

**Expected Result:**
- ✅ Error message appears
- ✅ Save button re-enables
- ✅ User can retry

---

## Verification Checklist

After testing, verify:

### Frontend:
- [ ] Edit button appears in buyer ledger modal
- [ ] Edit modal opens correctly
- [ ] All flower items are editable
- [ ] Delete buttons work
- [ ] Real-time calculation works
- [ ] Outstanding preview is accurate
- [ ] Save button shows loading state
- [ ] Success message appears
- [ ] Ledger refreshes automatically

### Backend:
- [ ] PUT endpoint receives requests
- [ ] Old sale is fetched correctly
- [ ] Difference is calculated correctly
- [ ] Sales record is updated
- [ ] Buyer outstanding is adjusted
- [ ] Journal entry is updated
- [ ] Logs show all operations
- [ ] No errors in console

### Database:
- [ ] Sales collection updated
- [ ] Buyer outstanding changed
- [ ] BuyerJournal entry updated
- [ ] No orphaned data

---

## Common Issues & Solutions

### Issue: Edit button doesn't appear
**Solution:** Make sure you're viewing a sale with the expenses feature enabled

### Issue: Outstanding doesn't update
**Solution:** Check backend logs for errors in updateBuyerOutStanding

### Issue: Ledger doesn't refresh
**Solution:** Check if loadBuyerLedger is being called after save

### Issue: Can't delete items
**Solution:** Check browser console for JavaScript errors

---

## Backend Logs to Check

When testing, monitor these logs:

```
Old sale found | Total Amount: X | Expenses: Y
New sale calculation | Total Amount: X | Expenses: Y | Actual: Z
Difference in sale amount: X (Old: Y - New: Z)
Sales record updated | Matched: 1
Buyer outstanding adjusted | BuyerID: xxx | Adjustment: X
Buyer journal updated | BuyerID: xxx | Date: yyyy-MM-dd | New Purchase: X
```

---

## Success Criteria

The feature is working correctly if:

1. ✅ Users can edit past sales
2. ✅ Outstanding balance updates correctly
3. ✅ Ledger stays synchronized
4. ✅ All changes are logged
5. ✅ No data inconsistencies
6. ✅ User experience is smooth
7. ✅ Error handling works properly

---

## Performance Testing

Test with:
- Sales with 1 item
- Sales with 10+ items
- Sales with expenses
- Sales without expenses
- Multiple edits to same sale
- Rapid successive edits

All should work smoothly without errors.
