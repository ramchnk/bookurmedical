$(document).ready(function () {
    console.log("Product Page is fully loaded!");
    init();
    const toDate = new Date();
    // const fromDate = new Date();
    // fromDate.setDate(toDate.getDate() - 30);

    flatpickr("#indent-report-sale-date", {
        enableTime: false,
        dateFormat: "d-m-Y",
        defaultDate: toDate
    });

    // flatpickr("#indent-report-from-date", {
    //     enableTime: false,
    //     dateFormat: "d-m-Y",
    //     defaultDate: fromDate
    // });

    // flatpickr("#indent-report-to-date", {
    //     enableTime: false,
    //     dateFormat: "d-m-Y",
    //     defaultDate: toDate
    // });

});
let url = getHost() + `/salescounter`;
let purchaseURL = `${webmanagerurl}/productmaster`;
var products;
let selectedItem = {};
let purchaseList = [];
let selectedBrand;

var counterItemDetails;
async function init() {
    updateShopName();
    counterItemDetails = await getRequest(url);
    //  counterItemDetails = JSON.parse(``);

    const table = $('#purchase-table').DataTable({
        data: counterItemDetails?.productList?.map((item) => [
            item.SKU,
            item.openingStock,
            item.indentStock,
            item.stock,
            '' // Placeholder for actions column
        ]),
        columns: [
            { title: "SKU" },
            { title: "Opening" },
            { title: "Indent" },
            { title: "Total" },
            {
                title: "Action",
                render: function (data, type, row) {
                    return `<div class="d-flex gap-2"><a href="#!" onclick="loadUpdateItem('${row[0]}','${row[3]}')" class="btn btn-soft-primary btn-sm" data-bs-toggle="modal" data-bs-target="#updateItemModal"><iconify-icon icon="solar:pen-2-broken" class="align-middle fs-18"></iconify-icon></a>`;
                }
            }
        ]
    });
}

$('#purchase-table').on('click', '.view-icon', function () {
    const index = $(this).data('index');
    var details = '<table class="table table-centered"><thead><tr><th scope="col">Item</th><th scope="col">Quantity</th><th scope="col">Amount</th></tr>';
    details += purchaseData.data[index].purchaseList
        .map(p => `
            <tr>
                <td> ${p.SKU}</td><td> ${p.purchaseQty}</td><td>${p.purchasePrice}</td>
            </tr>
        `)
        .join('');
    details += `</table>`;
    $('#membersalesDetails').html(details);
});

async function loadNewPurchase() {
    products = await getRequest(purchaseURL);
    // products = JSON.parse(``);
    const $dropdownMenu = $('#dropdownContent');
    $dropdownMenu.empty();
    products.productList.forEach(function (item) {
        const $item = $('<li><a class="dropdown-item" href="#">' + item.SKU + '-' + item.brand + '</a></li>');
        $dropdownMenu.append($item);
    });
}

$('#searchInput').on('input', function () {
    const query = $(this).val().toLowerCase(); // Get the input value
    const $dropdownContent = $('#dropdownContent');
    $dropdownContent.empty(); // Clear the previous dropdown items

    // Filter productList based on SKU or brand matching the query
    const filteredProducts = products?.productList?.filter(function (item) {
        return item.SKU.toLowerCase().includes(query) || item.brand.toLowerCase().includes(query);
    });

    // If there are matches, append them to the dropdown menu
    filteredProducts.forEach(function (item) {
        const $item = $('<li><a class="dropdown-item" href="#">' + item.SKU + ' - ' + item.brand + '</a></li>');
        $dropdownContent.append($item);
    });

    // Optionally, you can show/hide the dropdown based on the query
    if (filteredProducts.length > 0) {
        $dropdownContent.show();
    } else {
        $dropdownContent.hide();
    }
});
$('#dropdownContent').on('click', 'a', function () {
    const selectedItemText = $(this).text(); // Get the text of the selected item
    const selectedSKU = selectedItemText.split(' - ')[0]; // Get SKU from selected item

    const selectedBrand = selectedItemText.split(' - ')[1];

    // Find the selected product from the products list
    const selectedProduct = products.productList.find(item => item.SKU === selectedSKU && item.brand === selectedBrand);

    // Update the input field with the selected SKU and brand
    $('#searchInput').val(selectedItemText);

    // Update the price based on the selected product
    if (selectedProduct) {
        selectedItem = selectedProduct;
        $('#store-room-available').text('' + selectedProduct.stock); // Display the sale price
    }
    $('#dropdownContent').empty().hide();
});

