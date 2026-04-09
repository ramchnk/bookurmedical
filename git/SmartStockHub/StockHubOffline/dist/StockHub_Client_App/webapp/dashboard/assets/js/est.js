$(document).ready(function() {
    console.log("Product Page is fully loaded!");
    updateShopName();
});

let responseData;

async function initEst() {
    const startDate = document.getElementById('pl-startDate').value;
    const endDate = document.getElementById('pl-endDate').value;
    const startTimestamp = Math.floor(new Date(startDate).getTime() / 1000);
    const endTimestamp = Math.floor(new Date(endDate).getTime() / 1000);
    
    if (!startDate || !endDate) {
        alert("Please select both dates before fetching Report.");
        return;
    }

    const maxDays = 30;
    const timeDiff = (endDate - startDate) / (1000 * 60 * 60 * 24);
    if (timeDiff > maxDays) {
        alert(`Date range cannot exceed ${maxDays} days.`);
        endDateInput.value = ''; 
        return;
    }
    document.getElementById("spinner").style.display = "inline-block";
    document.getElementById("fetch-expenses").disabled = true;
    try {
        let url = `${webmanagerurl}/sales/estimate`;
      //  responseData = await getRequest(url+"?startDate="+startTimestamp+"&endDate="+endTimestamp);
        responseData = JSON.parse(`{"data":[{"totalSaleAmount":1254900,"averageDailyQty":1394.3333333333333,"totalSalesQty":8366,"totalProfitAmount":108336.10012817383,"SKU":"Black Pearl Brandy-180ML"},{"totalSaleAmount":579500,"averageDailyQty":508.3333333333333,"totalSalesQty":3050,"totalProfitAmount":104824.71542358398,"SKU":"kingfisher Maganam-650ML"},{"totalSaleAmount":431400,"averageDailyQty":479.3333333333333,"totalSalesQty":2876,"totalProfitAmount":36869.342697143555,"SKU":"Dimond Rum-180ML"},{"totalSaleAmount":339480,"averageDailyQty":314.3333333333333,"totalSalesQty":1886,"totalProfitAmount":26644.993005752563,"SKU":"Mc Brandy-180ML"},{"totalSaleAmount":224100,"averageDailyQty":249,"totalSalesQty":1494,"totalProfitAmount":19527.9102973938,"SKU":"Express Brandy-180ML"},{"totalSaleAmount":258840,"averageDailyQty":239.66666666666666,"totalSalesQty":1438,"totalProfitAmount":20315.747583389282,"SKU":"Honey Bee Brandy-180ML"},{"totalSaleAmount":147960,"averageDailyQty":137,"totalSalesQty":822,"totalProfitAmount":10269.780658721924,"SKU":"Mgm Orange Vodka-180ML"},{"totalSaleAmount":65400,"averageDailyQty":72.66666666666667,"totalSalesQty":436,"totalProfitAmount":5698.908226013184,"SKU":"MC LENE Ordinary Brandy-180ML"},{"totalSaleAmount":79610,"averageDailyQty":69.83333333333333,"totalSalesQty":419,"totalProfitAmount":14400.51008605957,"SKU":"British Empire Beer-650ML"},{"totalSaleAmount":102080,"averageDailyQty":58.666666666666664,"totalSalesQty":352,"totalProfitAmount":7091.69140625,"SKU":"Bacardi Limon-180ML"},{"totalSaleAmount":79800,"averageDailyQty":44.333333333333336,"totalSalesQty":266,"totalProfitAmount":7665.034687042236,"SKU":"Black Pearl Brandy-375ML"},{"totalSaleAmount":71820,"averageDailyQty":44.333333333333336,"totalSalesQty":266,"totalProfitAmount":6601.497394561768,"SKU":"King Louis Xo Brandy-180ML"},{"totalSaleAmount":70760,"averageDailyQty":40.666666666666664,"totalSalesQty":244,"totalProfitAmount":6383.605880737305,"SKU":"Louis Verant Brandy-180ML"},{"totalSaleAmount":52360,"averageDailyQty":39.666666666666664,"totalSalesQty":238,"totalProfitAmount":5620.103477478027,"SKU":"Mc Vsop Brandy-180ML"},{"totalSaleAmount":63000,"averageDailyQty":37.5,"totalSalesQty":225,"totalProfitAmount":5673.793458938599,"SKU":"Morpheus Brandy-180ML"},{"totalSaleAmount":36360,"averageDailyQty":33.666666666666664,"totalSalesQty":202,"totalProfitAmount":6938.27587890625,"SKU":"Snj 10000-650ML"},{"totalSaleAmount":69840,"averageDailyQty":32.333333333333336,"totalSalesQty":194,"totalProfitAmount":6165.343444824219,"SKU":"Mc Brandy-375ML"},{"totalSaleAmount":51840,"averageDailyQty":32,"totalSalesQty":192,"totalProfitAmount":4764.9906005859375,"SKU":"1848 Brandy-180ML"},{"totalSaleAmount":43250,"averageDailyQty":28.833333333333332,"totalSalesQty":173,"totalProfitAmount":4712.6377601623535,"SKU":"Old Monk Gold Re Super Rum-180ML"},{"totalSaleAmount":24820,"averageDailyQty":24.333333333333332,"totalSalesQty":146,"totalProfitAmount":5939.625907897949,"SKU":"British Empire Beer-500ML"},{"totalSaleAmount":34560,"averageDailyQty":21.333333333333332,"totalSalesQty":128,"totalProfitAmount":3176.660400390625,"SKU":"Chevalier Brandy-180ML"},{"totalSaleAmount":30900,"averageDailyQty":17.166666666666668,"totalSalesQty":103,"totalProfitAmount":2760.8985290527344,"SKU":"Juno Pink Vodka-180ML"},{"totalSaleAmount":22080,"averageDailyQty":15.333333333333334,"totalSalesQty":92,"totalProfitAmount":2614.8130264282227,"SKU":"Dutchman Exclusive Brandy-180ML"},{"totalSaleAmount":20470,"averageDailyQty":14.833333333333334,"totalSalesQty":89,"totalProfitAmount":3239.6303520202637,"SKU":"King Fisher ultra max-650ML"},{"totalSaleAmount":16020,"averageDailyQty":14.833333333333334,"totalSalesQty":89,"totalProfitAmount":1127.5756855010986,"SKU":"Power Orange Vodka-180ML"},{"totalSaleAmount":24600,"averageDailyQty":13.666666666666666,"totalSalesQty":82,"totalProfitAmount":2158.415458679199,"SKU":"Dimond Rum-375ML"},{"totalSaleAmount":29160,"averageDailyQty":13.5,"totalSalesQty":81,"totalProfitAmount":2574.1897888183594,"SKU":"Honey Bee Brandy-375ML"},{"totalSaleAmount":14630,"averageDailyQty":12.833333333333334,"totalSalesQty":77,"totalProfitAmount":2646.3944549560547,"SKU":"Foster S Original Strong-650ML"},{"totalSaleAmount":17020,"averageDailyQty":12.333333333333334,"totalSalesQty":74,"totalProfitAmount":2693.6252365112305,"SKU":"British Empire Beer MALT-650ML"},{"totalSaleAmount":13000,"averageDailyQty":10.833333333333334,"totalSalesQty":65,"totalProfitAmount":2354.5787811279297,"SKU":"Godfather-650ML"},{"totalSaleAmount":11800,"averageDailyQty":9.833333333333334,"totalSalesQty":59,"totalProfitAmount":2137.2330474853516,"SKU":"Amstel Beer-650ML"},{"totalSaleAmount":16240,"averageDailyQty":9.666666666666666,"totalSalesQty":58,"totalProfitAmount":1494.7475318908691,"SKU":"Signature Whishy-180ML"},{"totalSaleAmount":14040,"averageDailyQty":8.666666666666666,"totalSalesQty":52,"totalProfitAmount":1290.5182876586914,"SKU":"MH reserve Brandy-180ML"},{"totalSaleAmount":18620,"averageDailyQty":8.166666666666666,"totalSalesQty":49,"totalProfitAmount":1888.7344436645508,"SKU":"Morpheus Blue Brandy-180ML"},{"totalSaleAmount":13500,"averageDailyQty":7.5,"totalSalesQty":45,"totalProfitAmount":1269.4085884094238,"SKU":"Men Club Brandy-375ML"},{"totalSaleAmount":10920,"averageDailyQty":6.5,"totalSalesQty":39,"totalProfitAmount":952.1900939941406,"SKU":"RC Whishy-180ML"},{"totalSaleAmount":9250,"averageDailyQty":6.166666666666667,"totalSalesQty":37,"totalProfitAmount":740.3574466705322,"SKU":"Mgm Gold Vsop-180ML"},{"totalSaleAmount":7700,"averageDailyQty":5.833333333333333,"totalSalesQty":35,"totalProfitAmount":792.8718185424805,"SKU":"Old Nepolian-180ML"},{"totalSaleAmount":10500,"averageDailyQty":5.833333333333333,"totalSalesQty":35,"totalProfitAmount":938.1694030761719,"SKU":"Juno Orange Vodka-180ML"},{"totalSaleAmount":9450,"averageDailyQty":5.833333333333333,"totalSalesQty":35,"totalProfitAmount":868.6180782318115,"SKU":"Daes Marquis-180ML"},{"totalSaleAmount":9860,"averageDailyQty":5.666666666666667,"totalSalesQty":34,"totalProfitAmount":705.6564178466797,"SKU":"British Empire Brandy-180ML"},{"totalSaleAmount":5580,"averageDailyQty":5.166666666666667,"totalSalesQty":31,"totalProfitAmount":414.7133369445801,"SKU":"Gold Maker White Rum-180ML"},{"totalSaleAmount":4500,"averageDailyQty":5,"totalSalesQty":30,"totalProfitAmount":1130.4000091552734,"SKU":"Misty Vintage Red Wine-180ML"},{"totalSaleAmount":16820,"averageDailyQty":4.833333333333333,"totalSalesQty":29,"totalProfitAmount":1855.7796325683594,"SKU":"Louis Verant Brandy-375ML"},{"totalSaleAmount":6500,"averageDailyQty":4.166666666666667,"totalSalesQty":25,"totalProfitAmount":593.7544822692871,"SKU":"Snj No 1 vsop Brandy-180ML"},{"totalSaleAmount":3500,"averageDailyQty":4.166666666666667,"totalSalesQty":25,"totalProfitAmount":1005.5000305175781,"SKU":"Red Sea Classic-180ML"},{"totalSaleAmount":6480,"averageDailyQty":4,"totalSalesQty":24,"totalProfitAmount":470.2089385986328,"SKU":"Magic Moment Chocolate Vodka-180ML"},{"totalSaleAmount":13440,"averageDailyQty":4,"totalSalesQty":24,"totalProfitAmount":1268.7422790527344,"SKU":"Morpheus Brandy-375ML"},{"totalSaleAmount":6720,"averageDailyQty":4,"totalSalesQty":24,"totalProfitAmount":605.2046356201172,"SKU":"Magic Moment Apple Vodka-180ML"},{"totalSaleAmount":26880,"averageDailyQty":4,"totalSalesQty":24,"totalProfitAmount":2654.1311645507812,"SKU":"Morpheus Brandy-750ML"},{"totalSaleAmount":9680,"averageDailyQty":3.6666666666666665,"totalSalesQty":22,"totalProfitAmount":1081.7670669555664,"SKU":"Mc Vsop Brandy-375ML"},{"totalSaleAmount":15200,"averageDailyQty":3.3333333333333335,"totalSalesQty":20,"totalProfitAmount":1524.788818359375,"SKU":"Morpheus Blue Brandy-375ML"},{"totalSaleAmount":5400,"averageDailyQty":3.3333333333333335,"totalSalesQty":20,"totalProfitAmount":514.4380187988281,"SKU":"MGM GOVA VODKA-180ML"},{"totalSaleAmount":9180,"averageDailyQty":2.8333333333333335,"totalSalesQty":17,"totalProfitAmount":1019.4055213928223,"SKU":"King Louis Xo Brandy-375ML"},{"totalSaleAmount":9280,"averageDailyQty":2.6666666666666665,"totalSalesQty":16,"totalProfitAmount":757.4703979492188,"SKU":"Bacardi Limon-375ML"},{"totalSaleAmount":17920,"averageDailyQty":2.6666666666666665,"totalSalesQty":16,"totalProfitAmount":1660.454345703125,"SKU":"RC Whishy-750ML"},{"totalSaleAmount":3600,"averageDailyQty":2.5,"totalSalesQty":15,"totalProfitAmount":388.04609298706055,"SKU":"MH Ultra Brandy-180ML"},{"totalSaleAmount":13200,"averageDailyQty":2.5,"totalSalesQty":15,"totalProfitAmount":1551.6879272460938,"SKU":"Mc Vsop Brandy-750ML"},{"totalSaleAmount":3380,"averageDailyQty":2.1666666666666665,"totalSalesQty":13,"totalProfitAmount":308.76105880737305,"SKU":"Courrier Nepoleon ultra french Brandy-180ML"},{"totalSaleAmount":7020,"averageDailyQty":2.1666666666666665,"totalSalesQty":13,"totalProfitAmount":779.5453987121582,"SKU":"1848 Brandy-375ML"},{"totalSaleAmount":13920,"averageDailyQty":2,"totalSalesQty":12,"totalProfitAmount":1270.3787841796875,"SKU":"Bacardi Limon-750ML"},{"totalSaleAmount":5500,"averageDailyQty":1.8333333333333333,"totalSalesQty":11,"totalProfitAmount":510.7369270324707,"SKU":"Hobsons Brandy-180ML"},{"totalSaleAmount":5400,"averageDailyQty":1.6666666666666667,"totalSalesQty":10,"totalProfitAmount":599.6503067016602,"SKU":"Chevalier Brandy-375ML"},{"totalSaleAmount":5000,"averageDailyQty":1.6666666666666667,"totalSalesQty":10,"totalProfitAmount":593.4337997436523,"SKU":"Old Monk Gold Re Super Rum-375ML"},{"totalSaleAmount":2400,"averageDailyQty":1.6666666666666667,"totalSalesQty":10,"totalProfitAmount":222.84400939941406,"SKU":"Royal Accord Brandy-180ML"},{"totalSaleAmount":6000,"averageDailyQty":1.6666666666666667,"totalSalesQty":10,"totalProfitAmount":561.8114852905273,"SKU":"Dimond Rum-750ML"},{"totalSaleAmount":960,"averageDailyQty":1.3333333333333333,"totalSalesQty":8,"totalProfitAmount":262.9303283691406,"SKU":"Spiel Beer-325ML"},{"totalSaleAmount":1920,"averageDailyQty":1.3333333333333333,"totalSalesQty":8,"totalProfitAmount":227.37135314941406,"SKU":"Imperial vs Reserv Brandy-180ML"},{"totalSaleAmount":8640,"averageDailyQty":1.3333333333333333,"totalSalesQty":8,"totalProfitAmount":879.7376098632812,"SKU":"Chevalier Brandy-750ML"},{"totalSaleAmount":6560,"averageDailyQty":1.3333333333333333,"totalSalesQty":8,"totalProfitAmount":773.7463989257812,"SKU":"Dimond Rum-1000ML"},{"totalSaleAmount":1680,"averageDailyQty":1.1666666666666667,"totalSalesQty":7,"totalProfitAmount":255.5,"SKU":"Fosters special malt beer 650ml "},{"totalSaleAmount":7000,"averageDailyQty":1.1666666666666667,"totalSalesQty":7,"totalProfitAmount":837.6146850585938,"SKU":"Old Monk Gold Re Super Rum-750ML"},{"totalSaleAmount":7200,"averageDailyQty":1,"totalSalesQty":6,"totalProfitAmount":700.1705932617188,"SKU":"Bacardi Black-750ML"},{"totalSaleAmount":3000,"averageDailyQty":1,"totalSalesQty":6,"totalProfitAmount":252.506103515625,"SKU":"Mgm Gold Vsop-375ML"},{"totalSaleAmount":5600,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":549.5389938354492,"SKU":"Signature Whishy-750ML"},{"totalSaleAmount":2400,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":280.5788993835449,"SKU":"Dutchman Exclusive Brandy-375ML"},{"totalSaleAmount":5400,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":549.8360061645508,"SKU":"1848 Brandy-750ML"},{"totalSaleAmount":7600,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":740.5329895019531,"SKU":"Morpheus Blue Brandy-750ML"},{"totalSaleAmount":5800,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":638.7215042114258,"SKU":"Louis Verant Brandy-750ML"},{"totalSaleAmount":5400,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":549.8360061645508,"SKU":"King Louis Xo Brandy-750ML"},{"totalSaleAmount":1400,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":145.83419799804688,"SKU":"Holands Brandy-180ML"},{"totalSaleAmount":850,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":171.65449142456055,"SKU":"Black Pearl-650ML"},{"totalSaleAmount":2700,"averageDailyQty":0.8333333333333334,"totalSalesQty":5,"totalProfitAmount":299.8251533508301,"SKU":"MH reserve Brandy-375ML"},{"totalSaleAmount":4320,"averageDailyQty":0.6666666666666666,"totalSalesQty":4,"totalProfitAmount":329.024169921875,"SKU":"Magic Moment Chocolate Vodka-750ML"},{"totalSaleAmount":1120,"averageDailyQty":0.6666666666666666,"totalSalesQty":4,"totalProfitAmount":97.70612335205078,"SKU":"MGM IND Challenge Whisky-180ML"},{"totalSaleAmount":1360,"averageDailyQty":0.6666666666666666,"totalSalesQty":4,"totalProfitAmount":128.5965576171875,"SKU":"HICHKI GIN-180ML"},{"totalSaleAmount":1140,"averageDailyQty":0.5,"totalSalesQty":3,"totalProfitAmount":100.33026123046875,"SKU":"Antiquity Blue Whisky-180ML"},{"totalSaleAmount":3360,"averageDailyQty":0.5,"totalSalesQty":3,"totalProfitAmount":331.76639556884766,"SKU":"Magic Moment Apple Vodka-750ML"},{"totalSaleAmount":3240,"averageDailyQty":0.5,"totalSalesQty":3,"totalProfitAmount":329.90160369873047,"SKU":"MH reserve Brandy-750ML"},{"totalSaleAmount":5900,"averageDailyQty":0.3333333333333333,"totalSalesQty":2,"totalProfitAmount":711.1923828125,"SKU":"Jhonnie walker REDlable Whisky-750ML"},{"totalSaleAmount":700,"averageDailyQty":0.3333333333333333,"totalSalesQty":2,"totalProfitAmount":84.29920196533203,"SKU":"Shipping Spirit Pink Vodka-180ML"},{"totalSaleAmount":1440,"averageDailyQty":0.3333333333333333,"totalSalesQty":2,"totalProfitAmount":107.47232055664062,"SKU":"Mgm Orange Vodka-750ML"},{"totalSaleAmount":2000,"averageDailyQty":0.3333333333333333,"totalSalesQty":2,"totalProfitAmount":169.30799865722656,"SKU":"Mgm Gold Vsop-750ML"},{"totalSaleAmount":960,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":107.8511962890625,"SKU":"Dutchman Exclusive Brandy-750ML"},{"totalSaleAmount":290,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":27.989999771118164,"SKU":"Wassup vodka-180ML"},{"totalSaleAmount":780,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":100.19000244140625,"SKU":"Blenders Pride-375ML"},{"totalSaleAmount":1080,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":109.96720123291016,"SKU":"Daes Marquis-750ML"},{"totalSaleAmount":520,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":51.025360107421875,"SKU":"Snj No 1vsop Brandy-375ML"},{"totalSaleAmount":1040,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":103.51100158691406,"SKU":"COURRIER NAPOLEON ULTRA FRE BRANDY-750ML"},{"totalSaleAmount":960,"averageDailyQty":0.16666666666666666,"totalSalesQty":1,"totalProfitAmount":106.63919830322266,"SKU":"MH Ultra Brandy-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Juno Pink Vodka-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Bhutanese Wheat Beer-330ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Jhonnie walker Double BLack-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Hobbs XO-180ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Marcel Xo Brandy-180ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Juno Orange Vodka-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Accord Orange Vodka-180ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Antiquity Blue Whisky-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Black Label-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Men Club Brandy-180ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"British Empire Brandy-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Royal Accord Brandy-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Tanqueray London Dry Gin-750ML"},{"totalSaleAmount":0,"averageDailyQty":0,"totalSalesQty":0,"totalProfitAmount":0,"SKU":"Snj No 1vsop Brandy-750ML"}],"status":"success"}`);
        constructTable(responseData);
    } catch (error) {
        console.error("Error fetching report:", error);
    } finally {
        // Hide spinner
        document.getElementById("spinner").style.display = "none";
        document.getElementById("fetch-expenses").disabled = false;
    }

   

    $(document).on("click", ".clickable-row td:first-child", function () {
        const row = $(this).closest("tr");        // get the row
        const sku = row.find("td:first").text();  // get SKU

        if (row.closest("#est-billing-table").length) {
            // 🔴 Row is in billing → move back to main table
            row.removeClass("table-success");
            $("#est-report-table tbody").append(row.detach());
        } else {
            // 🟢 Row is in main table → move to billing table
            row.addClass("table-success");
            $("#est-billing-table tbody").append(row.detach());
        }

        updateBillingTotals();
    });
    if ($.fn.DataTable.isDataTable('#est-report-table')) {
        $('#est-report-table').DataTable().destroy(); 
    }

    $('#est-report-table').DataTable({
        scrollY: '600px', 
        scrollCollapse: true,
        paging: false,
        searching: false,
        ordering: false,
        info: true,
        lengthChange: true,
        autoWidth: false
    });
    
}

