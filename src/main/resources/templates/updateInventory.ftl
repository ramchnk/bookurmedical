<#assign baseSKUWithSoldQuantityDetailsObject = exchange.properties.baseSKUWithSoldQuantityDetailsObject>
{
  <#if exchange.properties.warehouseName?? && exchange.properties.warehouseName?has_content>
  "warehouseName" : ${exchange.properties.warehouseName?js_string},
  </#if>
  "data": [
   
      <#list baseSKUWithSoldQuantityDetailsObject?keys as key>
      {
        "customSKU":"${key}",
        <#if exchange.properties.isOutOfStock == true>
          "quantity": ${baseSKUWithSoldQuantityDetailsObject[key]}
        <#else>
         "quantityDiff": ${baseSKUWithSoldQuantityDetailsObject[key]}
        </#if>
      }<#if key_has_next>,</#if>
</#list>  
  ]
}