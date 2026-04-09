$(document).ready(function () {
	console.log("Account Page is fully loaded!");
	init();
});



async function init() {
	updateShopName();

	// Ensure Product Master is loaded for Base Price calc
	try {
		const pmResponse = await getRequest(productMasterurl);
		if (pmResponse && pmResponse.status === "success") {
			localStorage.setItem('products', JSON.stringify(pmResponse.data));
			console.log("Product Master loaded for Accounts calculation.");
		}
	} catch (e) { console.error("Failed to load Product Master", e); }

	const response = await getRequest(salesurl);
	//const response = JSON.parse(``);
	showSalesDetails(response);
}

let salesTable = null;

async function showSalesDetails(response) {
	const data = constructSalesList(response).sort((a, b) => a[0] - b[0]);

	// Convert rows
	const tableRows = data.map(row => {
		const date = new Intl.DateTimeFormat('en-GB').format(new Date(row[0] * 1000));
		const sales = row[1];
		const base = row[2];
		const profit = Math.ceil(row[3]);
		const id = row[4];

		return [
			date,
			sales,
			base,
			profit,
			`
            <div class="d-flex gap-2">
                <a href="#!" onclick="loadSalesDetails('${id}')" class="btn btn-light btn-sm"
                    data-bs-toggle="modal" data-bs-target="#exampleModalXl">
                    <iconify-icon icon="solar:eye-broken"></iconify-icon>
                </a>
                <a href="#!" onclick="printDailyReport('${id}')" class="btn btn-light btn-sm">
                    <iconify-icon icon="solar:clipboard-text-bold-duotone"></iconify-icon>
                </a>
            </div>
            `
		];
	});

	// If table not created → create
	if (!salesTable) {
		salesTable = $('#salesTable').DataTable({
			data: tableRows,
			columns: [
				{ title: "Date" },
				{ title: "Sales Amount" },
				{ title: "Base Price" },
				{ title: "Sales Profit" },
				{ title: "View" }
			],
			paging: true,
			searching: true,
			ordering: true,
			pageLength: 20,
			responsive: true,

			// Export buttons
			dom: 'Bfrtip',
			buttons: [
				'csv',
				'excel',
				{
					extend: 'pdf',
					title: 'Sales Report'
				},
				'print'
			]
		});

	} else {
		// Clear + Add new rows
		salesTable.clear();
		salesTable.rows.add(tableRows).draw();
	}
}

function constructSalesList(response) {
	// 1. Get Product Master map from localStorage to find Cost Price
	const productMasterRaw = localStorage.getItem('products');
	let productMap = {};
	if (productMasterRaw) {
		try {
			const pm = JSON.parse(productMasterRaw);
			(pm.productList || []).forEach(p => {
				productMap[p.SKU] = parseFloat(p.purchasePrice) || 0;
			});
		} catch (e) { console.error("Error parsing product master", e); }
	}

	var tableList = [];
	for (var i in response.data) {
		var item = response.data[i];
		var tableRow = [];
		tableRow.push(item.timeCreatedAt);
		tableRow.push(item.totalSalesAmount);

		// Calculate Base Price dynamically (Sum of Cost * SalesQty)
		let calculatedBasePrice = 0;
		if (item.productList && Array.isArray(item.productList)) {
			item.productList.forEach(p => {
				const cost = productMap[p.SKU] || 0;
				const qty = parseFloat(p.sales) || 0;
				calculatedBasePrice += (cost * qty);
			});
		} else if (item.basePrice != null) {
			calculatedBasePrice = item.basePrice;
		}

		var basePrice = Math.round(calculatedBasePrice);
		tableRow.push(basePrice);

		var profitAmount = item.totalSalesAmount - basePrice;
		tableRow.push(Math.round(profitAmount));

		var id = item._id;
		if (id && typeof id === 'object' && id.$oid) {
			id = id.$oid;
		}
		tableRow.push(id);
		tableList.push(tableRow);
	}
	return tableList;
}

async function loadDateWiseReport() {
	const startDate = document.getElementById('accounts-startDate').value;
	const endDate = document.getElementById('accounts-endDate').value;
	const startTimestamp = Math.floor(new Date(startDate).getTime() / 1000);
	const endTimestamp = Math.floor(new Date(endDate).getTime() / 1000);

	if (!startDate || !endDate) {
		alert("Please select both dates before fetching Sales Report.");
		return;
	}
	const response = await getRequest(salesurl + "?startDate=" + startTimestamp + "&endDate=" + endTimestamp);
	showSalesDetails(response);
}

