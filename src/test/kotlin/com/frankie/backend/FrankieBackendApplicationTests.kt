package com.frankie.backend

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@Disabled("The connection to a database is necessary. Provide H2 or a test.txt-container to fix!")
@SpringBootTest
class FrankieBackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}
