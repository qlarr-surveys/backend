package com.frankie.expressionmanager.ext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


@Suppress("LocalVariableName")
class ValidVariableNameTest {

    @Test
    fun `isValidVariableName returns false if invalid char is used1`() {
        assertFalse(isValidVariableName("amr.askoura"))
    }

    @Test
    fun `isValidVariableName returns false if invalid char is used2`() {
        assertFalse(isValidVariableName("amr.askoura"))
    }

    @Test
    fun `isValidVariableName returns false if name starts with number`() {
        assertFalse(isValidVariableName("1amr"))
    }

    @Test
    fun `otherwise isValidVariableName returns true`() {
        assertTrue(isValidVariableName("amr1"))
    }
}
