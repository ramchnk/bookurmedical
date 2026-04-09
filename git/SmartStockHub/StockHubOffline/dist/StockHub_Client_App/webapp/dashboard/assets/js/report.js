async function printDailyReport(id) {
  	const response = await getRequest(salesurl + "/id?id=" + id);	  		
	var html = reptemplate;
	html = html.replace('{saleDate}', convertUnixToDate(response.data.saleDate));
	html = html.replace('{topSaleAmount}', formatCurrency(response.data.totalSalesAmount));


	const closingAmount = response.data.productList.reduce((sum, product) => sum + (product.closingStock * product.salePrice), 0);
	html = html.replace('{topClosingAmount}', formatCurrency(closingAmount));

	const openingAmount = response.data.productList.reduce((sum, product) => sum + (product.openingStock * product.salePrice), 0);
	html = html.replace('{topOpeningAmount}', formatCurrency(openingAmount));

	const purchaseAmount = response.data.productList.reduce((sum, product) => sum + (product.purchaseStock * product.salePrice), 0);
	html = html.replace('{topPurchaseAmount}', formatCurrency(purchaseAmount));
	

	html = html.replace('{topExpenses}', formatCurrency(response.data.totalExpensesAmount));

	html = html.replace('{scanpay}', formatCurrency((response.data.payments?.gpay || '0')));
	html = html.replace('{cardpay}', formatCurrency((response.data.payments?.card || '0')));
	html = html.replace('{cashpay}', formatCurrency((response.data.finalCashSettlement || '0')));

	var expDetails = "";
	for(var i in response.data.expenseList){
		var item = response.data.expenseList[i];
		expDetails += '<tr><td style="border: 1px solid #000;">'+item.details+'</td><td  style="border: 1px solid #000;">'+item.amount+'</td></tr>';
	}
	html = html.replace('{expBody}', expDetails);


	const filteredList = response.data.productList.filter(product => product.SKU);

	const summary = filteredList.reduce((acc, product) => {
	    const mlValue = product.SKU.split("-").pop(); // Extract ML value from SKU
	    
	    // Group BEER & WINE into a single category
	    if (product.category === "BEER" || product.category === "WINE") {
	        acc[product.category] = (acc[product.category] || 0) + product.sales;
	    } else {
	        acc[mlValue] = (acc[mlValue] || 0) + product.sales;
	    }
	    
	    return acc;
	}, {});

	const newWindow = window.open("", "_blank");
	if (newWindow) {
	     newWindow.document.write(html);
	     newWindow.document.close();
	     newWindow.print();
	}
}

function convertUnixToDate(unixTime) {
    const date = new Date(unixTime * 1000);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0'); 
    const year = date.getFullYear(); 
    return `${day}/${month}/${year}`;
}

function formatCurrency(amount){
	const formattedAmount = new Intl.NumberFormat('en-IN', {
	    minimumFractionDigits: 2, // At least two decimal places
	    maximumFractionDigits: 2  // At most two decimal places
	}).format(amount);
	return formattedAmount;
}

var reptemplate = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Report</title>
    <style>
        body { font-family: Arial, sans-serif; position: relative;}
		.tables-wrapper { display: flex; gap: 40px; }
        .print-table {width: 70%;border: 2px solid #000;border-collapse: separate;}
        .print-table th, .print-table td {padding: 8px 12px;text-align: right;}
        .print-table th, .cash-flow-table th {text-align: left;}
        .print-section {margin: 20px;padding-left:10%;}
        .print-button {margin-top: 20px;display: block;margin-left: auto;margin-right: auto;}
        .date-display {position: absolute;top: 10px;right: 20px;font-size: 14px;font-weight: bold;}
    </style>
</head>
<body>
    <!-- Date Display -->
    <div class="date-display" id="current-date">Date : {saleDate}</div>

    <div class="print-section">
		<div class="tables-wrapper">
	        <table class="print-table">
	            <thead>
	                <tr>
	                    <th>Opening Stock Amount:</th>
	                    <td>{topOpeningAmount}</td>
	                </tr>
	                <tr>
	                    <th>Purchase Amount:</th>
	                    <td>{topPurchaseAmount}</td>
	                </tr>
	                <tr>
	                    <th>Closing Stock Amount:</th>
	                    <td>{topClosingAmount}</td>
	                </tr>
					<tr>
	                    <th>Sales Amount:</th>
	                    <td>{topSaleAmount}</td>
	                </tr>
					<tr>
	                    <th>SCAN PAY:</th>
	                    <td>{scanpay}</td>
	                </tr>
					<tr>
	                    <th>Card PAY:</th>
	                    <td>{cardpay}</td>
	                </tr>
					<tr>
	                    <th>Expenses:</th>
	                    <td>{topExpenses}</td>
	                </tr>
					<tr>
	                    <th>Cash:</th>
	                    <td>{cashpay}</td>
	                </tr>
	            </thead>
	        </table>
		</div>
		<div style="padding-top:60px;"><span>Expenses Details</span></div>
		<div style="text-align: center;">
				<div style="display: flex; gap: 20px;padding-top:30px;">
					<table style="width:70%;border: 2px solid #000; border-collapse: collapse;">
						<thead>
							<tr><th>Details</th><th>Amount</th>
						</thead>
						<tbody>
							{expBody}
						</tbody>
					</table>
				</div>
    </div>
</body>
</html>
`;