/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.serialization;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

class SerializationTest {

    private MySerialization mySerialization;

    private MyObjectOutput myObjectOutput;
    private MyObjectInput myObjectInput;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ByteArrayInputStream byteArrayInputStream;

    @BeforeEach
    public void setUp() throws Exception {
        this.mySerialization = new MySerialization();

        this.byteArrayOutputStream = new ByteArrayOutputStream();
        this.myObjectOutput = new MyObjectOutput(byteArrayOutputStream);
    }

    @Test
    void testContentType() {
        assertThat(mySerialization.getContentType(), is("x-application/my"));
    }

    @Test
    void testContentTypeId() {
        assertThat(mySerialization.getContentTypeId(), is((byte) 101));
    }

    @Test
    void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = mySerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.<ObjectOutput>instanceOf(MyObjectOutput.class));
    }

    @Test
    void testObjectInput() throws IOException {
        ObjectInput objectInput = mySerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.<ObjectInput>instanceOf(MyObjectInput.class));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // Charset maynot UTF-8 on Windows JDK 8 ~ 17
    void testWriteUTF() throws IOException {
        myObjectOutput.writeUTF("Pace");
        myObjectOutput.writeUTF("和平");
        myObjectOutput.writeUTF(" Мир");
        flushToInput();

        assertThat(myObjectInput.readUTF(), CoreMatchers.is("Pace"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is("和平"));
        assertThat(myObjectInput.readUTF(), CoreMatchers.is(" Мир"));
    }

    private void flushToInput() throws IOException {
        this.myObjectOutput.flushBuffer();
        this.byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        this.myObjectInput = new MyObjectInput(byteArrayInputStream);
    }
}
