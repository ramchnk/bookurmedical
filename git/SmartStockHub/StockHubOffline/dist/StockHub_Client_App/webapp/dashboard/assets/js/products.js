$(document).ready(function () {
    console.log("Product Page is fully loaded!");
    init();
});

var items;
// let url = `${webmanagerurl}/productmaster`;
let url = productMasterurl; // Use global from http-util.js
async function init() {
    $("#reArrangeItemBtn").hide();
    updateShopName();
    const response = await getRequest(url);
    // const response = JSON.parse(``);
    items = response.data;
    var data = constructList(response.data);
    const container = document.getElementById("product-table");
    container.innerHTML = "";
    if (document.getElementById("product-table"))
        new gridjs.Grid({
            sort: true,
            pagination: {
                limit: 50
            },
            fixedHeader: true,
            height: '550px',
            search: true,
            data: data,
            columns: [
                {
                    name: " ",
                    formatter: (cell) => gridjs.html(`<div class="form-check ms-1"><input type="checkbox" class="form-check-input" id="customCheck2"><label class="form-check-label" for="customCheck2">&nbsp;</label></div>`)
                },
                {
                    name: "SKU",
                    formatter: (cell, row) => gridjs.html(`<div id="SKU-${row.cells[0].data}">${row.cells[0].data}</div>`)
                },
                {
                    name: "Stock",
                    formatter: (cell, row) => gridjs.html(`${row.cells[1].data}`)
                },
                {
                    name: "Sales Price",
                    formatter: (cell, row) => `₹${row.cells[2].data}` // Format as currency
                },
                {
                    name: "Profit Amount",
                    formatter: (cell, row) => gridjs.html(`${row.cells[3].data}`)
                },
                {
                    name: "Brand",
                    formatter: (cell, row) => gridjs.html(`${row.cells[4].data}`)
                },
                {
                    name: "Category",
                    formatter: (cell, row) => gridjs.html(`${row.cells[5].data}`)
                },
                {
                    name: "Action",
                    formatter: (cell, row) => gridjs.html(`<div class="d-flex gap-2"><a href="#!" onclick="loadUpdateItem('${row.cells[0].data}','${row.cells[1].data}','${row.cells[6].data}','${row.cells[2].data}','${row.cells[3].data}','${row.cells[4].data}','${row.cells[5].data}','${row.cells[7].data}')" class="btn btn-soft-primary btn-sm" data-bs-toggle="modal" data-bs-target="#updateItemModal"><iconify-icon icon="solar:pen-2-broken" class="align-middle fs-18"></iconify-icon></a><a href="#!" onclick="loadDeleteItem('${row.cells[0].data}')" class="btn btn-soft-danger btn-sm" data-bs-toggle="modal" data-bs-target="#deleteItemModal"><iconify-icon icon="solar:trash-bin-minimalistic-2-broken" class="align-middle fs-18"></iconify-icon></a></div>`) // Render a button
                }],
        }).render(document.getElementById("product-table"));

    document.getElementById("download-csv").addEventListener("click", function () {
        const headers = ["SKU", "Stock", "Sales Price", "Profit Amount", "Brand", "Category"];

        const rows = data.map(row => [
            row[0], // SKU
            row[1], // Stock
            row[2], // Sales Price
            row[3], // Profit Amount
            row[4], // Brand
            row[5]  // Category
        ]);

        let csvContent = "data:text/csv;charset=utf-8,"
            + [headers, ...rows].map(e => e.join(",")).join("\n");

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", "products.csv");
        document.body.appendChild(link); // Required for Firefox
        link.click();
        document.body.removeChild(link);
    });

    document.getElementById("download-stock").addEventListener("click", function () {
        const headers = ["SKU", "Stock", "Purchase Price", "Sales Price", "Brand", "Category"];
        var productItems = items.productList;

        productItems = productItems.sort((a, b) => {
            const A = a.order ?? Infinity;
            const B = b.order ?? Infinity;
            return A - B;
        });

        const filteredData = productItems.filter(row => row.stock > 0);

        const rows = filteredData.map(row => [
            row.SKU, // SKU
            row.stock, // Stock
            row.purchasePrice, // Purchase Price
            row.salePrice, // Sales Price
            row.brand, // Brand
            row.category  // Category
        ]);

        let csvContent = "data:text/csv;charset=utf-8,"
            + [headers, ...rows].map(e => e.join(",")).join("\n");

        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", "products.csv");
        document.body.appendChild(link); // Required for Firefox
        link.click();
        document.body.removeChild(link);
    });
}