function constructTable() {
    const productMaster = localStorage.getItem('products');  
    const products = productMaster ? JSON.parse(productMaster) : null;
    const noOfDays = $("#purchase-no-days").val();
    let tbdy = ``;

    // 🔸 Step 1: Compute required fields & filter
    const computedData = responseData.data
        .filter(item => item.totalSalesQty > 0 && Math.round(item.averageDailyQty) > 0)
        .map(item => {
            const avgqty = Math.round(item.averageDailyQty);
            let estQty = avgqty * noOfDays;
            const stockInHand = getInHandStock(item.SKU, products);
            estQty = Math.max(0, estQty - stockInHand);

            return { ...item, avgqty, estQty, stockInHand };
        })
        // 🔸 Step 2: Only include rows where estQty > 0
        .filter(item => item.estQty > 0)
        // 🔸 Step 3: Sort by estQty ascending (lowest first)
        .sort((a, b) => a.estQty - b.estQty);

    // 🔸 Step 4: Build table rows
    computedData.forEach(item => {
        const purchasePrice = getPurchasePrice(item.SKU, products);
        const ML = item.SKU.split("-")[1];
        let estCase = 1;
        let ml = 0;

        if (ML === "180ML") {
            estCase = Math.round(item.estQty / 48);
            ml = 48;
        } else if (ML === "375ML" || ML === "500ML" || ML === "325ML") {
            estCase = Math.round(item.estQty / 24);
            ml = 24;
        } else if (ML === "750ML" || ML === "650ML") {
            estCase = Math.round(item.estQty / 12);
            ml = 12;
        } else if (ML === "1000ML") {
            estCase = Math.round(item.estQty / 9);
            ml = 9;
        }

        tbdy += `<tr class="clickable-row">
            <td>${item.SKU}</td>
            <td>${item.totalSalesQty}</td>
            <td>${item.avgqty}</td>
            <td>${item.stockInHand}</td>
            <td>${item.estQty}</td>
            <td>
                <input type="number" class="form-control form-control-sm est-case-input" 
                    value="${estCase}" data-sku="${item.SKU}" data-ml="${ml}" style="text-align: right;">
            </td>
            <td>${purchasePrice}</td>
            <td class="est-total-amount">${Math.round(purchasePrice * item.estQty)}</td>            
        </tr>`;
    });

    $('#est-table').html(tbdy);
    updateBillingTotals();
}