async function loadSalesDetails(id) {
	const response = await getRequest(salesurl + "/id?id=" + id);
	var html = '<table class="table table-centered"><thead><tr><th scope="col">SKU</th><th scope="col">Sales</th><th scope="col">Profit</th></tr></thead>';
	html += '<tbody>';
	for (var i in response.data.productList) {
		var item = response.data.productList[i];
		if (item.sales != 0) {
			html += '<tr>';
			html += '<td>' + item.SKU + '</td>';
			html += '<td>' + item.sales + '</td>';
			html += '<td>' + item.profitAmount + '</td>';
			html += '</tr>';
		}
	}
	html += '</tbody></table>';
	$("#sales-details-table").html(html);
}

async function printDailyReport(id) {
	const response = await getRequest(salesurl + "/id?id=" + id);
	var html = template;
	html = html.replace('{saleDate}', convertUnixToDate(response.data.saleDate));
	html = html.replace('{topSaleAmount}', formatCurrency(response.data.totalSalesAmount));
	html = html.replace('{bottomSaleAmount}', formatCurrency(response.data.totalSalesAmount));

	const totalSale = response.data.productList.reduce((sum, product) => sum + product.sales, 0);
	html = html.replace('{topBottleSales}', formatCurrency(totalSale));

	const closingAmount = response.data.productList.reduce((sum, product) => sum + (product.closingStock * product.salePrice), 0);
	html = html.replace('{topClosingAmount}', formatCurrency(closingAmount));
	html = html.replace('{bottomClosingAmount}', formatCurrency(closingAmount));

	const openingAmount = response.data.productList.reduce((sum, product) => sum + (product.openingStock * product.salePrice), 0);
	html = html.replace('{topOpeningAmount}', formatCurrency(openingAmount));
	html = html.replace('{bottomOpening}', formatCurrency(openingAmount));

	const purchaseAmount = response.data.productList.reduce((sum, product) => sum + (product.purchaseStock * product.salePrice), 0);
	html = html.replace('{topPurchaseAmount}', formatCurrency(purchaseAmount));
	html = html.replace('{bottomPurchase}', formatCurrency(purchaseAmount));


	html = html.replace('{topProfit}', formatCurrency(response.data.profitAmount));
	html = html.replace('{topExpenses}', formatCurrency(response.data.totalExpensesAmount));

	const totalItemAmount = response.data.productList.reduce((sum, product) => sum + (product.stock * product.salePrice), 0);
	html = html.replace('{bottomTotal}', formatCurrency(totalItemAmount));

	const openingAmountinMRP = response.data.productList.reduce((sum, product) => sum + (product.openingStock * getPurchasePrice(product.SKU)), 0);
	html = html.replace('{openingAmountinMRP}', formatCurrency(openingAmountinMRP));

	const purchaseAmountinMRP = response.data.productList.reduce((sum, product) => sum + (product.purchaseStock * getPurchasePrice(product.SKU)), 0);
	html = html.replace('{purcchaseAmountinMRP}', formatCurrency(purchaseAmountinMRP));

	const totalStockAmountinMRP = response.data.productList.reduce((sum, product) => sum + (product.stock * getPurchasePrice(product.SKU)), 0);
	html = html.replace('{totalAmountinMRP}', formatCurrency(totalStockAmountinMRP));

	const saleAmountinMRP = response.data.productList.reduce((sum, product) => sum + (product.sales * getPurchasePrice(product.SKU)), 0);
	html = html.replace('{salesAmountinMRP}', formatCurrency(saleAmountinMRP));

	const closingAmountinMRP = response.data.productList.reduce((sum, product) => sum + (product.closingStock * getPurchasePrice(product.SKU)), 0);
	html = html.replace('{closingAmountinMRP}', formatCurrency(closingAmountinMRP));

	var expDetails = "";
	for (var i in response.data.expenseList) {
		var item = response.data.expenseList[i];
		expDetails += '<tr><td style="border: 1px solid #000;">' + item.details + '</td><td  style="border: 1px solid #000;">' + item.amount + '</td></tr>';
	}
	if (formatCurrency(response.data.totalExpensesAmount) !== 0 && formatCurrency(response.data.totalExpensesAmount) !== null) {
		expDetails += '<tr><td>TOTAL</td><td><b>' + formatCurrency(response.data.totalExpensesAmount) + '</b></td></tr>';

	}
	html = html.replace('{expBody}', expDetails);

	if (hasKeyInLocalStorage('isBankBlanceEnabled')) {
		var bankBalance = 0; var cashinHandAMT = 0; var tasmacBalance = 0;

		var row = `<tr><th>Closing Stock :</th><td>` + formatCurrency(closingAmountinMRP) + `</td></tr>`;
		if (response.data.bankBalance !== undefined && response.data.bankBalance !== null) {
			bankBalance = parseInt(response.data.bankBalance);
			if (response.data.payments?.gpay) {
				bankBalance += Number(response.data.payments.gpay) || 0;
			}
			if (response.data.payments?.card) {
				bankBalance += Number(response.data.payments.card) || 0;
			}
			row += `<tr><th>Bank Balance :</th><td>` + formatCurrency(bankBalance) + `</td></tr>`;
		}
		if (response.data.cashInHand !== undefined && response.data.cashInHand !== null) {
			cashinHandAMT = parseInt(response.data.cashInHand);
			row += `<tr><th>Cash In Hand :</th><td>` + formatCurrency(cashinHandAMT) + `</td></tr>`;
		}
		if (response.data.tasmacBalance !== undefined && response.data.tasmacBalance !== null) {
			tasmacBalance = parseInt(response.data.tasmacBalance);
			row += `<tr><th>TASMAC Balance :</th><td>` + formatCurrency(tasmacBalance) + `</td></tr>`;
		}
		row += `<tr><th>Total :</th><td>` + formatCurrency(closingAmountinMRP + bankBalance + cashinHandAMT + tasmacBalance) + `</td></tr>`;
		const totalBottlesInHand = response.data.productList.reduce((sum, product) => sum + product.closingStock, 0);
		row += `<tr><th>Bottles in Hand :</th><td>` + totalBottlesInHand + `</td></tr>`;

		html = html.replace('{cashFlowTable}', row);
	} else {
		var row = `<tr><th>Gpay:</th><td>` + (response.data.payments?.gpay || '0') + `</td></tr>`;
		row += `<tr><th>Card:</th><td>` + (response.data.payments?.card || '0') + `</td></tr>`;
		row += `<tr><th>Kitchen :</th><td>` + (response.data.kitchenSales || '0') + `</td></tr>`;
		row += `<tr><th>Cash:</th><td>` + (response.data.finalCashSettlement || '0') + `</td></tr>`;
		html = html.replace('{cashFlowTable}', row);
	}

	let denomHtml = "";
	if (response.data.denomination && Object.keys(response.data.denomination).length > 0) {
		denomHtml += '<table class="print-table"><thead><tr><th colspan="2">Denomination</th></tr></thead><tbody>';
		let total = 0;
		const denoms = Object.keys(response.data.denomination).sort((a, b) => b - a);
		for (let key of denoms) {
			let count = response.data.denomination[key];
			let val = key * count;
			total += val;
			denomHtml += `<tr><th>${key} x ${count}:</th><td>${val}</td></tr>`;
		}
		denomHtml += `<tr><th>Total:</th><td>${total}</td></tr>`;
		denomHtml += '</tbody></table>';
	}
	html = html.replace('{denominationTable}', denomHtml);

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

	let summaryBody = "";
	for (const [key, totalSold] of Object.entries(summary)) {
		summaryBody += `<tr>
	                        <td style="border: 2px solid #000; padding: 8px;">${key}</td>
	                        <td style="border: 2px solid #000; padding: 8px;">${totalSold}</td>
	                    </tr>`;
	}
	if (totalSale !== 0 && totalSale !== null) {
		summaryBody += '<tr><td>TOTAL</td><td><b>' + formatCurrency(totalSale) + '</b></td></tr>';

	}

	html = html.replace('{salesSummaryQTY}', summaryBody);

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

function formatCurrency(amount) {
	const formattedAmount = new Intl.NumberFormat('en-IN', {
		minimumFractionDigits: 2, // At least two decimal places
		maximumFractionDigits: 2  // At most two decimal places
	}).format(amount);
	return formattedAmount;
}

function getPurchasePrice(SKU) {
	const productMaster = localStorage.getItem('products');
	const products = productMaster ? JSON.parse(productMaster) : null;
	for (var i in products.productList) {
		var item = products.productList[i];
		if (item.SKU == SKU) {
			return item.purchasePrice;
		}
	}
	return 0;
}

var template = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Report</title>
    <style>
        body { font-family: Arial, sans-serif; position: relative;}
		.tables-wrapper { display: flex; gap: 40px; }
        .print-table {flex: 1;border: 2px solid #000;border-collapse: separate;}
        .print-table th, .print-table td {padding: 8px 12px;text-align: right;}
        .print-table th, .cash-flow-table th {text-align: left;}
        .print-section {margin: 20px;}
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
	                    <th>Sales Amount:</th>
	                    <td>{topSaleAmount}</td>
	                </tr>
	                <tr>
	                    <th>Closing Stock Amount:</th>
	                    <td>{topClosingAmount}</td>
	                </tr>
	                <tr>
	                    <th>Total Bottle Sales:</th>
	                    <td>{topBottleSales}</td>
	                </tr>
					<tr>
	                    <th>Snacks Purchase:</th>
	                    <td>{topProfit}</td>
	                </tr>
					<tr>
	                    <th>Expenses:</th>
	                    <td>{topExpenses}</td>
	                </tr>
	            </thead>
	        </table>
			 <table class="print-table cash-flow-table" id="cashFlowTable">
		        <thead>
		            <tr><th colspan="2">Cash Flow</th></tr>
		        </thead>
		        <tbody>
					{cashFlowTable}
		        </tbody>
		    </table>
            {denominationTable}
		</div>
		<div style="padding-top:60px;text-align: center;">
		            <div><span><b>Total Liquor Summary in Purchase Price</b></span></div><br>
		            <table style="width:100%;border: 2px solid #000; border-collapse: collapse;height: 80px;">
		                <thead style="height: 40px;">
		                    <tr>
		                        <th style="border: 1px solid #000;">Liquor Opening Stock</th>
		                        <th style="border: 1px solid #000;">Liquor Purchase</th>
		                        <th style="border: 1px solid #000;">Liquor Total Stock</th>
		                        <th style="border: 1px solid #000;">Liquor Sales</th>
		                        <th style="border: 1px solid #000;">Liquor Stock</th>
		                    </tr>
		                </thead>
		            <tbody>
		                <tr>
		                    <td style="border: 1px solid #000;">{openingAmountinMRP}</td>
		                    <td style="border: 1px solid #000;">{purcchaseAmountinMRP}</td>
		                    <td style="border: 1px solid #000;">{totalAmountinMRP}</td>
		                    <td style="border: 1px solid #000;">{salesAmountinMRP}</td>
		                    <td style="border: 1px solid #000;">{closingAmountinMRP}</td>
		                </tr>
		            </tbody>
		            </table>
		        </div>


		        <div style="padding-top:60px;text-align: center;">
		            <div><span><b>GRAND TOTAL SUMMARY in Sale Price</b></span></div><br>
		            <table style="width:100%;border: 2px solid #000; border-collapse: collapse;height: 80px;">
		                <thead style="height: 40px;">
		                    <tr>
		                        <th style="border: 1px solid #000;">Opening</th>
		                        <th style="border: 1px solid #000;">Purchase</th>
		                        <th style="border: 1px solid #000;">Total</th>
		                        <th style="border: 1px solid #000;">Sales</th>
		                        <th style="border: 1px solid #000;">Closing</th>
		                    </tr>
		                </thead>
		            <tbody>
		                <tr>
		                    <td style="border: 1px solid #000;">{bottomOpening}</td>
		                    <td style="border: 1px solid #000;">{bottomPurchase}</td>
		                    <td style="border: 1px solid #000;">{bottomTotal}</td>
		                    <td style="border: 1px solid #000;">{bottomSaleAmount}</td>
		                    <td style="border: 1px solid #000;">{bottomClosingAmount}</td>
		                </tr>
		            </tbody>
		            </table>
		        </div>
				<div style="display: flex; gap: 20px;padding-top:30px;">
					<table style="width:50%;border: 2px solid #000; border-collapse: collapse;">
						<thead>
							<tr><th>Details</th><th>Amount</th>
						</thead>
						<tbody>
							{expBody}
						</tbody>
					</table>
					<table style="width: 40%; border: 2px solid #000; border-collapse: collapse;">
				        <thead>
				            <tr>
				                <th style="border: 2px solid #000; padding: 8px;">Category</th>
				                <th style="border: 2px solid #000; padding: 8px;">Total Sold</th>
				            </tr>
				        </thead>
				        <tbody>
				            {salesSummaryQTY}
				        </tbody>
				    </table>
				</div>
    </div>
</body>
</html>
`;
