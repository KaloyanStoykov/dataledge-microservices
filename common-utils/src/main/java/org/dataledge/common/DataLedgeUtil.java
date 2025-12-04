package org.dataledge.common;

public final class DataLedgeUtil {
    // Prevent instantiation
    private DataLedgeUtil() {}

    /**
     * The HTTP header name for correlation ID.
     * Used for each microservice to request the userId from HttpHeader
     * using @RequestHeader(DataLedgeUtil.CORRELATION_ID)
     */
    public static final String USER_ID_HEADER = "X-DataLedge-User-ID";

}