function constructList(response) {
    var tableList = [];
    var productList = [];
    var isCustomSortingEnabled = false;
    const storedAccount = localStorage.getItem('account');
    const accountInfo = storedAccount ? JSON.parse(storedAccount) : null;
    if (accountInfo && accountInfo.hasOwnProperty('isCustomSortingEnabled')) {
        isCustomSortingEnabled = accountInfo.isCustomSortingEnabled;
    }
    if (isCustomSortingEnabled) {
        productList = sortByValume(response.productList);
    } else {
        productList = sortByBrand(response.productList);
    }
    if (accountInfo && accountInfo.hasOwnProperty('isCustomOrderEnabled')) {
        $("#reArrangeItemBtn").show();

        productList = productList.sort((a, b) => {
            const A = a.order ?? Infinity;
            const B = b.order ?? Infinity;
            return A - B;
        });
    }
    for (var i in productList) {
        var item = productList[i];
        var tableRow = [];
        tableRow.push(item.SKU);
        tableRow.push(item.stock);
        tableRow.push(item.salePrice);
        tableRow.push(item.profitAmount);
        tableRow.push(item.brand);
        tableRow.push(item.category);
        tableRow.push(item.purchasePrice);
        tableRow.push(item.hasOwnProperty('UPC') ? item.UPC : "NA");
        tableList.push(tableRow);
    }
    return tableList;
}

let selectedBrandUpdate = '';

$('#brand-update .dropdown-item').on('click', function (e) {
    e.preventDefault();
    selectedBrandUpdate = $(this).data('brand');
    $('#brandDropdown-updateItem').text(selectedBrandUpdate);
});


function loadUpdateItem(SKU, stock, purchasePrice, salePrice, profitAmount, brand, category, upc) {
    $("#sellerSKU-update").val(SKU);
    $("#sellerSKU-update").data('old-sku', SKU);
    $("#stock-update").val(stock);
    $("#purchasePrice-update").val(purchasePrice);
    $("#salePrice-update").val(salePrice);
    $("#profitAmount-update").val(profitAmount);
    $('#brandDropdown-updateItem').text(brand);
    selectedBrandUpdate = brand;
    $('input[name="updateType"][value="' + category + '"]').prop('checked', true);
    if (upc != "NA") {
        $("#upc-update").val(upc);
    }
}

