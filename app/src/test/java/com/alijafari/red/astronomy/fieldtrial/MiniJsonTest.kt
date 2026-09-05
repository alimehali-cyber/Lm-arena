package com.alijafari.red.astronomy.fieldtrial

import com.alijafari.red.astronomy.fieldtrial.engine.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-1.3: JSON parser/writer round-trips + mutation guards. */
class MiniJsonTest {

    @Test
    fun `round trips nested structures with escapes and numbers`() {
        val src = Json.JObj(LinkedHashMap<String, Json>().apply {
            put("name", Json.JStr("trial \"1\"\nline2 / \\ ok"))
            put("epochMs", Json.JNum(1757000000123.0))
            put("negative", Json.JNum(-0.5))
            put("flag", Json.JBool(true))
            put("nul", Json.JNull)
            put("arr", Json.JArr(listOf(Json.JNum(1.0), Json.JStr("two"), Json.JArr(listOf(Json.JBool(false))))))
        })
        val text = Json.write(src)
        val back = Json.parse(text)
        assertEquals(src, back)
        assertEquals(text, Json.write(back)) // canonical: write(parse(write(x))) == write(x)
        assertTrue("1757000000123" in text) // longs stay integral
    }

    @Test
    fun `parses whitespace variants and rejects malformed input`() {
        assertEquals(Json.JNum(1.5), Json.parse("  1.5  "))
        assertEquals(Json.JStr("a"), Json.parse("\t\"a\"\n"))
        assertThrows(Json.JsonError::class.java) { Json.parse("{\"a\":}") }
        assertThrows(Json.JsonError::class.java) { Json.parse("[1,2") }
        assertThrows(Json.JsonError::class.java) { Json.parse("tru") }
        assertThrows(Json.JsonError::class.java) { Json.parse("{}extra") }
        assertThrows(Json.JsonError::class.java) { Json.parse("\"unterminated") }
    }

    @Test
    fun `mutation guard - unicode escapes and control chars survive a tamper check`() {
        val s = Json.JStr("ctlﬁ\u0007")
        val rt = Json.parse(Json.write(s))
        assertEquals(s, rt)
        // a truncated \u escape must fail, not silently mis-decode
        assertThrows(Json.JsonError::class.java) { Json.parse("\"\\u12\"") }
    }
}
