async function printMemberSalesBills(){
		const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    var memberSalesShopName = accountInfo.shopName;
    var memberSalesLicenceNo = accountInfo.licenceNO;
    var cityName = accountInfo.city ?? "";
    var body = '<div class="page">';
    var salesList = memberSales.data[0];
    
    var salesDate = new Date(salesList.date * 1000);
    var formattedDate = salesDate.toLocaleDateString('en-IN');
    for(var i in salesList.saleDetails){
    	var billNumber = parseInt(i)+1;
    	var obj = salesList.saleDetails[i];
    	const totalAmount = obj.purchasedItems.reduce((sum, item) => {
		    return sum + item.salePrice * item.qty;
		  }, 0);
    	var bill = `<div class="bill">
              <center><h4>`+memberSalesShopName+`</h4></center>
              <center><p>${cityName}</p></center>
              <center><strong>Li. No.</strong> FL2-`+memberSalesLicenceNo+`</center>

              <p><strong>Name:</strong> `+obj.name+` (M.No.-${obj.id})</p>
              <div style="display:flex; justify-content:space-between; font-size:12px; margin:2px 0">
                <span><strong>Bill No.</strong> ${billNumber}</span>
                <span><strong>Date</strong> ${formattedDate}</span>
              </div>

              <table>
                <thead><tr><th>Item</th><th>Qty</th><th>Price</th><th>Total</th></tr></thead>
                <tbody>
                  ${obj.purchasedItems.map(item =>
                    `<tr><td>${item.SKU}</td><td>${item.qty}</td><td>${item.salePrice.toFixed(2)}</td><td>${item.salePrice.toFixed(2) * item.qty}</td></tr>`
                  ).join('')}
                </tbody>
              </table>

              <p style="text-align:right"><strong>Total: ₹${totalAmount.toFixed(2)}</strong></p>
            </div>`;
            body += bill;
    }
   	body += `</div>`;
    var page = memberSaleTemplate;
    page = page.replace('{body}', body);
    // Open in a new window and render the template content
         const newWindow = window.open("", "_blank");
         if (newWindow) {
             newWindow.document.write(page);
             newWindow.document.close();
             newWindow.print();
         }
}

var memberSaleTemplate = `<!DOCTYPE html>
			<html lang="en">
			<head>
			  <meta charset="UTF-8">
			  <meta name="viewport" content="width=device-width, initial-scale=1.0">
			  <title>A4 Liquor Register</title>
			  <style>
			    body { font-family: monospace; margin: 0; padding: 0; }
			    .page {
			      width: 210mm; height: 297mm; padding: 10mm;
			      display: grid;
			      grid-template-columns: 1fr 1fr;
			      grid-template-rows: repeat(4, 1fr);
			      gap: 5mm;
			      box-sizing: border-box;
			      page-break-after: always;
			    }
			    .bill {
			      border: 1px dashed black;
			      padding: 5mm;
			      box-sizing: border-box;
			      display: flex;
			      flex-direction: column;
			      justify-content: space-between;
			      font-size: 12px;
			      height: 100%;
			    }
			    .bill p, .bill h4 {
			      margin: 2px 0;
			      font-size: 12px;
			    }
			    .bill table {
			      width: 100%;
			      border-collapse: collapse;
			      margin-top: 5px;
			      font-size: 11px;
			    }
			    .bill th, .bill td {
			      border: none;
			      border-bottom: 1px solid #ccc;
			      padding: 2px;
			      text-align: center;
			    }
			    .bill thead th {
			      border-top: 1px solid #000;
			      border-bottom: 1px solid #000;
			    }
			    #print-btn {
			      margin: 20px;
			      padding: 10px 20px;
			      font-size: 16px;
			    }
			    @media print {
			      #print-btn { display: none; }
			    }
			  </style>
			</head>
			<body>
			  {body}
			</body>
			</html>
`; 