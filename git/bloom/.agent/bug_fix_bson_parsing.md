# Bug Fix: MongoDB BSON Parsing Error

## Issue Description
An error occurred when updating buyer sales:
`java.lang.RuntimeException: Failed to update buyer sales: readStartDocument can only be called when CurrentBSONType is DOCUMENT, not when CurrentBSONType is ARRAY.`

This was caused by attempting to parse a JSON Array string (`[...]`) directly using `Document.parse()`, which expects a JSON Object (`{...}`).

## Changes Made
Modified `BloomSalesService.java` to correctly handle JSON Arrays:

**Before (Incorrect):**
```java
updateData.put("flowerSaleItems", Document.parse(flowerItems.toString())); 
// Fails because flowerItems.toString() is "[...]" (Array) but Document.parse expects "{...}" (Document)
```

**After (Correct):**
```java
List<Document> flowerSaleItemsList = new ArrayList<>();
for (int i = 0; i < flowerItems.length(); i++) {
    flowerSaleItemsList.add(Document.parse(flowerItems.getJSONObject(i).toString()));
}
updateData.put("flowerSaleItems", flowerSaleItemsList);
```

## Verification
- Verified that `flowerItems` is iterated correctly.
- Verified that each item is parsed individually as a `Document`.
- Verified that the list of documents is put into the update data.

This ensures proper compatibility with MongoDB's BSON structure requirements.