$('#purchase-qty').on('input', function () {
    const quantity = parseInt($(this).val(), 10); // Get the quantity
    const pricePerItem = parseFloat($('#purchaseitem-amount').text().replace('$', '')); // Get the price per item, remove the dollar sign and convert to number

    // Calculate total amount
    const totalAmount = quantity * pricePerItem;

    // Update the total amount display
    $('#purchaseitem-totalAmount').text(totalAmount.toFixed(2)); // Display total with two decimal places
});

$('#addpurchaseListtable').on('click', function () {
    const quantity = parseInt($('#purchase-qty').val(), 10);
    const stock = getOpeningStockFromSalesCounter(selectedItem.SKU);
    if (selectedItem && quantity) {
        const item = {
            SKU: selectedItem.SKU,
            openingStock: stock,
            quantity: quantity,
            stock: stock + quantity
        };

        purchaseList.push(item);
        addItemToTable(item);

        // Reset fields after adding to table
        $('#searchInput').val('');
        $('#purchase-qty').val('');
        $('#purchaseitem-amount').text('0');
        $('#purchaseitem-totalAmount').text('0');
    } else {
        alert("Please select an item and enter quantity!");
    }
});

function addItemToTable(item) {
    const row = `
        <tr>
            <td>${item.SKU}</td>
            <td>${item.openingStock}</td>
            <td>${item.quantity}</td>
            <td>${item.stock}</td>
            <td>
                <a href="#!" class="btn btn-soft-primary btn-sm edit-item"><iconify-icon icon="solar:pen-2-broken" class="align-middle fs-18"></iconify-icon></a>
                <a href="#!" class="btn btn-soft-danger btn-sm delete-item"><iconify-icon icon="solar:trash-bin-minimalistic-2-broken" class="align-middle fs-18"></iconify-icon></a>
            </td>
        </tr>
    `;
    $('#purchase-details-table tbody').append(row);
}

$('#purchase-details-table').on('click', '.edit-item', function () {
    const row = $(this).closest('tr');
    const index = row.index();
    const item = purchaseList[index];

    // Populate the input fields with the item's data
    $('#searchInput').val(item.brand + ' - ' + item.SKU);
    $('#purchase-qty').val(item.quantity);
    $('#purchaseitem-amount').text(item.amountPerItem);
    $('#purchaseitem-totalAmount').text(item.totalAmount.toFixed(2));

    // Remove the item from the list for re-editing
    purchaseList.splice(index, 1);
    row.remove();
});

// Delete item functionality
$('#purchase-details-table').on('click', '.delete-item', function () {
    const row = $(this).closest('tr');
    const index = row.index();

    // Remove the item from the purchase list
    purchaseList.splice(index, 1);
    row.remove();
});

$('#saveTransfersDetails').on('click', function () {
    const button = $(this);
    button.html(`
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Saving...
    `);
    button.prop("disabled", true);

    // Create the item list
    const itemList = purchaseList.map(item => ({
        SKU: item.SKU,
        openingStock: item.openingStock,
        indentStock: item.quantity,
        stock: item.stock
    }));

    // Construct the JSON request object
    const requestData = {
        productList: itemList
    };
    putRequestWithCallback(url, requestData, updatePurchaseCalBack, null);
});

function updatePurchaseCalBack() {
    const button = $("#savePurchaseDetails");
    button.html(`
        <span class="badge bg-primary me-1">Updated</span>
    `);
    setTimeout(function () {
        location.reload();
    }, 500);
}

function loadUpdateItem(SKU, stock) {
    $("#sellerSKU-update").val(SKU);
    $("#stock-update").val(stock);
}

