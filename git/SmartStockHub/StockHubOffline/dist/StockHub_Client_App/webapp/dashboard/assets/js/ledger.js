async function printLedger(invoiceID){
   	const response = await getRequest(salesurl + "/id?id=" + invoiceID);
 // 	const response = JSON.parse(``);
  	const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    var page = ledgerTemplate;
    if(accountInfo?.unitNoteFormat?.toLowerCase() === "tirupur"){
	  		page = tirupurTemplate;		
	  }else if(accountInfo?.unitNoteFormat?.toLowerCase() === "cbe"){
	  		page = cbeTemplate;		
	  }else if(accountInfo?.unitNoteFormat?.toLowerCase() === "pudukottai"){
	  		page = pudukottaiTemplate;		
	  }
  	page = page.replace('{shopName}', accountInfo.shopName);
    page = page.replace('{licenseNo}', accountInfo.licenceNO);
  	const date = new Date(response.data.timeCreatedAt * 1000); 
	const formattedDate = date.toLocaleDateString(); 
	page = page.replace('{date}', formattedDate);
  page = page.replace('{image-shopNumber}', accountInfo.shopNumber);

  var categories = groupCategories(response);
  page = page.replace(/\{(\w+)-(\w+)\}/g, (_, key, category) => {
		const value = categories[category]?.[key] || "-";
	  	return value;
	});
  
  var totClosingBal = Math.round(
	  ((categories['BEER']?.closingBalance || 0) / 7800) +
	  ((categories['WINE']?.closingBalance || 0) / 2250) +
	  (
	    (categories['WHISKY']?.closingBalance || 0) +
	    (categories['BRANDY']?.closingBalance || 0) +
	    (categories['RUM']?.closingBalance || 0) +
	    (categories['VODKA']?.closingBalance || 0)
	  ) / 750
	);

  page = page.replace('{closing-in-units}', totClosingBal);
  
  var totSalesBal = Math.round(
	  ((categories['BEER']?.sales || 0) / 7800) +
	  ((categories['WINE']?.sales || 0) / 2250) +
	  (
	    (categories['WHISKY']?.sales || 0) +
	    (categories['BRANDY']?.sales || 0) +
	    (categories['RUM']?.sales || 0) +
	    (categories['VODKA']?.sales || 0)
	  ) / 750
	);
  page = page.replace('{sales-in-units}', totSalesBal);

  var totTOTBal = Math.round(
	  ((categories['BEER']?.total || 0) / 7800) +
	  ((categories['WINE']?.total || 0) / 2250) +
	  (
	    (categories['WHISKY']?.total || 0) +
	    (categories['BRANDY']?.total || 0) +
	    (categories['RUM']?.total || 0) +
	    (categories['VODKA']?.total || 0)
	  ) / 750
	);
  page = page.replace('{total-in-units}', totTOTBal);

  var totpurchaseBal = Math.round(
	  ((categories['BEER']?.purchase || 0) / 7800) +
	  ((categories['WINE']?.purchase || 0) / 2250) +
	  (
	    (categories['WHISKY']?.purchase || 0) +
	    (categories['BRANDY']?.purchase || 0) +
	    (categories['RUM']?.purchase || 0) +
	    (categories['VODKA']?.purchase || 0)
	  ) / 750
	);
  page = page.replace('{purchase-in-units}', totpurchaseBal);

  var totopBal = Math.round(
	  ((categories['BEER']?.openingBalance || 0) / 7800) +
	  ((categories['WINE']?.openingBalance || 0) / 2250) +
	  (
	    (categories['WHISKY']?.openingBalance || 0) +
	    (categories['BRANDY']?.openingBalance || 0) +
	    (categories['RUM']?.openingBalance || 0) +
	    (categories['VODKA']?.openingBalance || 0)
	  ) / 750
	);
  page = page.replace('{openingBalance-in-units}', totopBal);

  const beerOBinBattles = response.data.productList.filter(product => product.category === "BEER")
    .reduce((total, product) => total + product.openingStock, 0);
  const beerPurchaseinBattles = response.data.productList.filter(product => product.category === "BEER")
    .reduce((total, product) => total + product.purchaseStock, 0);
  const beerTOTinBattles = response.data.productList.filter(product => product.category === "BEER")
    .reduce((total, product) => total + product.stock, 0);
  const beerSalesinBattles = response.data.productList.filter(product => product.category === "BEER")
    .reduce((total, product) => total + product.sales, 0);
  const beerCBinBattles = response.data.productList.filter(product => product.category === "BEER")
    .reduce((total, product) => total + product.closingStock, 0);    

  page = page.replace('{beerCountOB}', beerOBinBattles);
  page = page.replace('{beerCountPurchase}', beerPurchaseinBattles);
  page = page.replace('{beerCountTOT}', beerTOTinBattles);
  page = page.replace('{beerCountsales}', beerSalesinBattles);
  page = page.replace('{beerCountCB}', beerCBinBattles);

  if (["tirupur"].includes(accountInfo?.unitNoteFormat?.toLowerCase())) {
  	var totUnit = 0;
  	var ML1000Count = getBottleCountForLiquor(response, "1000ML");
  	page = page.replace('{1000MLbottles}', ML1000Count);
  	page = page.replace('{1000MLinML}', ML1000Count*1000);
  	totUnit += (ML1000Count*1000)/750;
  	page = page.replace('{1000MLUnits}', Math.round((ML1000Count*1000)/750));

  	var ML750Count = getBottleCountForLiquor(response, "750ML");
  	page = page.replace('{750Bottles}', ML750Count);
  	page = page.replace('{750MLinML}', ML750Count*750);
  	totUnit += (ML750Count*750)/750;
  	page = page.replace('{750MLUnits}', Math.round((ML750Count*750)/750));

  	var ML375Count = getBottleCountForLiquor(response, "375ML");
  	page = page.replace('{375MLbottles}', ML375Count);
  	page = page.replace('{375MLinML}', ML375Count*375);
  	totUnit += (ML375Count*375)/750;
  	page = page.replace('{375MLUnits}', Math.round((ML375Count*375)/750));

  	var ML180Count = getBottleCountForLiquor(response, "180ML");
  	page = page.replace('{180MLbottles}', ML180Count);
  	page = page.replace('{180MLinML}', ML180Count*180);
  	totUnit += (ML180Count*180)/750;
  	page = page.replace('{180MLUnits}', Math.round((ML180Count*180)/750));

  	var ML650Count = getBottleCountForBeer(response, "650ML");
  	page = page.replace('{650MLbottles}', ML650Count);
  	totUnit += (ML650Count*650)/7800;
  	page = page.replace('{650MLUnits}', Math.round((ML650Count*650)/7800));

  	var ML500Count = getBottleCountForBeer(response, "500ML");
  	page = page.replace('{500MLbottles}', ML500Count);
  	totUnit += (ML500Count*500)/7800;
  	page = page.replace('{500MLUnits}', Math.round((ML500Count*500)/7800));


  	var wineCount = getBottleCountForWine(response, "180ML");
  	var wineMl = wineCount*180;
  	var wineCount1 = getBottleCountForWine(response, "750ML");
  	wineMl += wineCount1*750;
  	wineCount += wineCount1;
  	page = page.replace('{winebottles}', wineCount);
  	page = page.replace('{wineML}', wineMl);
  	totUnit += (wineMl)/2250;
  	page = page.replace('{wineUnit}', Math.round(wineMl/2250));

  	page = page.replace('{totInUnitTRP}', Math.round(totUnit));

  	page = page.replace('{closing-in-unitstrp}', Math.round(totUnit));
  	page = page.replace('{balance-in-units}', Math.round(4000-totUnit));
  	
  }

  if (["cbe"].includes(accountInfo?.unitNoteFormat?.toLowerCase())) {
  	
  	page = page.replace('{1000MLOpening}', ""+(getBottleCountForLiquorWithType(response, "1000ML", "openingStock")*1000)+ " ("+getBottleCountForLiquorWithType(response, "1000ML", "openingStock")+")");
  	page = page.replace('{750MLOpening}', ""+(getBottleCountForLiquorWithType(response, "750ML", "openingStock")*750)+ " ("+getBottleCountForLiquorWithType(response, "750ML", "openingStock")+")");
  	page = page.replace('{375MLOpening}', ""+(getBottleCountForLiquorWithType(response, "375ML", "openingStock")*375)+ " ("+getBottleCountForLiquorWithType(response, "375ML", "openingStock")+")");
  	page = page.replace('{180MLOpening}', ""+(getBottleCountForLiquorWithType(response, "180ML", "openingStock")*180)+ " ("+getBottleCountForLiquorWithType(response, "180ML", "openingStock")+")");
  	page = page.replace('{650MLOpening}', getBottleCountForLiquorWithType(response, "650ML", "openingStock"));
  	page = page.replace('{500MLOpening}', getBottleCountForLiquorWithType(response, "500ML", "openingStock"));
  	page = page.replace('{325MLOpening}', getBottleCountForLiquorWithType(response, "325ML", "openingStock"));
  	page = page.replace('{750MLwineOpening}', ""+(getBottleCountForWineWithType(response, "750ML", "openingStock")*750)+ " ("+getBottleCountForWineWithType(response, "750ML", "openingStock")+")");
  	page = page.replace('{375MLwineOpening}', ""+(getBottleCountForWineWithType(response, "375ML", "openingStock")*375)+ " ("+getBottleCountForWineWithType(response, "375ML", "openingStock")+")");
  	page = page.replace('{180MLwineOpening}', ""+(getBottleCountForWineWithType(response, "180ML", "openingStock")*180)+ " ("+getBottleCountForWineWithType(response, "180ML", "openingStock")+")");

  	page = page.replace('{1000MLPurchase}', ""+(getBottleCountForLiquorWithType(response, "1000ML", "purchaseStock")*1000)+ " ("+getBottleCountForLiquorWithType(response, "1000ML", "purchaseStock")+")");
  	page = page.replace('{750MLPurchase}', ""+(getBottleCountForLiquorWithType(response, "750ML", "purchaseStock")*750)+ " ("+getBottleCountForLiquorWithType(response, "750ML", "purchaseStock")+")");
  	page = page.replace('{375MLPurchase}', ""+(getBottleCountForLiquorWithType(response, "375ML", "purchaseStock")*375)+ " ("+getBottleCountForLiquorWithType(response, "375ML", "purchaseStock")+")");
  	page = page.replace('{180MLPurchase}', ""+(getBottleCountForLiquorWithType(response, "180ML", "purchaseStock")*180)+ " ("+getBottleCountForLiquorWithType(response, "180ML", "purchaseStock")+")");
  	page = page.replace('{650MLPurchase}', getBottleCountForLiquorWithType(response, "650ML", "purchaseStock"));
  	page = page.replace('{500MLPurchase}', getBottleCountForLiquorWithType(response, "500ML", "purchaseStock"));
  	page = page.replace('{325MLPurchase}', getBottleCountForLiquorWithType(response, "325ML", "purchaseStock"));
  	page = page.replace('{750MLwinePurchase}', ""+(getBottleCountForWineWithType(response, "750ML", "purchaseStock")*750)+ " ("+getBottleCountForWineWithType(response, "750ML", "purchaseStock")+")");
  	page = page.replace('{375MLwinePurchase}', ""+(getBottleCountForWineWithType(response, "375ML", "purchaseStock")*375)+ " ("+getBottleCountForWineWithType(response, "375ML", "purchaseStock")+")");
  	page = page.replace('{180MLwinePurchase}', ""+(getBottleCountForWineWithType(response, "180ML", "purchaseStock")*180)+ " ("+getBottleCountForWineWithType(response, "180ML", "purchaseStock")+")");

  	page = page.replace('{1000MLSales}', ""+(getBottleCountForLiquorWithType(response, "1000ML", "sales")*1000)+ " ("+getBottleCountForLiquorWithType(response, "1000ML", "sales")+")");
  	page = page.replace('{750MLSales}', ""+(getBottleCountForLiquorWithType(response, "750ML", "sales")*750)+ " ("+getBottleCountForLiquorWithType(response, "750ML", "sales")+")");
  	page = page.replace('{375MLSales}', ""+(getBottleCountForLiquorWithType(response, "375ML", "sales")*375)+ " ("+getBottleCountForLiquorWithType(response, "375ML", "sales")+")");
  	page = page.replace('{180MLSales}', ""+(getBottleCountForLiquorWithType(response, "180ML", "sales")*180)+ " ("+getBottleCountForLiquorWithType(response, "180ML", "sales")+")");
  	page = page.replace('{650MLSales}', getBottleCountForLiquorWithType(response, "650ML", "sales"));
  	page = page.replace('{500MLSales}', getBottleCountForLiquorWithType(response, "500ML", "sales"));
  	page = page.replace('{325MLSales}', getBottleCountForLiquorWithType(response, "325ML", "sales"));
  	page = page.replace('{750MLwineSales}', ""+(getBottleCountForWineWithType(response, "750ML", "sales")*750)+ " ("+getBottleCountForWineWithType(response, "750ML", "sales")+")");
  	page = page.replace('{375MLwineSales}', ""+(getBottleCountForWineWithType(response, "375ML", "sales")*375)+ " ("+getBottleCountForWineWithType(response, "375ML", "sales")+")");
  	page = page.replace('{180MLwineSales}', ""+(getBottleCountForWineWithType(response, "180ML", "sales")*180)+ " ("+getBottleCountForWineWithType(response, "180ML", "sales")+")");

  	page = page.replace('{1000MLClosing}', ""+(getBottleCountForLiquorWithType(response, "1000ML", "closingStock")*1000)+ " ("+getBottleCountForLiquorWithType(response, "1000ML", "closingStock")+")");
  	page = page.replace('{750MLClosing}', ""+(getBottleCountForLiquorWithType(response, "750ML", "closingStock")*750)+ " ("+getBottleCountForLiquorWithType(response, "750ML", "closingStock")+")");
  	page = page.replace('{375MLClosing}', ""+(getBottleCountForLiquorWithType(response, "375ML", "closingStock")*375)+ " ("+getBottleCountForLiquorWithType(response, "375ML", "closingStock")+")");
  	page = page.replace('{180MLClosing}', ""+(getBottleCountForLiquorWithType(response, "180ML", "closingStock")*180)+ " ("+getBottleCountForLiquorWithType(response, "180ML", "closingStock")+")");
  	page = page.replace('{650MLClosing}', getBottleCountForLiquorWithType(response, "650ML", "closingStock"));
  	page = page.replace('{500MLClosing}', getBottleCountForLiquorWithType(response, "500ML", "closingStock"));
  	page = page.replace('{325MLClosing}', getBottleCountForLiquorWithType(response, "325ML", "closingStock"));
  	page = page.replace('{750MLwineClosing}', ""+(getBottleCountForWineWithType(response, "750ML", "closingStock")*750)+ " ("+getBottleCountForWineWithType(response, "750ML", "closingStock")+")");
  	page = page.replace('{375MLwineClosing}', ""+(getBottleCountForWineWithType(response, "375ML", "closingStock")*375)+ " ("+getBottleCountForWineWithType(response, "375ML", "closingStock")+")");
  	page = page.replace('{180MLwineClosing}', ""+(getBottleCountForWineWithType(response, "180ML", "closingStock")*180)+ " ("+getBottleCountForWineWithType(response, "180ML", "closingStock")+")"); 	
  }

  if (["pudukottai"].includes(accountInfo?.unitNoteFormat?.toLowerCase())) {  	
  	
  	const categories = ["BRANDY", "WHISKY", "RUM", "WINE", "VODKA", "BEER"];

	const types = {
	    opening: "OB",
	    purchase: "P",
	    total: "T",
	    sales: "S",
	    closing: "CB"
	};

	const totalUnitsByType = {
	    opening: 0,
	    purchase: 0,
	    total: 0,
	    sales: 0,
	    closing: 0
	};

	const beerStockFieldMap = {
	    opening: "openingStock",
	    purchase: "purchaseStock",
	    total: "stock",
	    sales: "sales",
	    closing: "closingStock"
	};

	for (const category of categories) {
	    for (const [typeKey, prefix] of Object.entries(types)) {
	        
	        let mlValue = 0;
	        let unitValue = 0;

	        if (category === "BEER") {
	            const stockField = beerStockFieldMap[typeKey];

	            mlValue = Math.round(getMLCountForBeer(response, stockField));
	            unitValue = getUnitCountForBeer(response, stockField);
	        
	        } else {
	            mlValue = getMLCountForLiquor(response, category, typeKey);

	            if (category === "WINE") {
	                unitValue = mlValue / 2250;
	            } else {
	                unitValue = mlValue / 750;
	            }
	        }
	        unitValue = Number(unitValue.toFixed(2));
	        // Replace ML placeholder
	        page = page.replace(`{${prefix}_${category}_ML}`, mlValue);

	        // Replace UNIT placeholder
	        page = page.replace(`{${prefix}_${category}_U}`,  unitValue);
	        // Add to total (Include BEER units also)
	        totalUnitsByType[typeKey] += Math.trunc(unitValue * 100);
	    }
	}

	// Replace TOTAL UNIT placeholders
	page = page.replace('{OB_TOTAL}', totalUnitsByType.opening/ 100);
	page = page.replace('{P_TOTAL}', totalUnitsByType.purchase/ 100);
	page = page.replace('{T_TOTAL}', totalUnitsByType.total/ 100);
	page = page.replace('{S_TOTAL}', totalUnitsByType.sales/ 100);
	page = page.replace('{CB_TOTAL}', totalUnitsByType.closing/ 100);


  }


	const newWindow = window.open("", "_blank");
	if (newWindow) {
	     newWindow.document.write(page);
	     newWindow.document.close();
	     newWindow.print();
	}
}

