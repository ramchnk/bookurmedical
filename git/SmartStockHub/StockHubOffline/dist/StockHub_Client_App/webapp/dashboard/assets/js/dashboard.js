var dateArray = []
dateRangeArray()
getSalesProfit()
getSalesData();
getWeeklyData();
getTopSalesSKU();

async function getSalesProfit() {
updateShopName();
var salesData = []
var profitData = []
  let url = `${webmanagerurl}/dashboard/performance`;
  try {
    const response = await getRequest(url);
    const monthlySales = response.monthlySales;
    for (const date of dateArray) {
      let found = false;
      for (const i of monthlySales) {
        const options = { month: "short", day: "numeric" };
        let salesDate = new Intl.DateTimeFormat("en-US", options).format(
          new Date(i._id * 1000)
        );
        if (date === salesDate) {
          salesData.push(salesDate);
          if (i.profitAmount < 0) {
            profitData.push(0);
          } else {
            profitData.push(i.profitAmount);
          }

          found = true;
          break;
        }
      }
      if (!found) {
        salesData.push(date);
        profitData.push(0);
      }
    }

    options={series:[{name:"",type:"",data:[]},{name:"Profit",type:"area",data:profitData}],chart:{height:313,type:"line",toolbar:{show:!1}},stroke:{dashArray:[0,0],width:[0,2],curve:"smooth"},fill:{opacity:[1,1],type:["solid","gradient"],gradient:{type:"vertical",inverseColors:!1,opacityFrom:.5,opacityTo:0,stops:[0,90]}},markers:{size:[0,0],strokeWidth:2,hover:{size:4}},xaxis:{categories:dateArray,axisTicks:{show:!1},axisBorder:{show:!1}},yaxis:{min:0,axisBorder:{show:!1}},grid:{show:!0,strokeDashArray:3,xaxis:{lines:{show:!1}},yaxis:{lines:{show:!0}},padding:{top:0,right:-2,bottom:0,left:10}},legend:{show:!0,horizontalAlign:"center",offsetX:0,offsetY:5,markers:{width:9,height:9,radius:6},itemMargin:{horizontal:10,vertical:0}},plotOptions:{bar:{columnWidth:"30%",barHeight:"70%",borderRadius:3}},colors:["","#22c55e"],tooltip:{shared:!0,y:[{formatter:function(e){return void 0!==e?e.toFixed(1)+"":e}},{formatter:function(e){return void 0!==e?e.toFixed(1)+"":e}}]}},
    (chart=new ApexCharts(document.querySelector("#dash-performance-chart"),options)).render();class VectorMap{initWorldMapMarker(){new jsVectorMap({map:"world",selector:"#world-map-markers",zoomOnScroll:!0,zoomButtons:!1,markersSelectable:!0,markers:[{name:"Canada",coords:[56.1304,-106.3468]},{name:"Brazil",coords:[-14.235,-51.9253]},{name:"Russia",coords:[61,105]},{name:"China",coords:[35.8617,104.1954]},{name:"United States",coords:[37.0902,-95.7129]}],markerStyle:{initial:{fill:"#7f56da"},selected:{fill:"#22c55e"}},labels:{markers:{render:e=>e.name}},regionStyle:{initial:{fill:"rgba(169,183,197, 0.3)",fillOpacity:1}}})}init(){this.initWorldMapMarker()}}document.addEventListener("DOMContentLoaded",function(e){(new VectorMap).init()});

  } catch (error) {
    console.error(error);
  }
}

function dateRangeArray() {
  for (let i = 0; i < 30; i++) {
    let date = new Date();
    date.setDate(date.getDate() - i);
    const options = { month: "short", day: "numeric" };
    let formattedDate = Intl.DateTimeFormat("en-US", options).format(date);
    dateArray.push(formattedDate);
  }
}

async function getSalesData() {
  let profit = 0;
  let saleAmount = 0;
  let saleQuantity = 0;
  let expense = 0;
  let url = `${webmanagerurl}/dashboard/salesdata`;
  const response = await getRequest(url);
  const yesterdaySales = response.salesData;
  for (let i of yesterdaySales) {
    profit = i.profitAmount;
    saleAmount = i.salesAmount;
    saleQuantity = i.productSales;
    expense = i.expensesAmount;
  }
  document.getElementById("salesAmount").textContent =
    saleAmount.toFixed();
  document.getElementById("quantity-sold").textContent = saleQuantity;
  document.getElementById("sales-profit").textContent = profit.toFixed();
  document.getElementById("sales-expenses").textContent =
    expense.toFixed();
}

