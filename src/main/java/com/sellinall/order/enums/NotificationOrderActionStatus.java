package com.sellinall.order.enums;

public enum NotificationOrderActionStatus {
	NO_ACTION("NO_ACTION"), /* No Action */
	INITIATED("INITIATED "), /* "decrement" inventory inStock quantity */
	ACCEPTED("ACCEPTED"), /* "decrement" inventory inStock quantity */
	PROCESSING("PROCESSING"), /* "decrement" inventory inStock quantity */
	DISPATCHED("DISPATCHED "), /* "decrement" inventory inStock quantity */
	COMPLETED("COMPLETED"), /* "decrement" inventory inStock quantity */
	DELIVERED("DELIVERED"), /* "decrement" inventory inStock quantity */
	DELIVERY_FAILED("DELIVERY_FAILED"), /* "decrement" inventory inStock quantity */
	CANCELLED("CANCELLED"), /* "increment" inventory inStock quantity */
	RETURNED("RETURNED"),
	COMBINED("COMBINED"), /* No Action, as the sync have happened already */

	INITIATED_TO_ACCEPTED("INITIATED_TO_ACCEPTED"), /* No Action */
	INITIATED_TO_COMBINED("INITIATED_TO_COMBINED"), /* No Action */
	INITIATED_TO_PROCESSING("INITIATED_TO_PROCESSING"), /* No Action */
	INITIATED_TO_COMPLETED("INITIATED_TO_COMPLETED"), /* No Action */
	INITIATED_TO_DISPATCHED("INITIATED_TO_DISPATCHED"), /* No Action */
	ACCEPTED_TO_PROCESSING("ACCEPTED_TO_PROCESSING"), /* No Action */
	ACCEPTED_TO_DISPATCHED("ACCEPTED_TO_DISPATCHED"), /* No Action */
	ACCEPTED_TO_COMBINED("ACCEPTED_TO_COMBINED"), /* No Action */
	PROCESSING_TO_INITIATED("PROCESSING_TO_INITIATED"), /* No Action */
	PROCESSING_TO_DISPATCHED("PROCESSING_TO_DISPATCHED"), /* No Action */
	PROCESSING_TO_DELIVERED("PROCESSING_TO_DELIVERED"), /* No Action */
	PROCESSING_TO_COMPLETED("PROCESSING_TO_COMPLETED"), /* No Action */
	PROCESSING_TO_COMBINED("PROCESSING_TO_COMBINED"), /* No Action */
	DISPATCHED_TO_DELIVERED("DISPATCHED_TO_DELIVERED"), /* No Action */
	DISPATCHED_TO_DELIVERY_FAILED("DISPATCHED_TO_DELIVERY_FAILED"), /* No Action */
	DELIVERED_TO_DISPATCHED("DISPATCHED_TO_DELIVERED"), /* No Action */
	DISPATCHED_TO_COMPLETED("DISPATCHED_TO_COMPLETED"), /* No Action */

	INITIATED_TO_CANCELLED("INITIATED_TO_CANCELLED"), /* "increment" inventory inStock quantity */
	ACCEPTED_TO_CANCELLED("ACCEPTED_TO_CANCELLED"), /* "increment" inventory inStock quantity */
	DISPATCHED_TO_RETURNED("DISPATCHED_TO_RETURNED"), /* "increment" inventory inStock quantity */
	DELIVERED_TO_RETURNED("DELIVERED_TO_RETURNED"), /* "increment" inventory inStock quantity */
	PROCESSING_TO_CANCELLED("PROCESSING_TO_CANCELLED"); /* "increment" inventory inStock quantity */

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