function groupCategories(response){
	for(var i in response.data.productList){
		var item = response.data.productList[i];
		if(!("category" in item)){
			item.category = getCategory(item.SKU);
		}
	}
	const result = {};

	response.data.productList.forEach(item => {
	  const category = item.category || "UNKNOWN"; // Default to UNKNOWN if no category
	  let sizeInML = 0;

	  const sizeMatch = item.SKU.match(/(\d+)(ML)/);
		sizeInML = sizeMatch ? parseInt(sizeMatch[1], 10) : 0;

		// If size is still 0, check if category is BEER
		if (sizeInML === 0 && category.toUpperCase() === "BEER") {
		  sizeInML = 650;
		}

	  if (!result[category]) {
	    result[category] = {
	      openingBalance: 0,
	      purchase: 0,
	      total: 0,
	      sales: 0,
	      closingBalance: 0
	    };
	  }

	  result[category].openingBalance += item.openingStock * sizeInML;
	  result[category].purchase += item.purchaseStock * sizeInML;
	  result[category].total += (item.openingStock + item.purchaseStock) * sizeInML;
	  result[category].sales += item.sales * sizeInML;
	  result[category].closingBalance += item.closingStock * sizeInML;
	});

  
	return result;
}

function getCategory(SKU){
    const productMaster = localStorage.getItem('products');  
    const products = productMaster ? JSON.parse(productMaster) : null;
    for(var i in products.productList){
        var item = products.productList[i];
        if(item.SKU == SKU){
            return item.brand;
        }
    }
}

