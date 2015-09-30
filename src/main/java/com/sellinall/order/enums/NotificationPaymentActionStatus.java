package com.sellinall.order.enums;

public enum NotificationPaymentActionStatus {
	NO_ACTION ("NO_ACTION"), 			// noAction
	NOT_INITIATED ("NOT_INITIATED"), 	// Payment Not initiated
	AUTHORIZED ("AUTHORIZED"), 			// ( + )pending amount calculation only
	COMPLETED ("COMPLETED"), 			// ( + )completed amount calculation only
	REFUNDED ("REFUNDED"), 				// ( + )refund amount calculation only 
	NOT_INITIATED_TO_AUTHORIZED ("NOT_INITIATED_TO_AUTHORIZED"), // ( + )pending amount calculation only
	NOT_INITIATED_TO_COMPLETED ("NOT_INITIATED_TO_COMPLETED"), 	// ( + )completed amount calculation only
	NOT_INITIATED_TO_REFUNDED ("NOT_INITIATED_TO_REFUNDED"), // ( + )refund amount calculation only
	AUTHORIZED_TO_NOT_INITIATED ("AUTHORIZED_TO_NOT_INITIATED"), // (-) pending
	AUTHORIZED_TO_COMPLETED ("AUTHORIZED_TO_COMPLETED"),	// (-) pending  (+) completed
	AUTHORIZED_TO_REFUNDED ("AUTHORIZED_TO_REFUNDED"), 		// (-) pending (+) refunded
	COMPLETED_TO_REFUNDED ("COMPLETED_TO_REFUNDED"); 		// (-) completed (+) refunded
	

	/* NOT_INITIATED_TO_AUTHORIZED  // 
	 * */
	
	private final String name;       

	private NotificationPaymentActionStatus(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return (otherName == null) ? false : name.equals(otherName);
	}

	public String toString() {
		return this.name;
	}
}
