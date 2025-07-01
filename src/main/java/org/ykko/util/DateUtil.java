package org.ykko.util;

import java.time.Instant;

public class DateUtil {

    /**
     * e.g. isoTimestamp = "2025-07-01T01:08:27.225Z";
     * @param isoTimestamp
     * @return
     */
    public static Long convertISOTimestampToUnixTimestamp(String isoTimestamp) {
        Instant instant = Instant.parse(isoTimestamp);
        return instant.toEpochMilli(); // milliseconds since epoch
    }
}