function getMLCountForLiquor(response, category, type) {
    const SIZE_MAP = {
        "180ML": 180, "375ML": 375, "750ML": 750,
        "1000ML": 1000, "650ML": 650, "500ML": 500, "325ML": 325
    };

    const STOCK_FIELD_MAP = {
        opening: "openingStock",
        purchase: "purchaseStock",
        total: "stock",
        sales: "sales",
        closing: "closingStock"
    };

    const stockField = STOCK_FIELD_MAP[type];

    return (response?.data?.productList || []).reduce((sum, item) => {
        if ((item.category || "").toUpperCase() !== category) return sum;

        const sizeML = SIZE_MAP[item.SKU?.split("-")[1]] || 0;
        const stock = item[stockField] || 0;

        return sum + stock * sizeML;
    }, 0);
}

//Get bottle count for Brandy, Whisky, Vodka, Rum
function getBottleCountForLiquor(response, size) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (["BRANDY", "RUM", "VODKA", "WHISKY", "SCOTCH", "LIQUOR"].includes(category)) {
			const itemSize = item.SKU?.split("-")[1];
			if (itemSize == size) {
				count += item.closingStock;
			}
		}
	}
	return count;
}

function getMLCountForBeer(response, type) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category == "BEER") {
			const itemSize = item.SKU?.split("-")[1];
			if(itemSize == "650ML"){
				count += item[type]*650;
			}else if(itemSize == "500ML"){
				count += item[type]*500;
			}else if(itemSize == "325ML"){
				count += item[type]*325;
			}
		}
	}
	return count;
}

