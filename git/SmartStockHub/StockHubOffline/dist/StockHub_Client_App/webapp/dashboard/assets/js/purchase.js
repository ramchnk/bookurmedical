$(document).ready(function () {
    console.log("Product Page is fully loaded!");
    init();
});
// let url = `${webmanagerurl}/productmaster`;
let url = productMasterurl;
let purchaseURL = `${webmanagerurl}/purchase`;
var products;
let selectedItem = {};
let purchaseList = [];
let selectedBrand;

var purchaseData;
async function init() {
    updateShopName();
    purchaseData = await getRequest(purchaseURL);
    // purchaseData = JSON.parse(``);

    const table = $('#purchase-table').DataTable({
        data: purchaseData.data.map((item, index) => [
            item.billNumber,
            item.purchaseDate, // hidden raw timestamp
            new Date(item.purchaseDate * 1000).toLocaleDateString(),
            item.billTotalAmount,
            item.billTotalUnits,
            `<div class="d-flex gap-2">
                <a href="#!" data-index="${index}" class="btn btn-light btn-sm view-icon" data-bs-toggle="modal" data-bs-target="#purchaseItems">
                    <iconify-icon icon="solar:eye-broken" class="align-middle fs-18"></iconify-icon>
                </a>
            </div>`
        ]),
        columns: [
            { title: "Bill Number" },
            { title: "Raw Date", visible: false }, // hidden timestamp column
            { title: "Purchase Date" },
            { title: "Bill Total Amount" },
            { title: "Bill Total Units" },
            { title: "Purchase Details" }
        ],
        order: [[1, 'desc']] // sort by hidden timestamp column
    });
}

// Flag to prevent duplicate clicks while saving product
let isSavingProduct = false;

async function saveProduct() {
    // Prevent duplicate clicks
    if (isSavingProduct) {
        return;
    }

    var button = $('#saveButton');
    const originalButtonText = button.html();

    const sellerSKU = $('#sellerSKU').val().trim();
    const radioValue = $('input[name="inlineRadioOptions"]:checked').val();

    // Validation before showing spinner
    if (!sellerSKU) {
        alert('Please fill out Item Name fields before saving.');
        return false;
    }
    if (!radioValue) {
        alert('Please Select type fields before saving.');
        return false;
    }

    // Validate Beer ML selection if Beer type is selected
    const beerMLValue = $('input[name="beerMLOptions"]:checked').val();
    if (radioValue === "Beer" && !beerMLValue) {
        alert('Please select ML size for Beer.');
        return false;
    }

    // Set flag and show spinner immediately after basic validation
    isSavingProduct = true;
    button.html(`<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Saving...`);
    button.prop('disabled', true);

    try {
        var items = await getRequest(url);
        if (items.data) items = items.data;

        // Initialize an empty details array
        var details = [];
        let skuExists = false;

        // Define sizes to iterate through
        // For Beer type, use the selected ML size; for others use liquor sizes
        let sizes;
        if (radioValue === "Beer" || radioValue === "COOLDRINKS" || radioValue === "CIGERATTE") {
            // Use the selected Beer ML size or 'beer' as fallback
            sizes = [beerMLValue || "beer"];
        } else {
            sizes = ["1000ML", "750ML", "375ML", "180ML"];
        }

        // Iterate through each size
        sizes.forEach(size => {
            // Determine which input fields to use based on type
            const inputSuffix = (radioValue === "Beer" || radioValue === "COOLDRINKS" || radioValue === "CIGERATTE") ? "beer" : size;

            const stock = parseInt($(`#stock-${inputSuffix}`).val()) || 0;
            const purchasePrice = parseFloat($(`#purchasePrice-${inputSuffix}`).val()) || 0;
            const salePrice = parseFloat($(`#salePrice-${inputSuffix}`).val()) || 0;

            if (purchasePrice > 0 && salePrice > 0 || (size === '60ML' && ['liquor', 'wine'].includes(radioValue?.toLowerCase()))) { // Only include if stock is provided and greater than 0
                const fullSKU = `${sellerSKU}-${size}`;
                skuExists = items.productList.some(item => item.SKU.toLowerCase() === fullSKU.toLowerCase());
                if (skuExists) {
                    alert(`The SKU "${sellerSKU}"-"${size}" already exists. Please enter a unique SKU.`);
                    return false;
                }

                details.push({
                    size: size,
                    stock: stock,
                    purchasePrice: parseFloat($(`#purchasePrice-${inputSuffix}`).val()) || 0,
                    salePrice: parseFloat($(`#salePrice-${inputSuffix}`).val()) || 0,
                    profitAmount: parseFloat($(`#profitAmount-${inputSuffix}`).val()) || 0
                });
            }
        });

        if (skuExists) {
            // Re-enable button if SKU exists
            isSavingProduct = false;
            button.html(originalButtonText);
            button.prop('disabled', false);
            return;
        }

        if (selectedBrand == "Beer") {
            selectedBrand = "BEER";
        }
        // Construct the JSON data
        var requestData = {
            SKU: $('#sellerSKU').val(),
            brand: selectedBrand,
            category: $('input[name="inlineRadioOptions"]:checked').val(),
            details: details
        };
        console.log(requestData);
        postRequestWithCallback(url, requestData, saveProductCallback, saveProductErrorCallback);
    } catch (error) {
        // Handle any errors and re-enable button
        console.error('Error saving product:', error);
        isSavingProduct = false;
        button.html(originalButtonText);
        button.prop('disabled', false);
        alert('An error occurred while saving the product. Please try again.');
    }
}

