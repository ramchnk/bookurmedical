async function printDailyChart(needToShowStock){
	const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
	const response = await getRequest(productMasterurl);
//	const response = JSON.parse(``);
	var page = template.replace('{shopName}', accountInfo.shopName);
	const currentDate = new Date().toLocaleDateString(); 
	page = page.replace('{currentDate}', currentDate);
	var data = await constructList(response);
    var afterPurchaseHasSale = response?.afterPurchaseHasSale ?? true;

	var tbody = '';
	for(var i in data){
		var item = data[i];
        const stock = parseFloat(item[3]); 
        const purchaseStock = parseFloat(item[2]); 
        const openingStock = parseFloat(item[1]); 
        let ob;
        if (!afterPurchaseHasSale) {
             if (openingStock + purchaseStock == stock) {
                ob = openingStock;
             }else{
                ob = stock;
             }
        }else{
            ob = stock; 
        }
        let purchase = 0;
        if (!afterPurchaseHasSale) {
             if (openingStock + purchaseStock === stock) {
                purchase = purchaseStock;
             }
        }
		tbody += '<tr><td>' + item[0] + '</td><td>' + ob + '</td><td>'+purchase+'</td><td>'+stock+'</td><td></td><td></td><td>X ' + item[6] + '</td><td></td></tr>';
	}
	page = page.replace('{tableBody}', tbody);
	const newWindow = window.open("", "_blank");
	if (newWindow) {
	     newWindow.document.write(page);
	     newWindow.document.close();
	     newWindow.print();
	}
    newWindow.close();
}

var template = '<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"><title>Daily Chart</title><style>'+
                'body {font-family: Arial, sans-serif;margin: 0;padding: 0;background-color: #fff;}'+
                '@page {size: A4;margin: 5mm;}'+
                '.container {width: 100%;max-width: 100%;margin: 0 auto;padding: 7mm;box-sizing: border-box;page-break-inside: avoid;}'+
                '.title {text-align: center;font-size: 18px;font-weight: bold;margin-bottom: 5px;}'+
                '.subtitle {text-align: center;font-size: 18px;margin-bottom: 10px;}'+
                '.info {font-size: 16px;margin-bottom: 10px;line-height: 1.5;}'+
                '.record-table {width: 100%;border-collapse: collapse;margin-top: 10px;}'+
                '.record-table th, .record-table td {border: 1px solid #000;text-align: center;padding: 5px;font-size: 12px;vertical-align: middle;word-wrap: break-word;}'+
                '.record-table th {background-color: #f0f0f0;font-weight: bold;}'+
                '.record-table td {height: 25px;}'+
                '.record-table thead th:nth-child(1) {width: 20%;}'+
                '.record-table thead th:nth-child(2),'+
                '.record-table thead th:nth-child(3),'+
                '.record-table thead th:nth-child(4),'+
                '.record-table thead th:nth-child(5) {width: 10%;}'+
                '.record-table thead th:nth-child(6) {width: 10%;}'+
                '.d-flex {display: flex;}'+
                '.justify-content-between {justify-content: space-between;}'+
                '.align-items-center {align-items: center;}'+
                '@media print {body {    background-color: #fff;    -webkit-print-color-adjust: exact;}.container {page-break-inside: avoid;}.record-table th {background-color: #e0e0e0 !important;}}'+
                '</style></head><body>'+
                '<div class="container">'+
                ' <div class="d-flex justify-content-between align-items-center">'+
                '    <h3 class="title">{shopName}</h3>'+
                	'<b><span>{currentDate}</span></b>'+
                '</div>'+
                '    <table class="record-table">'+
                '        <thead>'+
                '            <tr>'+
                '                <th>Item</th>'+
                '                <th>OB</th>'+
                '                <th>Purchase</th>'+
                '                <th>Total</th>'+
                '                <th>CB</th>'+
                '                <th>Sales</th>'+
                '                <th>Price</th>'+
                '                <th>Amount</th>'+
                '            </tr>'+
                '        </thead>'+
                '        <tbody>{tableBody}</tbody></table></div></body></html>';