function getUnitCountForBeer(response, type) {
	let count12 = 0;
	let count24 = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category == "BEER") {
			const itemSize = item.SKU?.split("-")[1];
			if(itemSize == "650ML"){
				count12 += item[type];
			} else if(itemSize == "500ML" || itemSize == "325ML"){
				count24 += item[type];
			}

		}
	}
	return (count12/12)+(count24/24);
}

//Get bottle count for Beer
function getBottleCountForBeer(response, size) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category == "BEER") {
			if(size == "650ML"){
				if (!item.SKU.includes("500")) {
					count += item.closingStock;
				}
			} else if(size == "500ML"){
				if (item.SKU.includes("500")) {
					count += item.closingStock;
				}
			}

		}
	}
	return count;
}

//Get bottle count for wine
function getBottleCountForWine(response, size) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category == "WINE") {
				const itemSize = item.SKU?.split("-")[1];
				if (itemSize == size) {
					count += item.closingStock;
				}
		}
	}
	return count;
}

//Get bottle count for Brandy, Whisky, Vodka, Rum
function getBottleCountForLiquorWithType(response, size, stockType) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category !== "WINE") {
			const itemSize = item.SKU?.split("-")[1];
			if (itemSize == size && item[stockType] != null) {
				count += item[stockType];
			}
		}
	}
	return count;
}

