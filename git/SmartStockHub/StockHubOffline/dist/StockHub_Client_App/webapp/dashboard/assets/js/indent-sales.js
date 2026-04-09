async function loadBARCounterProducts(storeRoom) {
    const response = await getRequest(getHost()+`/salescounter`);    

    var productMap = {};
    storeRoom.productList.forEach(product => {
        productMap[product.SKU] = product;
    });

    // Update salesCounter productList
    response.productList = response.productList.map(product => {
        let storeProduct = productMap[product.SKU];
        if (storeProduct) {
            return {
                ...product,
                salePrice: storeProduct.salePrice,
                profitAmount: storeProduct.profitAmount,
                purchasePrice: storeProduct.purchasePrice,
                category: storeProduct.category,
                brand: storeProduct.brand,
                purchaseStock : product.indentStock
            };
        }
        return product;
    });
    return response;
}