function saveProductCallback() {
    var button = $('#saveButton');
    button.html(`<span class="badge bg-success me-1">Saved</span>`);
    setTimeout(function () {
        isSavingProduct = false;
        location.reload();
    }, 1000);
}

function saveProductErrorCallback() {
    var button = $('#saveButton');
    isSavingProduct = false;
    button.html('Save Product');
    button.prop('disabled', false);
    alert('Failed to save product. Please try again.');
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

let currentIndex = -1;

function enableKeyboardNavigation() {
    const $input = $('#searchInput');
    const $menu = $('#dropdownContent');

    // Typing Filter
    $input.on('input', function () {
        const val = $(this).val().toLowerCase();
        const $items = $menu.find('.dropdown-item');

        $items.each(function () {
            $(this).toggle($(this).text().toLowerCase().includes(val));
        });

        currentIndex = -1;
        updateHighlight($menu.find('.dropdown-item:visible'));
        $input.dropdown('show'); // Keep dropdown open while typing
    });

    $(document).on('keydown', function (e) {
        const $visibleItems = $menu.find('.dropdown-item:visible');
        if (!$visibleItems.length) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            currentIndex = (currentIndex + 1) % $visibleItems.length;
            updateHighlight($visibleItems);
        }
        else if (e.key === 'ArrowUp') {
            e.preventDefault();
            currentIndex = (currentIndex - 1 + $visibleItems.length) % $visibleItems.length;
            updateHighlight($visibleItems);
        }
        else if (e.key === 'Enter') {
            e.preventDefault();

            if (currentIndex < 0) {
                currentIndex = 0;
            }

            $visibleItems.eq(currentIndex).click();
            $("#purchase-case").focus();
        }
    });

    // Click Selection
    $menu.on('click', '.dropdown-item', function () {
        $('#searchInput').val($(this).text()).blur();
        $menu.hide();
    });
}

function updateHighlight($items) {
    $items.removeClass('highlight');
    if (currentIndex >= 0) {
        $items.eq(currentIndex).addClass('highlight')[0].scrollIntoView({ block: 'nearest' });
    }
}

async function loadNewPurchase() {
    products = await getRequest(url);
    if (products.data) products = products.data;
    // products = JSON.parse(``);
    const $dropdownMenu = $('#dropdownContent');
    $dropdownMenu.empty();
    products.productList.forEach(function (item) {
        const $item = $('<li><a class="dropdown-item" href="#">' + item.SKU + '-' + item.brand + '</a></li>');
        $dropdownMenu.append($item);
    });

    if ('hasDraftPurchase' in products && products.hasDraftPurchase === true) {
        loadDraftPurchase();
    }
    enableKeyboardNavigation();
}

