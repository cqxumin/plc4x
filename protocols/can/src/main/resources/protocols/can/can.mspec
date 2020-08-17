/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

[type 'CANFrame'
    [simple uint 11 'identifier']
    [simple bit 'remoteTransmissionRequest']
    [discriminator bit 'extended']
    [typeSwitch 'extended'
        ['false' CANDataFrame
            [reserved uint 1 '0x0']
        ]
        ['true' ExtendedCANFrame
            [simple uint 18 'extensionId']
            [simple bit 'extensionRemoteTransmissionRequest']
            [reserved uint 2 '0x0']
        ]
    ]
    [simple uint 4 'length']
    [array uint 8 'data' count 'length']
    [simple uint 15 'crc']
]

/* These are structures defined in linux kernel, provided here just for information purposes
struct can_frame {
  canid_t can_id;  // 32 bit CAN_ID + EFF/RTR/ERR flags
  __u8    can_dlc; // frame payload length in byte (0 .. 8)
  __u8    __pad;   // padding
  __u8    __res0;  // reserved / padding
  __u8    __res1;  // reserved / padding
  __u8    data[8] __attribute__((aligned(8)));
};
struct canfd_frame {
  canid_t can_id;  // 32 bit CAN_ID + EFF/RTR/ERR flags
  __u8    len;     // frame payload length in byte (0 .. 64)
  __u8    flags;   // additional flags for CAN FD
  __u8    __res0;  // reserved / padding
  __u8    __res1;  // reserved / padding
  __u8    data[64] __attribute__((aligned(8)));
};
*/

[type 'SocketCANFrame'
    [simple uint 29 'identifier']
    [simple bit 'extended']
    [simple bit 'remote']
    [simple bit 'error']
    [implicit uint 8 'length' 'ARRAY_SIZE_IN_BYTES(data)']
    [typeSwitch 'extended', 'identifier'
        ['true' SocketCANFDFrame
            [simple uint 8 'flags']
            [reserved uint 8 '0x0']
            [reserved uint 8 '0x0']
        ]
        ['false' ScoketCANFrame
            [reserved uint 8 '0x0']
            [reserved uint 8 '0x0']
            [reserved uint 8 '0x0']
        ]
    ]
    [array int 8 'data' COUNT 'length']
]
