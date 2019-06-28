<#assign baseSKUWithSoldQuantityDetailsObject = exchange.properties.baseSKUWithSoldQuantityDetailsObject>
{
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