function getBottleCountForWineWithType(response, size, stockType) {
	let count = 0;
	for (const item of response?.data?.productList || []) {
		const category = item.category?.toUpperCase() || "";
		if (category == "WINE") {
			const itemSize = item.SKU?.split("-")[1];
			if (itemSize == size && item[stockType] != null) {
				count += item[stockType];
			}
		}
	}
	return count;
}




var ledgerTemplate = `<!DOCTYPE html>
			<html lang="en">
			<head>
			  <meta charset="UTF-8">
			  <meta name="viewport" content="width=device-width, initial-scale=1.0">
			  <title>A4 Liquor Register</title>
			  <style>
			    body {
			      font-family: Arial, sans-serif;
			      margin: 1px;
			    }
			    table {
			      width: 100%;
			      border-collapse: collapse;
			      margin-top: 20px;
			    }
				table tr {
				    height: 50px; /* Set the desired height */
				  }
			    th, td {
			      border: 1px solid #000;
			      text-align: center;
			      padding: 5px;
			    }
			    th {
			      background-color: #f4f4f4;
			    }
			    .header {
			      text-align: center;
			      font-weight: bold;
			      margin-bottom: 1px;
			    }
			    .horizontal-text {
			      writing-mode: horizontal-tb;
			      text-align: center; /* Align text in the center */
			      vertical-align: middle; /* Vertically center the text */
			      transform: rotate(-90deg); /* Rotate the text if necessary */
			      white-space: nowrap; /* Prevent text wrapping */
			    }
          .row {
                display: flex;
                justify-content: center;
                height: 100px;
                padding-top: 20px;
              }
          img {
            width: 20%;
            height: auto;
          }
			    @media print {
			      @page {
			        size: A4 landscape;
			        margin: 1mm;
			      }
			      body {
			        margin: 3px;
			      }
			    }
			  </style>
			</head>
			<body>
			  <br><br><br>
			  <div class="header">
			    <h2>{shopName}</h2>
				<h3>(See rule 34 of Tamil Nadu Liquor (License and Permit) Rules, 1981</h3>
			    <p>Accounts of liquor received and sold or issued and license in Form F.L.2.No.{licenseNo}</p>
			    <p>Date of : {date}</p>
			  </div>

			  <table id="liquorTable">
			    <thead>
			      <tr>
			        <th rowspan="2">Receipt / Issues</th>
			        <th colspan="2">BEER</th>
			        <th colspan="2">WINES</th>
			        <th colspan="2">WHISKY</th>
			        <th colspan="2">BRANDY</th>
			        <th colspan="2">RUM</th>
			        <th colspan="2">GIN</th>
			        <th colspan="2">VODKA</th>
			        <th colspan="2">Other Spirits</th>
			        <th rowspan="2">Remarks</th>
			      </tr>
			      <tr>
			        <th>650 ML</th>
			        <th>325 ML</th>
			        <th>Sparkling ML</th>
			        <th>Other</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			        <th>Indian ML</th>
			        <th>Foreign ML</th>
			      </tr>
			      <tr>
			        <td>Opening Balance</td>
			        <td>{openingBalance-BEER} ({beerCountOB})</td>
			        <td></td>
			        <td>{openingBalance-WINE}</td>
			        <td></td>
			        <td>{openingBalance-WHISKY}</td>
			        <td></td>
			        <td>{openingBalance-BRANDY}</td>
			        <td></td>
			        <td>{openingBalance-RUM}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{openingBalance-VODKA}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{openingBalance-in-units}</td>
			      </tr>
			      <tr>
			        <td>Received From Store</td>
			        <td>{purchase-BEER} ({beerCountPurchase})</td>
			        <td></td>
			        <td>{purchase-WINE}</td>
			        <td></td>
			        <td>{purchase-WHISKY}</td>
			        <td></td>
			        <td>{purchase-BRANDY}</td>
			        <td></td>
			        <td>{purchase-RUM}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{purchase-VODKA}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{purchase-in-units}</td>
			      </tr>
			      <tr>
			        <td>Total</td>
			        <td>{total-BEER} ({beerCountTOT})</td>
			        <td></td>
			        <td>{total-WINE}</td>
			        <td></td>
			        <td>{total-WHISKY}</td>
			        <td></td>
			        <td>{total-BRANDY}</td>
			        <td></td>
			        <td>{total-RUM}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{total-VODKA}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{total-in-units}</td>
			      </tr>
			      <tr>
			        <td>Sales</td>
			        <td>{sales-BEER} ({beerCountsales})</td>
			        <td></td>
			        <td>{sales-WINE}</td>
			        <td></td>
			        <td>{sales-WHISKY}</td>
			        <td></td>
			        <td>{sales-BRANDY}</td>
			        <td></td>
			        <td>{sales-RUM}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{sales-VODKA}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{sales-in-units}</td>
			      </tr>
			      <tr>
			        <td>Closing Balance</td>
			        <td>{closingBalance-BEER} ({beerCountCB})</td>
			        <td></td>
			        <td>{closingBalance-WINE}</td>
			        <td></td>
			        <td>{closingBalance-WHISKY}</td>
			        <td></td>
			        <td>{closingBalance-BRANDY}</td>
			        <td></td>
			        <td>{closingBalance-RUM}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{closingBalance-VODKA}</td>
			        <td></td>
			        <td></td>
			        <td></td>
			        <td>{closing-in-units}</td>
			      </tr>
			    </thead>
			    <tbody>
			      <!-- Dynamic rows will be inserted here -->
			    </tbody>
			  </table>
        <div class="row">
            <img src="http://tnfl2.com/images/{image-shopNumber}.png">
          </div>
			</body>
			</html>
`; 