function updateProduct() {
    var button = $('#updateButton');

    // Add spinner and disable button
    button.html(`<div class="spinner-grow text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    button.prop('disabled', true);

    var requestData = {
        SKU: $('#sellerSKU-update').val(),
        oldSKU: $('#sellerSKU-update').data('old-sku'),
        brand: selectedBrandUpdate,
        category: $('input[name="updateType"]:checked').val(),
        purchasePrice: parseFloat($('#purchasePrice-update').val()),
        salePrice: parseFloat($('#salePrice-update').val()),
        stock: parseFloat($("#stock-update").val()),
        profitAmount: parseFloat($('#profitAmount-update').val())
    };
    var upcValue = $('#upc-update').val().trim();
    if (upcValue !== "") {
        requestData.UPC = upcValue;
    }
    putRequestWithCallback(url + "/updateItem", requestData, updateProductCallback, null);


}

function updateProductCallback() {
    var button = $('#updateButton');
    button.html("Saved");
    setTimeout(function () {
        location.reload();
    }, 500);
}

function loadDeleteItem(SKU) {
    console.log(" ram " + SKU);
    $("#deleteSKU").text(SKU);
}

function deleteProduct() {
    var button = $('#deleteButton');

    // Add spinner and disable button
    button.html(`<div class="spinner-grow text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    button.prop('disabled', true);

    var requestData = {
        SKU: $('#deleteSKU').text()
    };
    deleteRequestWithCallback(url, requestData, deleteProductCallback, null);
}

$(document).ready(function () {
    // Function to update profit amount
    function updateProfitAmount(size) {
        const purchasePrice = parseFloat($(`#purchasePrice-${size}`).val()) || 0;
        const salePrice = parseFloat($(`#salePrice-${size}`).val()) || 0;
        const profitAmount = salePrice - purchasePrice;
        $(`#profitAmount-${size}`).val(profitAmount.toFixed(2)); // Update profit amount
    }

    function updateCallProfitAmount() {
        const purchasePrice = parseFloat($(`#purchasePrice-update`).val()) || 0;
        const salePrice = parseFloat($(`#salePrice-update`).val()) || 0;
        const profitAmount = salePrice - purchasePrice;
        $(`#profitAmount-update`).val(profitAmount.toFixed(2));
    }

    // Attach event listeners to purchase price and sale price inputs
    ['1000ML', '750ML', '375ML', '180ML', 'beer'].forEach(size => {
        $(`#purchasePrice-${size}, #salePrice-${size}`).on('input', function () {
            updateProfitAmount(size);
        });
    });

    $(`#salePrice-update`).on('input', function () {
        updateCallProfitAmount();
    });
});


function deleteProductCallback() {
    var button = $('#deleteButton');
    button.html("Deleted");
    setTimeout(function () {
        location.reload();
    }, 1000);
}


async function loadTotalCount() {
    var dashboardURL = `${webmanagerurl}/dashboard`;
    const response = await getRequest(dashboardURL);
    $("#total-stock-price").text(response.investmentAmount);
    var units = response.totalUnits;

    const totalUnitsSum = Object.values(units).reduce((sum, unit) => sum + parseFloat(unit), 0);
    $("#total-stock-units").text(Math.round(totalUnitsSum));

    var unitDetailsHTML = "";
    for (const [key, value] of Object.entries(units)) {
        if (key != "COOLDRINKS" && key != "CIGERATTE")
            unitDetailsHTML += `<div>${key} : ${Math.round(value)}</div>`;
    }

    $("#total-stock-units-category").html(unitDetailsHTML);
}


function openSkuSortModal() {
    const tbody = $("#sortableRows");
    tbody.empty();
    var products = items.productList;
    products.sort((a, b) => {
        const A = a.order ?? Infinity;
        const B = b.order ?? Infinity;
        return A - B;
    });
    products.forEach((item, index) => {
        tbody.append(`
      <tr>
        <td><input type="number" class="form-control form-control-sm order-input text-center" value="${index + 1}" style="width:60px;text-align:center;"></td>
        <td>${item.SKU}</td>
      </tr>
    `);
    });

    $("#skuSortModal").modal('show');
}

$(document).on('change', '.order-input', function () {
    let newPos = parseInt($(this).val(), 10);
    let row = $(this).closest("tr");

    if (isNaN(newPos) || newPos < 1) newPos = 1;

    const rows = $("#sortableRows tr");
    const totalRows = rows.length;

    if (newPos > totalRows) newPos = totalRows;

    // Move row to new position
    if (newPos === 1) {
        $("#sortableRows").prepend(row);
    } else {
        row.insertAfter($("#sortableRows tr").eq(newPos - 2));
    }

    updateOrderNumbers();
});

// Make the rows sortable
$("#sortableRows").sortable({
    update: function () {
        updateOrderNumbers();
    }
});

// Update Order column after sorting
function updateOrderNumbers() {
    $("#sortableRows tr").each((index, row) => {
        $(row).find(".order-number").text(index + 1);
    });
}

$("#saveSkuOrder").click(function () {
    let arrangedList = [];
    var button = $('#saveSkuOrder');
    button.prop("disabled", true).text("Saving...");
    $("#sortableRows tr").each(function (index) {
        const sku = $(this).find("td:eq(1)").text().trim();

        arrangedList.push({
            SKU: sku,
            order: index + 1
        });
    });
    var requestData = {
        productList: arrangedList
    };
    putRequestWithCallback(url + "/rearrangeItems", requestData, reArrangeProductCallback, null);

});

function reArrangeProductCallback() {
    var button = $('#saveSkuOrder');
    button.html("Saved");
    setTimeout(function () {
        location.reload();
    }, 500);
    init();
}