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
package org.apache.plc4x.java.s7.model;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.plc4x.java.api.exceptions.PlcInvalidFieldException;
import org.apache.plc4x.java.api.model.PlcField;
import org.apache.plc4x.java.s7.netty.model.types.MemoryArea;
import org.apache.plc4x.java.s7.netty.model.types.TransportSize;
import org.apache.plc4x.java.utils.ParseException;
import org.apache.plc4x.java.utils.ReadBuffer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S7Field implements PlcField {

    //byteOffset theoretically can reach up to 2097151 ... see checkByteOffset() below --> 7digits
    private static final Pattern ADDRESS_PATTERN =
        Pattern.compile("^%(?<memoryArea>.)(?<transferSizeCode>[XBWD]?)(?<byteOffset>\\d{1,7})(.(?<bitOffset>[0-7]))?:(?<dataType>[a-zA-Z_]+)(\\[(?<numElements>\\d+)])?");

    //blockNumber usually has its max hat around 64000 --> 5digits
    private static final Pattern DATA_BLOCK_ADDRESS_PATTERN =
        Pattern.compile("^%DB(?<blockNumber>\\d{1,5}).DB(?<transferSizeCode>[XBWD]?)(?<byteOffset>\\d{1,7})(.(?<bitOffset>[0-7]))?:(?<dataType>[a-zA-Z_]+)(\\[(?<numElements>\\d+)])?");

    private static final Pattern DATA_BLOCK_SHORT_PATTERN =
        Pattern.compile("^%DB(?<blockNumber>\\d{1,5}):(?<byteOffset>\\d{1,7})(.(?<bitOffset>[0-7]))?:(?<dataType>[a-zA-Z_]+)(\\[(?<numElements>\\d+)])?");

    private static final Pattern SIMOTION_ADDRESS_PATTERN =
        Pattern.compile("[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}-[0-9A-F]{2}");

    private static final String DATA_TYPE = "dataType";
    private static final String TRANSFER_SIZE_CODE = "transferSizeCode";
    private static final String BLOCK_NUMBER = "blockNumber";
    private static final String BYTE_OFFSET = "byteOffset";
    private static final String BIT_OFFSET = "bitOffset";
    private static final String NUM_ELEMENTS = "numElements";
    private static final String MEMORY_AREA = "memoryArea";

    private final TransportSize dataType;
    private final MemoryArea memoryArea;
    private final int blockNumber;
    private final int byteOffset;
    private final short bitOffset;
    private final int numElements;

    private S7Field(TransportSize dataType, MemoryArea memoryArea, int blockNumber, int byteOffset, short bitOffset, int numElements) {
        this.dataType = dataType;
        this.memoryArea = memoryArea;
        this.blockNumber = blockNumber;
        this.byteOffset = byteOffset;
        this.bitOffset = bitOffset;
        this.numElements = numElements;
    }

    public TransportSize getDataType() {
        return dataType;
    }

    public MemoryArea getMemoryArea() {
        return memoryArea;
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public int getByteOffset() {
        return byteOffset;
    }

    public short getBitOffset() {
        return bitOffset;
    }

    public int getNumElements() {
        return numElements;
    }

    public static boolean matches(String fieldString) {
        return DATA_BLOCK_ADDRESS_PATTERN.matcher(fieldString).matches() ||
            ADDRESS_PATTERN.matcher(fieldString).matches() ||
            DATA_BLOCK_SHORT_PATTERN.matcher(fieldString).matches() ||
            SIMOTION_ADDRESS_PATTERN.matcher(fieldString).matches();
    }

    /**
     * @return Java type of expected response.
     *
     * TODO validate all Methods existing are implemented
     */
    @Override
    public Class<?> getDefaultJavaType() {
        switch (dataType){
            case STRING:
                return String.class;
            case USINT:
            case SINT:
            case UINT:
            case INT:
            case DINT:
                return Integer.class;
            case UDINT:
            case ULINT:
            case LINT:
                return Long.class;
            case BOOL:
                return Boolean.class;
            case REAL:
            case LREAL:
                return Double.class;
            case DATE_AND_TIME:
                return LocalDateTime.class;
            case DATE:
                return LocalDate.class;
            case TIME_OF_DAY:
                return LocalTime.class;
            default:
                throw new NotImplementedException("The response type for datatype " + dataType + " is not yet implemented");
        }
    }

    public static S7Field of(String fieldString) {
        Matcher matcher = DATA_BLOCK_ADDRESS_PATTERN.matcher(fieldString);
        if(matcher.matches()) {
            TransportSize dataType = TransportSize.valueOf(matcher.group(DATA_TYPE));
            MemoryArea memoryArea = MemoryArea.DATA_BLOCKS;
            String transferSizeCode = matcher.group(TRANSFER_SIZE_CODE);

            int blockNumber = checkDatablockNumber(Integer.parseInt(matcher.group(BLOCK_NUMBER)));

            int byteOffset = checkByteOffset(Integer.parseInt(matcher.group(BYTE_OFFSET)));

            short bitOffset = 0;
            if(matcher.group(BIT_OFFSET) != null) {
                bitOffset = Short.parseShort(matcher.group(BIT_OFFSET));
            } else if(dataType == TransportSize.BOOL) {
                throw new PlcInvalidFieldException("Expected bit offset for BOOL parameters.");
            }
            int numElements = 1;
            if(matcher.group(NUM_ELEMENTS) != null) {
                numElements = Integer.parseInt(matcher.group(NUM_ELEMENTS));
            }
            numElements = calcNumberOfElementsForIndividualTypes(numElements,dataType);
            if(!transferSizeCode.isEmpty() && !dataType.getSizeCode().equals(transferSizeCode)) {
                throw new PlcInvalidFieldException("Transfer size code '" + transferSizeCode +
                    "' doesn't match specified data type '" + dataType.name() + "'");
            }
            return new S7Field(dataType, memoryArea, blockNumber, byteOffset, bitOffset, numElements);
        } else if (ADDRESS_PATTERN.matcher(fieldString).matches()) {
            matcher = ADDRESS_PATTERN.matcher(fieldString);
            if (matcher.matches()) {
                TransportSize dataType = TransportSize.valueOf(matcher.group(DATA_TYPE));
                MemoryArea memoryArea = MemoryArea.valueOfShortName(matcher.group(MEMORY_AREA));
                String transferSizeCode = matcher.group(TRANSFER_SIZE_CODE);

                int byteOffset = checkByteOffset(Integer.parseInt(matcher.group(BYTE_OFFSET)));

                short bitOffset = 0;
                if(matcher.group(BIT_OFFSET) != null) {
                    bitOffset = Short.parseShort(matcher.group(BIT_OFFSET));
                } else if(dataType == TransportSize.BOOL) {
                    throw new PlcInvalidFieldException("Expected bit offset for BOOL parameters.");
                }
                int numElements = 1;
                if(matcher.group(NUM_ELEMENTS) != null) {
                    numElements = Integer.parseInt(matcher.group(NUM_ELEMENTS));
                }
                numElements = calcNumberOfElementsForIndividualTypes(numElements,dataType);
                if(!transferSizeCode.isEmpty() && !dataType.getSizeCode().equals(transferSizeCode)) {
                    throw new PlcInvalidFieldException("Transfer size code '" + transferSizeCode +
                        "' doesn't match specified data type '" + dataType.name() + "'");
                }
                return new S7Field(dataType, memoryArea, (short) 0, byteOffset, bitOffset, numElements);
            }
        } else if (DATA_BLOCK_SHORT_PATTERN.matcher(fieldString).matches()) {
            matcher = DATA_BLOCK_SHORT_PATTERN.matcher(fieldString);

            boolean matches = matcher.matches();
            assert matches;

            TransportSize dataType = TransportSize.valueOf(matcher.group(DATA_TYPE));
            MemoryArea memoryArea = MemoryArea.DATA_BLOCKS;

            int blockNumber = checkDatablockNumber(Integer.parseInt(matcher.group(BLOCK_NUMBER)));

            int byteOffset = checkByteOffset(Integer.parseInt(matcher.group(BYTE_OFFSET)));

            short bitOffset = 0;
            if(matcher.group(BIT_OFFSET) != null) {
                bitOffset = Short.parseShort(matcher.group(BIT_OFFSET));
            } else if (dataType == TransportSize.BOOL) {
                throw new PlcInvalidFieldException("Expected bit offset for BOOL parameters.");
            }
            int numElements = 1;
            if (matcher.group(NUM_ELEMENTS) != null) {
                numElements = Integer.parseInt(matcher.group(NUM_ELEMENTS));
            }
            numElements = calcNumberOfElementsForIndividualTypes(numElements, dataType);
            return new S7Field(dataType, memoryArea, blockNumber, byteOffset, bitOffset, numElements);
        } else if (SIMOTION_ADDRESS_PATTERN.matcher(fieldString).matches()) {
            matcher = SIMOTION_ADDRESS_PATTERN.matcher(fieldString);

            boolean matches = matcher.matches();
            assert matches;

            try {
                byte[] addressData = Hex.decodeHex(fieldString.replaceAll("[-]", ""));
                ReadBuffer rb = new ReadBuffer(addressData);
                // Read out values according to definition in mspec
                final short resvd = rb.readUnsignedShort(8);
                if (0x10 != resvd) {
                    throw new PlcInvalidFieldException("Unsupported Field Type to parse!");
                }
                final TransportSize transportSize = TransportSize.valueOf((byte)rb.readUnsignedShort(8));
                final short numberOfElements = (short)rb.readUnsignedInt(16);
                final short dbNumber = (short)rb.readUnsignedInt(16);
                final MemoryArea memoryArea = MemoryArea.valueOf(rb.readByte(8));
                assert 0x00 == rb.readUnsignedShort(5);
                final short byteAddress = (short)rb.readUnsignedInt(16);
                final byte bitAddress = rb.readUnsignedByte(3);

                return new S7Field(transportSize, memoryArea, dbNumber, byteAddress, bitAddress,
                    numberOfElements);
            } catch (ParseException | DecoderException e) {
                throw new PlcInvalidFieldException("Unable to parse address: " + fieldString);
            }
        }
        throw new PlcInvalidFieldException("Unable to parse address: " + fieldString);
    }

    /**
     * checks if DatablockNumber of S7Field is in valid range
     * @param blockNumber given DatablockNumber
     * @return given blockNumber if Ok, throws PlcInvalidFieldException otherwise
     */
    private static int checkDatablockNumber(int blockNumber){
        //ToDo check the value or add reference - limit eventually depending on active S7 --> make a case selection
        if(blockNumber>64000 || blockNumber<1){
            throw new PlcInvalidFieldException("Datablock numbers larger than 64000 or smaller than 1 are not supported.");
        }
        return blockNumber;
    }

    /**
     * checks if ByteOffset from S7Field is in valid range
     * @param byteOffset given byteOffset
     * @return given byteOffset if Ok, throws PlcInvalidFieldException otherwise
     */
    private static int checkByteOffset(int byteOffset){
        //ToDo check the value or add reference
        if(byteOffset>2097151 || byteOffset<0){
            throw new PlcInvalidFieldException("ByteOffset must be smaller than 2097151 and positive.");
        }
        return byteOffset;
    }

    /**
     * correct the storage of "array"-like variables like STRING
     * @param numElements auto-detected numElements (1 if no numElements in brackets has been given, x if a specific number has been given)
     * @param dataType detected Transport-Size that represents the data-type
     * @return corrected numElements if nessesary
     */
    private static int calcNumberOfElementsForIndividualTypes(int numElements, TransportSize dataType){

        if(dataType.equals(TransportSize.STRING)){
            //on valid String-length add two byte because of S7-representation of Strings
            if(numElements>1 && numElements<=254){
                return numElements+2;
            }
            //connection String usage with "STRING" only --> numElements=1 --> enter default value
            return 256;
        }
        return numElements;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S7Field s7Field = (S7Field) o;
        return getBlockNumber() == s7Field.getBlockNumber() &&
            getByteOffset() == s7Field.getByteOffset() &&
            getBitOffset() == s7Field.getBitOffset() &&
            getNumElements() == s7Field.getNumElements() &&
            getDataType() == s7Field.getDataType() &&
            getMemoryArea() == s7Field.getMemoryArea();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), getMemoryArea(), getBlockNumber(), getByteOffset(), getBitOffset(), getNumElements());
    }

    @Override
    public String toString() {
        return "S7Field{" +
            "dataType=" + dataType +
            ", memoryArea=" + memoryArea +
            ", blockNumber=" + blockNumber +
            ", byteOffset=" + byteOffset +
            ", bitOffset=" + bitOffset +
            ", numElements=" + numElements +
            '}';
    }
}