var tirupurTemplate = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Bar Stock Details</title>
  <style>
    @page {
      size: A4;
      margin: 20mm;
    }
    body {
      font-family: Arial, sans-serif;
      margin: 0;
      padding: 20mm;
    }
    h1, h3 {
      text-align: center;
      margin-bottom: 10px;
    }
    p {
      text-align: center;
      margin: 5px 0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
      font-size: 16px;
    }
    th, td {
      border: 1px solid #000;
      padding: 8px;
      text-align: center;
    }
    .footer {
      margin-top: 40px;
      font-size: 16px;
      text-align: left;
    }
  </style>
</head>
<body>

  <h1>{shopName}</h1>
  <p>
    License No.: {licenseNo}<br>
    Door No.: 86,87,87A,87A1 – ST-6, Convent Street, Cheyur Road – 641654<br>
    Avinashi (Tk), Tirupur (Dt)
  </p>
  <hr><hr>
  <h3>BAR STOCK DETAILS AS ON DATE: {date}</h3>

  <table>
    <tr>
      <th>Item</th>
      <th>Details</th>
    </tr>
    <tr>
      <td>Total Permission Unit</td>
      <td>4000</td>
    </tr>
    <tr>
      <td>Stock Held in Hand As on Date</td>
      <td>{closing-in-unitstrp}</td>
    </tr>
    <tr>
      <td>Balance</td>
      <td>{balance-in-units}</td>
    </tr>
  </table>

  <h2>STOCK DETAILS</h2>

  <table>
    <thead>
      <tr>
        <th>S.No</th>
        <th>Pack Size</th>
        <th>No. of Bottles</th>
        <th>ML</th>
        <th>Units</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>1</td>
        <td>1000 ML</td>
        <td>{1000MLbottles}</td>
        <td>{1000MLinML}</td>
        <td>{1000MLUnits}</td>
      </tr>
      <tr>
        <td>2</td>
        <td>750 ML</td>
        <td>{750Bottles}</td>
        <td>{750MLinML}</td>
        <td>{750MLUnits}</td>
      </tr>
      <tr>
        <td>3</td>
        <td>375 ML</td>
        <td>{375MLbottles}</td>
        <td>{375MLinML}</td>
        <td>{375MLUnits}</td>
      </tr>
      <tr>
        <td>4</td>
        <td>180 ML</td>
        <td>{180MLbottles}</td>
        <td>{180MLinML}</td>
        <td>{180MLUnits}</td>
      </tr>
      <tr>
        <td>5</td>
        <td>650 ML</td>
        <td>{650MLbottles}</td>
        <td>-</td>
        <td>{650MLUnits}</td>
      </tr>
      <tr>
        <td>6</td>
        <td>500 ML</td>
        <td>{500MLbottles}</td>
        <td>-</td>
        <td>{500MLUnits}</td>
      </tr>
      <tr>
        <td>7</td>
        <td>Wine</td>
        <td>{winebottles}</td>
        <td>{wineML}</td>
        <td>{wineUnit}</td>
      </tr>
        <tr>
        
        <td colspan="4">Total</td>
        <td>{totInUnitTRP}</td>
      </tr>
    </tbody>
  </table>