$('#searchInput').on('input', function () {
    const query = $(this).val().toLowerCase(); // Get the input value
    const $dropdownContent = $('#dropdownContent');
    $dropdownContent.empty(); // Clear the previous dropdown items

    // Filter productList based on SKU or brand matching the query
    const filteredProducts = products.productList.filter(function (item) {
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

    const selectedBrand = selectedItemText.split(' - ')[1]; // Get brand from selected item

    // Find the selected product from the products list
    const selectedProduct = products.productList.find(item => item.SKU === selectedSKU && item.brand === selectedBrand);

    // Update the input field with the selected SKU and brand
    $('#searchInput').val(selectedItemText);

    // Update the price based on the selected product
    if (selectedProduct) {
        selectedItem = selectedProduct;
        $('#purchaseitem-amount').text('' + selectedProduct.purchasePrice); // Display the sale price
    } else {
        $('#purchaseitem-amount').text('0');
    }

    // Clear the dropdown content and hide it
    $('#dropdownContent').empty().hide();
});

$('#purchase-qty').on('input', function () {
    updatePurchasePriceTotalForItem();
});

function updatePurchasePriceTotalForItem() {
    const quantity = parseInt($("#purchase-qty").val(), 10); // Get the quantity
    const pricePerItem = parseFloat($('#purchaseitem-amount').text().replace('$', '')); // Get the price per item, remove the dollar sign and convert to number

    // Calculate total amount
    const totalAmount = quantity * pricePerItem;

    // Update the total amount display
    $('#purchaseitem-totalAmount').text(totalAmount.toFixed(2)); // Display total with two decimal places
}

async function loadDraftPurchase() {
    var draftPurchase = await getRequest(purchaseURL + '/draft');
    // var draftPurchase = JSON.parse(``);
    draftPurchase.data[0].productList.forEach(function (draft) {
        const item = {
            brand: "",
            SKU: draft.SKU,
            quantity: draft.purchaseStock,
            openingStock: draft.openingStock,
            stock: draft.stock,
            amountPerItem: draft.purchaseAmount / draft.purchaseStock,
            totalAmount: draft.purchaseAmount,
            cases: draft.cases ?? 0
        };

        purchaseList.push(item);
        addItemToTable(item);
    });
    $('#billNumber').val(draftPurchase.data[0].billNo);
    updateTotals(draftPurchase.data[0].totalQuantity);
}

// Listen Enter key in case box
$('#purchase-case').on('keydown', function (e) {
    if (e.key === 'Enter' || e.keyCode === 13) {
        e.preventDefault(); // stop form submission or default behavior
        $('#addpurchaseListtable').click(); // trigger your add method
    }
});

$('#addpurchaseListtable').on('click', function () {
    const quantity = parseInt($('#purchase-qty').val(), 10);
    const totalAmount = parseFloat($('#purchaseitem-totalAmount').text());
    const cases = parseInt($('#purchase-case').val(), 10);
    if (selectedItem && quantity && totalAmount > 0) {
        const item = {
            brand: selectedItem.brand,
            SKU: selectedItem.SKU,
            quantity: quantity,
            openingStock: selectedItem.stock,
            stock: selectedItem.stock + quantity,
            amountPerItem: selectedItem.purchasePrice,
            totalAmount: totalAmount,
            cases: cases
        };

        purchaseList.push(item);
        addItemToTable(item);

        // Reset fields after adding to table
        $('#searchInput').val('');
        $('#purchase-qty').val('');
        $('#purchaseitem-amount').text('0');
        $('#purchaseitem-totalAmount').text('0');
        $('#purchase-case').val('0');
        $('#purchase-loose').val('0');
        updateTotals();
        $('#searchInput').focus();

    } else {
        alert("Please select an item and enter quantity!");
    }
});

function addItemToTable(item) {
    const serialNo = $('#purchase-details-table tbody tr').length + 1;
    const row = `
        <tr>
            <td>${serialNo}</td>
            <td>${item.SKU}</td>
            <td>${item.quantity}</td>
            <td>${item.openingStock}</td>
            <td>${item.stock}</td>
            <td>${item.amountPerItem}</td>
            <td>${item.totalAmount.toFixed(2)}</td>
            <td>
                <a href="#!" class="btn btn-soft-danger btn-sm delete-item"><iconify-icon icon="solar:trash-bin-minimalistic-2-broken" class="align-middle fs-18"></iconify-icon></a>
            </td>
        </tr>
    `;
    $('#purchase-details-table tbody').append(row);
}

//Case and loose bottles calculation
let caseUnit = 0; // default case unit

function updateQuantity() {
    const caseCount = parseInt($('#purchase-case').val()) || 0;
    const looseCount = parseInt($('#purchase-loose').val()) || 0;
    var caseUnit = getCaseCount(extractSize($("#searchInput").val()));
    let total = looseCount;

    if (caseUnit > 0 && caseCount > 0) {
        total += caseCount * caseUnit;
    }

    $('#purchase-qty').val(total);
    updatePurchasePriceTotalForItem();
}

// Dropdown value selection
function setCaseUnit(value) {
    caseUnit = value;
    updateQuantity();
}

function extractSize(str) {
    const match = str.match(/(\d+(?:ML|ml|Ml|mL))/);
    return match ? match[1] : null;
}

function getCaseCount(sizeInML) {
    const map = {
        "180ML": 48,
        "375ML": 24,
        "325ML": 24,
        "500ML": 24,
        "750ML": 12,
        "650ML": 12,
        "1000ML": 9
    };
    return map[sizeInML] || 0;
}

// Event listeners
$(document).ready(function () {
    $('#purchase-case, #purchase-loose').on('input', function () {
        updateQuantity();
    });
});



// Delete item functionality
$('#purchase-details-table').on('click', '.delete-item', function () {
    const row = $(this).closest('tr');
    const index = row.index();

    // Remove the item from the purchase list
    purchaseList.splice(index, 1);
    row.remove();
    updateTotals();
    updateSerialNumbers();
});

function updateSerialNumbers() {
    $('#purchase-details-table tbody tr').each(function (index) {
        $(this).find('td:first').text(index + 1);
    });
}

function updateTotals(draftTotQty = null) {
    let totalQty = 0;
    let totalAmount = 0;

    // Loop through the purchaseList to calculate totals
    purchaseList.forEach(item => {
        totalQty += item.cases;
        totalAmount += item.totalAmount;
    });

    // Update the total display
    if (draftTotQty === null) {
        $('#totalQty').text(totalQty);
    } else {
        $('#totalQty').text(draftTotQty);
    }
    $('#totalAmount').text(totalAmount.toFixed(2));
}

$('#savePurchaseDetails').on('click', function () {
    const button = $(this);
    button.prop("disabled", true);
    const billNo = $('#billNumber').val().trim();
    const purchaseDate = Math.floor(new Date($('#purchaseDate').val()).getTime() / 1000);
    const totalQuantity = parseInt($('#totalQty').text(), 10);
    const purchaseAmount = parseFloat($('#totalAmount').text()).toFixed(2);

    if (!billNo || isNaN(purchaseDate) || totalQuantity === 0 || purchaseAmount === "0.00") {
        alert("Please fill in all the required fields and add items to the list.");
        return;
    }
    button.html(`
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Saving...
    `);

    // Create the item list
    const itemList = purchaseList.map(item => ({
        SKU: item.SKU,
        openingStock: item.openingStock,
        purchaseStock: item.quantity,
        stock: item.stock,
        purchaseAmount: item.totalAmount
    }));

    // Construct the JSON request object
    const requestData = {
        billNo: billNo,
        purchaseDate: purchaseDate,
        purchaseAmount: purchaseAmount,
        totalQuantity: totalQuantity,
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

$('#saveDraftPurchaseDetails').on('click', function () {
    const button = $(this);
    button.html(`
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        Saving...
    `);
    button.prop("disabled", true);
    const billNo = $('#billNumber').val().trim();
    const purchaseDate = Math.floor(new Date($('#purchaseDate').val()).getTime() / 1000);
    const totalQuantity = parseInt($('#totalQty').text(), 10);
    const purchaseAmount = parseFloat($('#totalAmount').text()).toFixed(2);

    if (!billNo || isNaN(purchaseDate) || totalQuantity === 0 || purchaseAmount === "0.00") {
        alert("Please fill in all the required fields and add items to the list.");
        return;
    }

    // Create the item list
    const itemList = purchaseList.map(item => ({
        SKU: item.SKU,
        openingStock: item.openingStock,
        purchaseStock: item.quantity,
        stock: item.stock,
        purchaseAmount: item.totalAmount,
        cases: item.cases
    }));

    // Construct the JSON request object
    const requestData = {
        billNo: billNo,
        purchaseDate: purchaseDate,
        purchaseAmount: purchaseAmount,
        totalQuantity: totalQuantity,
        productList: itemList
    };
    postRequestWithCallback(purchaseURL + '/draft', requestData, updateDraftPurchaseCalBack, null);
});

function updateDraftPurchaseCalBack() {
    const button = $("#saveDraftPurchaseDetails");
    button.html(`
        <span class="badge btn-info me-1">Draft Added</span>
    `);
}

$(document).ready(function () {
    $('input[name="inlineRadioOptions"]').change(function () {
        const selectedValue = $(this).val();

        // Reset Beer ML selection when type changes
        $('input[name="beerMLOptions"]').prop('checked', false);

        if (selectedValue == "Beer") {
            $('#ram-beer').show();
            $('#ram-liquor').hide();
            $('#brandName').show();
            $('#brandName').text("BEER");
            $('#brandDropdown-addItem').hide();
            selectedBrand = "BEER";
        }
        if (selectedValue == "COOLDRINKS" || selectedValue == "CIGERATTE") {
            $('#ram-beer').show();
            $('#ram-liquor').hide();
            $('#brandName').show();
            $('#brandName').text(selectedValue);
            $('#brandDropdown-addItem').hide();
            selectedBrand = selectedValue;
        }
        if (selectedValue == "Wine") {
            $('#ram-liquor').show();
            $('#ram-beer').hide();
            $('#brandName').show();
            $('#brandName').text("WINE");
            $('#brandDropdown-addItem').hide();
            selectedBrand = "WINE";
        }
        if (selectedValue == "Liquor") {
            $('#ram-liquor').show();
            $('#ram-beer').hide();
            $('#brandDropdown-addItem').show();
            $('#brandDropdown-addItem').text("Select Brand");
            selectedBrand = "BRANDY";
            $('#brandName').hide();
        }
    });
});

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

$('#brand .dropdown-item').on('click', function (e) {
    e.preventDefault();
    selectedBrand = $(this).data('brand');
    $('#brandDropdown-addItem').text(selectedBrand);
});
