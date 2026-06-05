package com.moneymind.domain.core;

import com.moneymind.finance.domain.core.Cursor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursorTest {

    @Test
    void singleIdRoundTrip() {
        String encoded = Cursor.forId(42);
        assertEquals(42, Cursor.decodeId(encoded));
    }

    @Test
    void periodRoundTrip() {
        String encoded = Cursor.forPeriod("2024-01");
        assertEquals("2024-01", Cursor.decodeString(encoded));
    }

    @Test
    void periodAndColumnRoundTrip() {
        String encoded = Cursor.forPeriodAndColumn("2024-01", "Housing");
        assertEquals("2024-01,Housing", Cursor.decodeString(encoded));
    }

    @Test
    void nullCursorDecodesAsZeroId() {
        assertEquals(0, Cursor.decodeId(null));
    }

    @Test
    void nullCursorDecodesAsNullString() {
        assertNull(Cursor.decodeString(null));
    }

    @Test
    void malformedBase64ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Cursor.decodeId("not!!valid!!base64"));
    }

    @Test
    void validBase64NonIntegerThrowsOnDecodeId() {
        // Valid Base64 but decoded content is not an integer
        String periodCursor = Cursor.forPeriod("2024-01");
        assertThrows(IllegalArgumentException.class, () -> Cursor.decodeId(periodCursor));
    }

    @Test
    void emptyCursorThrowsOnDecodeId() {
        // Empty string decodes to empty bytes — Integer.parseInt("") throws
        assertThrows(IllegalArgumentException.class, () -> Cursor.decodeId(""));
    }
}