function updateProduct() {
    var button = $('#updateButton');

    // Add spinner and disable button
    button.html(`<div class="spinner-grow text-primary" role="status"><span class="visually-hidden">Loading...</span></div>`);
    button.prop('disabled', true);

    var requestData = {
        SKU: $('#sellerSKU-update').val(),
        stock: parseFloat($("#stock-update").val())
    };
    putRequestWithCallback(url + "/updateItem", requestData, updateProductCallback, null);
}

function updateProductCallback() {
    var button = $('#updateButton');
    button.html("Saved");
    setTimeout(function () {
        location.reload();
    }, 500);
}

$('#brand .dropdown-item').on('click', function (e) {
    e.preventDefault();
    selectedBrand = $(this).data('brand');
    $('#brandDropdown-addItem').text(selectedBrand);
});

function getOpeningStockFromSalesCounter(SKU) {
    const product = counterItemDetails?.productList?.find(item => item.SKU === SKU);
    return product ? product.stock : 0;
}

async function downloadReport() {
    // const fromDateVal = document.getElementById("indent-report-from-date").value;
    // const toDateVal = document.getElementById("indent-report-to-date").value;

    const fromDateVal = document.getElementById("indent-report-sale-date").value;
    const toDateVal = document.getElementById("indent-report-sale-date").value;

    // if (!fromDateVal || !toDateVal) {
    //     customToastAlert("indent-alert-container", "Please select both From and To dates.");
    //     return;
    // }

    const [fromDay, fromMonth, fromYear] = fromDateVal.split("-").map(Number);
    const [toDay, toMonth, toYear] = toDateVal.split("-").map(Number);

    const fromDate = new Date(fromYear, fromMonth - 1, fromDay);
    const toDate = new Date(toYear, toMonth - 1, toDay);

    // if (fromDate > toDate) {
    //     customToastAlert("indent-alert-container", "From Date cannot be later than To Date.");
    //     return;
    // }

    const now = new Date();
    if (toDate > now) {
        customToastAlert("indent-alert-container", "Date cannot be in the future.");
        return;
    }

    fromDate.setHours(0, 0, 0, 0);

    toDate.setHours(23, 59, 59, 999);

    const fromUnix = Math.floor(fromDate.getTime() / 1000);
    const toUnix = Math.floor(toDate.getTime() / 1000);

    console.log("From (Unix):", fromUnix); // start of from date
    console.log("To (Unix):", toUnix);     // end of to date

    let reportResponse = await getRequest(url + `/get?fromTime=${fromUnix}&toTime=${toUnix}`);
    console.log(reportResponse);
    if (reportResponse?.data?.length == 0) {
        customToastAlert("indent-alert-container", "No data Found");
        return;
    }

    const headers = ["Date", "SKU", "Opening SR", "Indent Qty", "Closing Qty", "Opening Stock SC", , "Purchase Stock SC", "Closing Stock SC"];

    const rows = reportResponse?.data.map(row => {
        const date = new Date(row.date * 1000);
        const formattedDate = `${String(date.getDate()).padStart(2, '0')}-${String(date.getMonth() + 1).padStart(2, '0')}-${date.getFullYear()}`;

        return [
            formattedDate,
            row.sku ?? "",
            row.openingQty ?? 0,
            row.indentQty ?? 0,
            Math.trunc((row.openingQty ?? 0) + (row.indentQty ?? 0)),
            row.openingStockPM ?? 0,
            row.purchaseStockPM ?? 0,
            row.closingStockPM ?? 0
        ];

    });


    let csvContent = "data:text/csv;charset=utf-8,"
        + [headers, ...rows].map(e => e.join(",")).join("\n");

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    const formattedNow = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}-${String(now.getMinutes()).padStart(2, '0')}-${String(now.getSeconds()).padStart(2, '0')}`;
    link.setAttribute("download", `Indent Report_${formattedNow}.csv`);
    document.body.appendChild(link); // Required for Firefox
    link.click();
    document.body.removeChild(link);

    const modalEl = document.getElementById('downloadIndentReport');
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.hide();
    customToastAlert("indent-alert-container", "Report Downloaded Successfully", "success");

}