$(document).on('input', '#purchase-no-days', function() {
    if(responseData) {
        constructTable(responseData);
    }
});

$(document).on('input', '.est-case-input', function () {
    const input = $(this);
    const row = input.closest('tr');

    // Get values
    const estCase = parseFloat(input.val()) || 0;
    const ml = parseInt(input.data('ml')) || 0;
    const purchasePrice = parseFloat(row.find('td').eq(6).text()) || 0;

    // Calculate new values
    const estQty = estCase * ml;
    const totalAmount = Math.round(estQty * purchasePrice);

    // Update Est Qty cell (5th column index 4)
    row.find('td').eq(4).text(estQty);

    // Update Required Amount cell (8th column index 7)
    row.find('.est-total-amount').text(totalAmount);

    // Update billing totals if needed
    updateBillingTotals();
});

function updateQtyAndRequiredAmount(){

}

function updateBillingTotals() {
    let totalCases = 0;
    let totalValue = 0;

    $("#est-report-table tr").each(function () {
        const caseVal = parseFloat($(this).find("td:eq(5) input").val()) || 0; // 6th column
        const valueVal = parseFloat($(this).find("td:eq(7)").text()) || 0; // 8th column

        totalCases += caseVal;
        totalValue += valueVal;
    });

    // update new totals table
    $("#total-cases").text(totalCases);
    $("#total-value").text(totalValue);
}

function getInHandStock(SKU, products){
    for (var i in products.productList) {
        var item = products.productList[i];
        if(item.SKU == SKU){
            return item.stock;
        }
    }
    return 0;
}

function getPurchasePrice(SKU, products){
    for (var i in products.productList) {
        var item = products.productList[i];
        if(item.SKU == SKU){
            return item.purchasePrice;
        }
    }
    return 0;
}

