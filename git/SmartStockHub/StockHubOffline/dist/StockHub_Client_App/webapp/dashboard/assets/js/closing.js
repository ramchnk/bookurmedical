$(document).ready(function() {

    console.log("Purchase Page is fully loaded!");
    init();
});
let url = `${webmanagerurl}/productmaster`;
async function init() {
    const response = await getRequest(url);
    var data = constructList(response);
    const container = document.getElementById("purchase-table");
    container.innerHTML = "";
    if (document.getElementById("purchase-table"))
    new gridjs.Grid({
            sort: true,
            pagination: {
            	limit : 100
            },
            fixedHeader: true,
            height: '550px',
            search: true,
            data: data,
            columns: [
            { 
	            name: "SKU",
	            formatter: (cell, row) => gridjs.html(`<div id="sellerSKU-${row.cells[2].data}" class="ram-sellerSKU">${cell}</div>`)
	        },
	        { 
	            name: "Opening Stock",
	            formatter: (cell, row) => gridjs.html(`<div class="col-md-6" id="openingStock-${row.cells[2].data}">${cell}</div>`)
	        },
	        { 
	            name: "Purchase Stock",
	            formatter: (cell, row) => gridjs.html(`<div class="col-md-6"><input type="text" id="purchasestock-${row.cells[2].data}"+ class="form-control ram-purchase" value="0"></div>`)
	        },
	        { 
	            name: "Updated Stock",
	            formatter: (cell, row) => gridjs.html(`<span id="updatedStock-${row.cells[2].data}" class="text-dark fw-medium mt-3">${cell}</span>`)          
	        }],
    }).render(document.getElementById("purchase-table"));
}

function constructList(response){
	console.log(response);
	var tableList = [];
	for(var i in response.productList){
    	var item = response.productList[i];
    	var tableRow = [];
    	tableRow.push(item.SKU);
    	tableRow.push(item.stock);
    	tableRow.push(i);
    	tableRow.push(item.stock);
    	tableList.push(tableRow);
    }
    return tableList;
}

function onPurchaseProcess(){
	const id = $(this).attr('id');
	const sku = id.split("-")[1];
	var openingStock = parseInt($("#openingStock-"+sku).text());
	var purchseStock = parseInt($(this).val());
	$("#updatedStock-"+sku).text(openingStock+purchseStock);
}

$(document).on('blur', '.form-control.ram-purchase', onPurchaseProcess);

function updatePurchase(){
	$("#purchaseBtn").html(`<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
	var productMaster = [];
	$('.ram-sellerSKU').each(function() {
	    console.log("ID: " + $(this).attr('id') + ", Text: " + $(this).text());
	    var index = $(this).attr('id').split("-")[1];
	    var item = {
	    	SKU : $("#sellerSKU-"+index).text(),
	    	openingStock : parseInt($("#openingStock-"+index).text()),
	    	purchaseStock : parseInt($("#purchasestock-"+index).val()),
	    	stock : parseInt($("#updatedStock-"+index).text())
	    };
	    productMaster.push(item);
	});
	var item = {
		"productList" : productMaster
	}
	putRequestWithCallback(url, item, updatePurchaseCalBack, null);
}

function updatePurchaseCalBack(){
	$("#purchaseBtn").html(`<span class="badge bg-primary me-1">Updated</span>`);
	$("#purchaseBtn").css({
            "color": "gray",
            "pointer-events": "none",
            "cursor": "default"
        });
}


