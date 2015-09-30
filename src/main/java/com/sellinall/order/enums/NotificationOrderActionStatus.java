package com.sellinall.order.enums;

public enum NotificationOrderActionStatus {
	NO_ACTION ("NO_ACTION"), /* No Action */ 
	PROCESSING ("PROCESSING"), /* "decrement" inventory inStock quantity */
	COMPLETED ("COMPLETED"), /*  "decrement" inventory inStock quantity  */
	CANCELLED ("CANCELLED"), /* "increment" inventory inStock quantity */
	PROCESSING_TO_COMPLETED ("PROCESSING_TO_COMPLETED"), /* No Action */
	PROCESSING_TO_CANCELLED ("PROCESSING_TO_CANCELLED"); /* "increment" inventory inStock quantity  */

	private final String name;       

	private NotificationOrderActionStatus(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}

}