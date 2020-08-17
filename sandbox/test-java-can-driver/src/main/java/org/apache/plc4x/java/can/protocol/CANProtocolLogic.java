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
package org.apache.plc4x.java.can.protocol;

import org.apache.plc4x.java.can.readwrite.SocketCANFrame;
import org.apache.plc4x.java.spi.ConversationContext;
import org.apache.plc4x.java.spi.Plc4xProtocolBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CANProtocolLogic extends Plc4xProtocolBase<SocketCANFrame> {

    private Logger logger = LoggerFactory.getLogger(CANProtocolLogic.class);

    @Override
    public void onConnect(ConversationContext<SocketCANFrame> context) {
        context.fireConnected();
    }

    @Override
    protected void decode(ConversationContext<SocketCANFrame> context, SocketCANFrame msg) throws Exception {
        logger.info("Decode CAN message {}", msg);
    }

    @Override
    public void close(ConversationContext<SocketCANFrame> context) {

    }

    @Override
    public void onDisconnect(ConversationContext<SocketCANFrame> context) {

    }

}
