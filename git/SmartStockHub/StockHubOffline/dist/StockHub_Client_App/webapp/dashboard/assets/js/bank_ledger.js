$(document).ready(function() {
    console.log("Product Page is fully loaded!");
    updateShopName();
});
let url = `${webmanagerurl}/expenses`;
async function init() {
    const startDate = document.getElementById('exp-startDate').value;
    const endDate = document.getElementById('exp-endDate').value;
    const startTimestamp = Math.floor(new Date(startDate).getTime() / 1000);
    const endTimestamp = Math.floor(new Date(endDate).getTime() / 1000);
    
    if (!startDate || !endDate) {
        alert("Please select both dates before fetching expenses.");
        return;
    }
    const response = await getRequest(url+"/expensesReport?fromTime="+startTimestamp+"&toTime="+endTimestamp);
  //  const response = JSON.parse(``);

    let data = [];
    let row = 1;
    let totalAmount = 0;
    for (let i in response.data) {
        let amount = parseFloat(response.data[i].totalAmount) || 0;
        totalAmount += amount;
        data.push([
            row++,
            response.data[i].expenseDetail,
            `₹ ${response.data[i].totalAmount}`
        ]);
    }
    data.push([row, `<strong>Total</strong>`, `<strong>₹${totalAmount}</strong>`]);
    $("#totalAmountFooter").text("$ "+totalAmount);
    if ($.fn.DataTable.isDataTable('#exp-details-table')) {
        $('#exp-details-table').DataTable().destroy();
    }
    $('#exp-details-table').DataTable({
        data: data,
        columns: [
            { title: "#" },
            { title: "Expense Detail" },
            { title: "Total Amount (₹)" }
        ],
        dom: 'Bfrtip',
        buttons: [
            'copy', 'csv', 'excel', 'pdf', 'print'
        ],
        pageLength: 30,
        responsive: true
    });
}

function saveExpenses(){
    var button = $('#exp-saveButton');
    var requestData = {
        name: $('#exp-det').val()
    };

    const response = postRequest(url, requestData);
    console.log(response);
    button.html("Saved");
}
  
  