</body>
</html>
`;


var cbeTemplate = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Bar Stock Details</title>
  <style>
    @page {
      size: A4;
      margin: 20mm;
    }
    body {
      font-family: Arial, sans-serif;
      margin: 0;
      padding: 20mm;
    }
    h1, h3 {
      text-align: center;
      margin-bottom: 10px;
    }
    p {
      text-align: center;
      margin: 5px 0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
      font-size: 16px;
    }
    th, td {
      border: 1px solid #000;
      padding: 8px;
      text-align: center;
    }
    .footer {
      margin-top: 40px;
      font-size: 16px;
      text-align: left;
    }
  </style>
</head>
<body>

  <h1>{shopName}</h1>
  <p>
    License No.: {licenseNo}<br>
  </p>
  <hr><hr>
  <h3>STOCK DETAILS AS ON DATE: {date}</h3>

  <table>
    <thead>
      <tr>
        <th>S.No</th>
        <th>Pack Size</th>
        <th>Opening</th>
        <th>Purchase</th>
        <th>Sales</th>
        <th>Closing</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>1</td>
        <td>1000 ML</td>
        <td>{1000MLOpening}</td>
        <td>{1000MLPurchase}</td>
        <td>{1000MLSales}</td>
        <td>{1000MLClosing}</td>
      </tr>
      <tr>
        <td>2</td>
        <td>750 ML</td>
        <td>{750MLOpening}</td>
        <td>{750MLPurchase}</td>
        <td>{750MLSales}</td>
        <td>{750MLClosing}</td>
      </tr>
      <tr>
        <td>3</td>
        <td>375 ML</td>
        <td>{375MLOpening}</td>
        <td>{375MLPurchase}</td>
        <td>{375MLSales}</td>
        <td>{375MLClosing}</td>
      </tr>
      <tr>
        <td>4</td>
        <td>180 ML</td>
        <td>{180MLOpening}</td>
        <td>{180MLPurchase}</td>
        <td>{180MLSales}</td>
        <td>{180MLClosing}</td>
      </tr>
      <tr>
        <td>5</td>
        <td>650 ML</td>
        <td>{650MLOpening}</td>
        <td>{650MLPurchase}</td>
        <td>{650MLSales}</td>
        <td>{650MLClosing}</td>
      </tr>
      <tr>
        <td>6</td>
        <td>500 ML</td>
        <td>{500MLOpening}</td>
        <td>{500MLPurchase}</td>
        <td>{500MLSales}</td>
        <td>{500MLClosing}</td>
      </tr>
			<tr>
        <td>7</td>
        <td>325 ML</td>
        <td>{325MLOpening}</td>
        <td>{325MLPurchase}</td>
        <td>{325MLSales}</td>
        <td>{325MLClosing}</td>
      </tr>
      <tr>
        <td>8</td>
        <td>Wine-180ML</td>
        <td>{180MLwineOpening}</td>
        <td>{180MLwinePurchase}</td>
        <td>{180MLwineSales}</td>
        <td>{180MLwineClosing}</td>
      </tr>
			<tr>
        <td>9</td>
        <td>Wine-375ML</td>
        <td>{375MLwineOpening}</td>
        <td>{375MLwinePurchase}</td>
        <td>{375MLwineSales}</td>
        <td>{375MLwineClosing}</td>
      </tr>
			<tr>
        <td>10</td>
        <td>Wine-750ML</td>
        <td>{750MLwineOpening}</td>
        <td>{750MLwinePurchase}</td>
        <td>{750MLwineSales}</td>
        <td>{750MLwineClosing}</td>
      </tr>
    </tbody>
  </table>
</body>
</html>
`;

