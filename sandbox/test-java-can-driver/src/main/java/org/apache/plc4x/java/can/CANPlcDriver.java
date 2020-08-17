/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */
package org.apache.plc4x.java.can;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.can.configuration.CANConfiguration;
import org.apache.plc4x.java.can.context.CANDriverContext;
import org.apache.plc4x.java.can.field.CANFieldHandler;
import org.apache.plc4x.java.can.protocol.CANProtocolLogic;
import org.apache.plc4x.java.can.readwrite.CANFrame;
import org.apache.plc4x.java.can.readwrite.SocketCANFrame;
import org.apache.plc4x.java.can.readwrite.io.CANFrameIO;
import org.apache.plc4x.java.can.readwrite.io.SocketCANFrameIO;
import org.apache.plc4x.java.spi.configuration.Configuration;
import org.apache.plc4x.java.spi.connection.GeneratedDriverBase;
import org.apache.plc4x.java.spi.connection.ProtocolStackConfigurer;
import org.apache.plc4x.java.spi.connection.SingleProtocolStackConfigurer;
import tel.schich.javacan.CanFrame;
import tel.schich.javacan.CanId;

import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 */
public class CANPlcDriver extends GeneratedDriverBase<SocketCANFrame> {

    @Override
    public String getProtocolCode() {
        return "can";
    }

    @Override
    public String getProtocolName() {
        return "Controller Area Network";
    }

    @Override
    protected Class<? extends Configuration> getConfigurationType() {
        return CANConfiguration.class;
    }

    @Override
    protected String getDefaultTransport() {
        return "javacan";
    }

    @Override
    protected CANFieldHandler getFieldHandler() {
        return new CANFieldHandler();
    }

    @Override
    protected ProtocolStackConfigurer<SocketCANFrame> getStackConfigurer() {
        return SingleProtocolStackConfigurer.builder(SocketCANFrame.class, SocketCANFrameIO.class)
            .withProtocol(CANProtocolLogic.class)
            .withDriverContext(CANDriverContext.class)
            .withPacketSizeEstimator(CANEstimator.class)
            //.withCorruptPacketRemover(CANCleaner.class)
            .build();
    }

    public static class CANEstimator implements ToIntFunction<ByteBuf> {
        @Override
        public int applyAsInt(ByteBuf byteBuf) {
            if (byteBuf.readableBytes() >= 5) {
                System.out.println(ByteBufUtil.prettyHexDump(byteBuf));
                byte len = byteBuf.getByte(4);
                System.out.println("Length " + (int) len);
                return len + 8 /* overhead */;
            }
            return -1; //discard
        }
    }

    public class CANCleaner implements Consumer<ByteBuf> {
        @Override
        public void accept(ByteBuf byteBuf) {
            System.out.println("Discard");
            byteBuf.readByte();
        }
    }
}