async function getWeeklyData() {
var thisWeekSales = 0;
var lastWeekSales = 0;
var salesPercentage = 0;
var thisWeekInbound = 0;
var lastWeekInbound = 0;
  let url = `${webmanagerurl}/dashboard/weeklydata`;
  try {
    const response = await getRequest(url);
    const weeklydata = response.weeklydata;
    thisWeekSales = weeklydata?.[0]?.thisweek?.[0]?.salesAmount;
    lastWeekSales = weeklydata?.[0]?.lastweek?.[0]?.salesAmount;
    thisWeekInbound = weeklydata?.[0]?.thisweek?.[0]?.purchaseStock;
    lastWeekInbound = weeklydata?.[0]?.lastweek?.[0]?.purchaseStock;
  } catch (error) {
    console.error;
  }

  document.getElementById("this-week-sales").textContent = thisWeekSales > 0 ? thisWeekSales : 0;
  document.getElementById("last-week-sales").textContent = lastWeekSales > 0 ? lastWeekSales : 0;
  salesPercentage =  calculateWeeklySalesPercentage(thisWeekSales, lastWeekSales);

  var options={chart:{height:292,type:"radialBar"},plotOptions:{radialBar:{startAngle:-135,endAngle:135,dataLabels:{name:{fontSize:"14px",color:"undefined",offsetY:100},value:{offsetY:55,fontSize:"20px",color:void 0,formatter:function(e){return e+"%"}}},track:{background:"rgba(170,184,197, 0.2)",margin:0}}},fill:{gradient:{enabled:!0,shade:"dark",shadeIntensity:.2,inverseColors:!1,opacityFrom:1,opacityTo:1,stops:[0,50,65,91]}},stroke:{dashArray:4},colors:["#ff6c2f","#22c55e"],series:[parseFloat(salesPercentage)],labels:["Weekly Sales"],responsive:[{breakpoint:380,options:{chart:{height:180}}}],grid:{padding:{top:0,right:0,bottom:0,left:0}}},chart=new ApexCharts(document.querySelector("#conversions"),options);chart.render();

  calculateWeeklyInboundPercentage(thisWeekInbound, lastWeekInbound)
}

function calculateWeeklySalesPercentage(thisWeekSales, lastWeekSales) {
  percentage = ((thisWeekSales / lastWeekSales) * 100).toFixed();
  return (salesPercentage = percentage > 0 ? percentage : 0);
}


async function getTopSalesSKU() {
  let url = `${webmanagerurl}/dashboard/topsales`;
  try {
    const response = await getRequest(url);
    const topsalessku = response.topsalessku;
    const container = document.getElementById("top-selling-products");
    if (!container) {
      return;
    }
    container.innerHTML = "";
    new gridjs.Grid({
      fixedHeader: true,
      columns: [
        { name: "SKU" },
        { name: "Quantity" },
        { name: "Profit Amount" },
      ],
      data:topsalessku.map((item) => [
        item._id,
        item.stock,
        item.profitAmount.toFixed(2),
      ]),
    }).render(container);
  } catch (error) {
    console.error;
  }
}


function calculateWeeklyInboundPercentage(thisWeekInbound, lastWeekInbound) {
  let percentage = ((thisWeekInbound / lastWeekInbound) * 100).toFixed();
  document.getElementById("this-week-inbound").textContent = thisWeekInbound > 0 ? thisWeekInbound : 0;
  document.getElementById("last-week-inbound").textContent = lastWeekInbound > 0 ? lastWeekInbound : 0;
  if (percentage == 'Infinity' || percentage == '-Infinity' || isNaN(percentage || percentage < 0)) {
    percentage = 0;
  }
  var options={chart:{height:292,type:"radialBar"},plotOptions:{radialBar:{startAngle:-135,endAngle:135,dataLabels:{name:{fontSize:"14px",color:"undefined",offsetY:100},value:{offsetY:55,fontSize:"20px",color:void 0,formatter:function(e){return e+"%"}}},track:{background:"rgba(170,184,197, 0.2)",margin:0}}},fill:{gradient:{enabled:!0,shade:"dark",shadeIntensity:.2,inverseColors:!1,opacityFrom:1,opacityTo:1,stops:[0,50,65,91]}},stroke:{dashArray:4},colors:["#2980b9","#22c55e"],series:[parseFloat(percentage)],labels:["Weekly Stock Inbound"],responsive:[{breakpoint:380,options:{chart:{height:180}}}],grid:{padding:{top:0,right:0,bottom:0,left:0}}},chart=new ApexCharts(document.querySelector("#weekly-inbound"),options);chart.render();
}