package com.restify.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HelloToolTest {

    @Autowired
    private HelloTool helloTool;

    @Test
    void testHelloTool() {
        String result = helloTool.hello();
        assertEquals("hello world", result, "Hello tool should return 'hello world'");
    }
}

