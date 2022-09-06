package com.sellinall.order.bl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sellinall.order.enums.RuleActionEnum;


public class RuleActionsMapping {
	private static final Map<Object, String> ruleInventoryMethodsMapping = Collections
			.unmodifiableMap(new HashMap<Object, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(RuleActionEnum.ADD_ITEM, "setGiftItems");
					put(RuleActionEnum.REMOVE_ALL_ITEM, "removeGiftItems");
				}
			});

	public static String getRuleEngineMethod(RuleActionEnum actionName) {
		return (String) ruleInventoryMethodsMapping.get(actionName);
	}
}