var pudukottaiTemplate = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>A4 Liquor Register</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      margin: 10px;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 10px;
    }
    th, td {
      border: 1px solid #000;
      text-align: center;
      padding: 6px;
    }
    th {
      background-color: #f4f4f4;
      font-weight: bold;
    }
    .header {
      text-align: center;
      font-weight: bold;
      margin-bottom: 5px;
    }
    @media print {
      @page {
        size: A4 portrait;
        margin: 5mm;
      }
    }
  </style>
</head>
<body>

  <div class="header">
    <h2>{shopName}</h2>
    <p style="display: flex; justify-content: space-between; width: 100%;">
      <span>FL2 License No.: {licenseNo}</span>
      <span>Total Limit Unit : 6000</span>
  </p>
    <p><b>Form F.A.c(3a)</b> (See Rules 17/35 and 36)</p>
    <p style="display: flex; justify-content: space-between; width: 100%;">
      <span>Daily Accounts of Issues of Liquor Under the License in Form FL2 Club</span>
      <span>Date : {date}</span>
    </p>
  </div>

  <table>
    <thead>
      <tr>
        <th rowspan="2">Description</th>
        <th colspan="2">Brandy</th>
        <th colspan="2">Whisky</th>
        <th colspan="2">Rum</th>
        <th colspan="2">Wine</th>
        <th colspan="2">Vodka</th>
        <th colspan="2">Beer</th>
        <th rowspan="2">Total Units</th>
      </tr>
      <tr>
        <th>No. of Units</th><th>Stock in ML</th>
        <th>No. of Units</th><th>Stock in ML</th>
        <th>No. of Units</th><th>Stock in ML</th>
        <th>No. of Units</th><th>Stock in ML</th>
        <th>No. of Units</th><th>Stock in ML</th>
        <th>No. of Units</th><th>Stock in ML</th>
      </tr>
    </thead>

    <tbody>
      <tr>
        <td>Opening Balance</td>
        <td>{OB_BRANDY_U}</td><td>{OB_BRANDY_ML}</td>
        <td>{OB_WHISKY_U}</td><td>{OB_WHISKY_ML}</td>
        <td>{OB_RUM_U}</td><td>{OB_RUM_ML}</td>
        <td>{OB_WINE_U}</td><td>{OB_WINE_ML}</td>
        <td>{OB_VODKA_U}</td><td>{OB_VODKA_ML}</td>
        <td>{OB_BEER_U}</td><td>{OB_BEER_ML}</td>
        <td>{OB_TOTAL}</td>
      </tr>

      <tr>
        <td>Purchase</td>
        <td>{P_BRANDY_U}</td><td>{P_BRANDY_ML}</td>
        <td>{P_WHISKY_U}</td><td>{P_WHISKY_ML}</td>
        <td>{P_RUM_U}</td><td>{P_RUM_ML}</td>
        <td>{P_WINE_U}</td><td>{P_WINE_ML}</td>
        <td>{P_VODKA_U}</td><td>{P_VODKA_ML}</td>
        <td>{P_BEER_U}</td><td>{P_BEER_ML}</td>
        <td>{P_TOTAL}</td>
      </tr>

      <tr>
        <td>Total</td>
        <td>{T_BRANDY_U}</td><td>{T_BRANDY_ML}</td>
        <td>{T_WHISKY_U}</td><td>{T_WHISKY_ML}</td>
        <td>{T_RUM_U}</td><td>{T_RUM_ML}</td>
        <td>{T_WINE_U}</td><td>{T_WINE_ML}</td>
        <td>{T_VODKA_U}</td><td>{T_VODKA_ML}</td>
        <td>{T_BEER_U}</td><td>{T_BEER_ML}</td>
        <td>{T_TOTAL}</td>
      </tr>

      <tr>
        <td>Sales</td>
        <td>{S_BRANDY_U}</td><td>{S_BRANDY_ML}</td>
        <td>{S_WHISKY_U}</td><td>{S_WHISKY_ML}</td>
        <td>{S_RUM_U}</td><td>{S_RUM_ML}</td>
        <td>{S_WINE_U}</td><td>{S_WINE_ML}</td>
        <td>{S_VODKA_U}</td><td>{S_VODKA_ML}</td>
        <td>{S_BEER_U}</td><td>{S_BEER_ML}</td>
        <td>{S_TOTAL}</td>
      </tr>

      <tr>
        <td>Closing Balance</td>
        <td>{CB_BRANDY_U}</td><td>{CB_BRANDY_ML}</td>
        <td>{CB_WHISKY_U}</td><td>{CB_WHISKY_ML}</td>
        <td>{CB_RUM_U}</td><td>{CB_RUM_ML}</td>
        <td>{CB_WINE_U}</td><td>{CB_WINE_ML}</td>
        <td>{CB_VODKA_U}</td><td>{CB_VODKA_ML}</td>
        <td>{CB_BEER_U}</td><td>{CB_BEER_ML}</td>
        <td>{CB_TOTAL}</td>
      </tr>
    </tbody>
  </table>

  <br><br>
</body>
</html>
`; 

