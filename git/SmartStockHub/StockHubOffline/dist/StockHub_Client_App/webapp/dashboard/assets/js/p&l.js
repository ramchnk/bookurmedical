$(document).ready(function() {
    console.log("Product Page is fully loaded!");
    updateShopName();
});
let url = `${webmanagerurl}/pandl`;
async function init() {
    const startDate = document.getElementById('pl-startDate').value;
    const endDate = document.getElementById('pl-endDate').value;
    const startTimestamp = Math.floor(new Date(startDate).getTime() / 1000);
    const endTimestamp = Math.floor(new Date(endDate).getTime() / 1000);
    
    if (!startDate || !endDate) {
        alert("Please select both dates before fetching Report.");
        return;
    }

    const maxDays = 30;
    const timeDiff = (endDate - startDate) / (1000 * 60 * 60 * 24);
    if (timeDiff > maxDays) {
        alert(`Date range cannot exceed ${maxDays} days.`);
        endDateInput.value = ''; 
        return;
    }

    const response = await getRequest(url+"?fromTime="+startTimestamp+"&toTime="+endTimestamp);
    const productMaster = localStorage.getItem('products');  
    const products = productMaster ? JSON.parse(productMaster) : null;
    var tbdy = ``;
    var sno = 1;
    var totalOpeningStock = 0;
    var totalPurchaseStock = 0;
    var totalSoldQty = 0;
    var totalSoldCost = 0;
    var totalClosingStock = 0;
    var totalOpeningValueSP = 0;
    var totalReceivedValueSP = 0;
    var totalClosingValueSP = 0;
    var totalOpeningValue = 0;
    var totalPurchaseValue = 0;
    var totalPurchaseCost = 0;
    var totalProfitMargin = 0;
    var totalClosingValue = 0;

    for (var i in products.productList) {
        var item = products.productList[i];
        var runningSKU = item.SKU;
        var openingStock = getOpeningStock(runningSKU, response);
        var purchaseStock = getPurchaseStock(runningSKU, response);
        var soldQty = getSoldQty(runningSKU, response);
        var closingStock = getClosingStock(runningSKU, response);
        var soldCost = soldQty * item.salePrice;
        var purchaseCost = soldQty * item.purchasePrice;
        var profitMargin = (soldCost - purchaseCost).toFixed(2);
        var profitPercentage = soldCost / purchaseCost * 100 - 100;

        totalOpeningStock += openingStock;
        totalPurchaseStock += purchaseStock;
        totalSoldQty += soldQty;
        totalSoldCost += soldCost;
        totalClosingStock += closingStock;
        totalOpeningValueSP += openingStock * item.salePrice;
        totalReceivedValueSP += purchaseStock * item.salePrice;
        totalClosingValueSP += closingStock * item.salePrice;
        totalOpeningValue += openingStock * item.purchasePrice;
        totalPurchaseValue += purchaseStock * item.purchasePrice;
        totalPurchaseCost += purchaseCost;
        totalProfitMargin += parseFloat(profitMargin);
        totalClosingValue += closingStock * item.purchasePrice;

        tbdy += `<tr>
            <td>${runningSKU}</td>
            <td>${openingStock}</td>
            <td>${purchaseStock}</td>
            <td>${soldQty}</td>
            <td>${item.salePrice}</td>
            <td>${(soldQty * item.salePrice).toFixed(2)}</td>
            <td>${closingStock}</td>
            <td>${(openingStock * item.salePrice).toFixed(2)}</td>
            <td>${(purchaseStock * item.salePrice).toFixed(2)}</td>
            <td>${(closingStock * item.salePrice).toFixed(2)}</td>
            <td>${item.purchasePrice}</td>
            <td>${(openingStock * item.purchasePrice).toFixed(2)}</td>
            <td>${(purchaseStock * item.purchasePrice).toFixed(2)}</td>
            <td>${(soldQty * item.purchasePrice).toFixed(2)}</td>
            <td>${profitMargin}</td>
            <td>${((profitPercentage || 0).toFixed(2))}</td>
            <td>${(closingStock * item.purchasePrice).toFixed(2)}</td>
        </tr>`;
    }

    tbdy += `<tr class="table-success">
        <td><strong>Total</strong></td>
        <td>${totalOpeningStock}</td>
        <td>${totalPurchaseStock}</td>
        <td>${totalSoldQty}</td>
        <td></td>  <!-- Sale Price Placeholder -->
        <td>${totalSoldCost.toFixed(2)}</td>
        <td>${totalClosingStock}</td>
        <td>${totalOpeningValueSP.toFixed(2)}</td>
        <td>${totalReceivedValueSP.toFixed(2)}</td>
        <td>${totalClosingValueSP.toFixed(2)}</td>
        <td></td>  <!-- Purchase Price Placeholder -->
        <td>${totalOpeningValue.toFixed(2)}</td>
        <td>${totalPurchaseValue.toFixed(2)}</td>
        <td>${totalPurchaseCost.toFixed(2)}</td>
        <td>${totalProfitMargin.toFixed(2)}</td>
        <td></td>
        <td>${totalClosingValue.toFixed(2)}</td>
    </tr>`;

    $('#pl-table').html(tbdy);
    if ($.fn.DataTable.isDataTable('#pl-report-table')) {
        $('#pl-report-table').DataTable().destroy(); 
    }

    $('#pl-report-table').DataTable({
        scrollY: '600px', 
        scrollCollapse: true,
        paging: false,
        searching: true,
        ordering: false,
        info: true,
        lengthChange: true,
        autoWidth: false,
        dom: 'Bfrtip',
        buttons: [
            'copy', 'csv', 'excel', 'pdf', 'print'
        ]
    });
}

function getOpeningStock(SKU, sales){
    var startDateSales = sales.data[0];
    for(var i in startDateSales.productList){
        if(startDateSales.productList[i].SKU == SKU){
            return startDateSales.productList[i].openingStock;
        }
    }
    return 0;
}

function getClosingStock(SKU, sales){
    var endDateSales = sales.data[sales.data.length - 1];
    for(var i in endDateSales.productList){
        if(endDateSales.productList[i].SKU == SKU){
            return endDateSales.productList[i].closingStock;
        }
    }
    return 0;
}

function getPurchaseStock(SKU, sales){
    var purchaseStock = 0;
    for(var i in sales.data){
        var productList = sales.data[i].productList;
        for(var j in productList){
            if(productList[j].SKU == SKU){
                purchaseStock += productList[j].purchaseStock;
                break;
            }
        }
    }
    return purchaseStock;
}

function getSoldQty(SKU, sales){
    var salesQty = 0;
    for(var i in sales.data){
        var productList = sales.data[i].productList;
        for(var j in productList){
            if(productList[j].SKU == SKU){
                salesQty += productList[j].sales;
                break;
            }
        }
    }
    return salesQty;
}
